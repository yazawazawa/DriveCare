package com.takumi.drivecare.cloud

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.takumi.drivecare.DriveCareRepository
import com.takumi.drivecare.MaintenanceItemDefRepository
import java.security.SecureRandom
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

private const val TAG = "CloudCoordinator"
private const val PREFS = "cloud_prefs"
private const val KEY_GROUP = "group_id"
private const val KEY_UPLOADED_LOCAL = "uploaded_local"
private const val NETWORK_TIMEOUT_MS = 20_000L
private const val INVITE_VALID_DAYS = 7L
/** 紛らわしい文字（0/O, 1/I/L）を除いた招待コード用の文字。 */
private const val CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
private const val CODE_LENGTH = 10

data class CloudMember(val uid: String, val name: String, val email: String, val isMe: Boolean)

data class CloudUiState(
    /** Firebase の設定（google-services.json）が含まれているか */
    val available: Boolean = false,
    val signedIn: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null,
    val groupId: String? = null,
    val members: List<CloudMember> = emptyList(),
    val syncStatus: SyncStatus? = null,
    val busy: Boolean = false,
    val inviteCode: String? = null,
    val inviteExpiresAt: Long? = null
)

data class CloudEvent(val text: String, val isError: Boolean)

/**
 * ログイン、共有グループの作成・参加・退出、招待コード、同期の開始/停止をまとめて扱う。
 * Firebase が未設定のビルドでは何もしない（[CloudUiState.available] が false）。
 */
class CloudCoordinator(
    private val app: Context,
    private val repository: DriveCareRepository,
    private val scope: CoroutineScope,
    /** データの取得元が変わった/内容が変わったときに画面用データを読み直す。引数は選択中の車両をリセットするか。 */
    private val onDataChanged: suspend (resetSelection: Boolean) -> Unit
) {
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val webClientId: String? = app.resources
        .getIdentifier("default_web_client_id", "string", app.packageName)
        .takeIf { it != 0 }?.let { app.getString(it) }
    val available: Boolean = webClientId != null && FirebaseApp.getApps(app).isNotEmpty()

    private val _state = MutableStateFlow(CloudUiState(available = available))
    val state: StateFlow<CloudUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CloudEvent>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<CloudEvent> = _events.asSharedFlow()

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private var dataSource: CloudDataSource? = null
    private var collectJobs: List<Job> = emptyList()
    private var membersRegistration: ListenerRegistration? = null
    @Volatile private var detaching = false

    val isSharing: Boolean get() = dataSource != null

    init {
        if (available) {
            updateUser(auth.currentUser)
            auth.addAuthStateListener { updateUser(it.currentUser) }
            val savedGroup = prefs.getString(KEY_GROUP, null)
            if (savedGroup != null) {
                if (auth.currentUser != null) attach(savedGroup) else prefs.edit().remove(KEY_GROUP).apply()
            }
        }
    }

    // ── ログイン ────────────────────────────────────────────────────────────

    fun signIn(activity: Activity) {
        val clientId = webClientId ?: return
        scope.launch {
            busy {
                try {
                    val credentialManager = CredentialManager.create(activity)
                    val option = GetSignInWithGoogleOption.Builder(clientId).build()
                    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                    val credential = credentialManager.getCredential(activity, request).credential
                    if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        error("Google アカウントの情報を取得できませんでした")
                    }
                    val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    awaitOnline(auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)))
                    resolveExistingGroup()
                } catch (e: GetCredentialCancellationException) {
                    // 本当に取り消した場合と、署名(SHA-1)の不一致などで Google 側が中断した場合の両方でここに来る。
                    // 黙って戻ると原因が分からないため、必ず知らせる
                    Log.w(TAG, "sign-in cancelled", e)
                    _events.tryEmit(
                        CloudEvent(
                            "ログインが完了しませんでした。取り消していないのに戻る場合は、Firebase に登録した SHA-1 とこのアプリの署名が一致していない可能性があります",
                            true
                        )
                    )
                } catch (e: NoCredentialException) {
                    Log.w(TAG, "no credential", e)
                    _events.tryEmit(CloudEvent("この端末に Google アカウントが見つかりません。端末の設定でアカウントを追加してください", true))
                } catch (e: GetCredentialException) {
                    Log.e(TAG, "sign-in failed", e)
                    _events.tryEmit(CloudEvent("ログインに失敗しました: ${e.message ?: e.type}", true))
                }
            }
        }
    }

    fun signOut(activity: Activity) {
        if (!available || isSharing) return
        scope.launch {
            runCatching { auth.signOut() }
            runCatching { CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest()) }
            prefs.edit().remove(KEY_GROUP).apply()
            _state.update { it.copy(inviteCode = null, inviteExpiresAt = null) }
        }
    }

    /** 別の端末で作成・参加済みなら、そのグループに接続する。 */
    private suspend fun resolveExistingGroup() {
        val uid = auth.currentUser?.uid ?: return
        val doc = awaitOnline(db.collection("users").document(uid).get())
        val groupId = doc.getString("groupId")
        if (!groupId.isNullOrEmpty()) {
            prefs.edit().putBoolean(KEY_UPLOADED_LOCAL, false).apply()
            attach(groupId)
            onDataChanged(true)
            _events.tryEmit(CloudEvent("共有グループに接続しました", false))
        }
    }

    private fun updateUser(user: FirebaseUser?) {
        _state.update {
            it.copy(
                signedIn = user != null,
                userName = user?.displayName,
                userEmail = user?.email
            )
        }
    }

    // ── グループ ────────────────────────────────────────────────────────────

    /** 共有グループを新しく作って共有を始める。[includeLocal] が true なら端末内のデータをグループへ追加する。 */
    fun createGroup(includeLocal: Boolean) {
        scope.launch {
            busy {
                val user = auth.currentUser ?: error("ログインしてください")
                val groupId = UUID.randomUUID().toString()
                awaitOnline(
                    db.collection("groups").document(groupId)
                        .set(mapOf("ownerUid" to user.uid, "createdAt" to System.currentTimeMillis()))
                )
                awaitOnline(memberRef(groupId, user.uid).set(memberMap(user, null)))
                if (includeLocal) uploadLocal(groupId)
                awaitOnline(db.collection("users").document(user.uid).set(mapOf("groupId" to groupId)))
                prefs.edit().putBoolean(KEY_UPLOADED_LOCAL, includeLocal).apply()
                attach(groupId)
                onDataChanged(true)
                _events.tryEmit(CloudEvent("共有を開始しました。招待コードを発行して相手に伝えましょう", false))
            }
        }
    }

    /** 招待コードでグループに参加する。 */
    fun joinGroup(rawCode: String, includeLocal: Boolean) {
        scope.launch {
            busy {
                val user = auth.currentUser ?: error("ログインしてください")
                val code = normalizeCode(rawCode)
                if (code.length != CODE_LENGTH) {
                    _events.tryEmit(CloudEvent("招待コードは${CODE_LENGTH}文字です", true))
                    return@busy
                }
                val invite = awaitOnline(db.collection("invites").document(code).get())
                val groupId = invite.getString("groupId")
                if (!invite.exists() || groupId.isNullOrEmpty()) {
                    _events.tryEmit(CloudEvent("招待コードが見つかりません", true))
                    return@busy
                }
                val expires = invite.getTimestamp("expiresAt")?.toDate()?.time
                if (expires != null && expires < System.currentTimeMillis()) {
                    _events.tryEmit(CloudEvent("招待コードの有効期限が切れています。新しいコードを発行してもらってください", true))
                    return@busy
                }
                awaitOnline(memberRef(groupId, user.uid).set(memberMap(user, code)))
                if (includeLocal) uploadLocal(groupId)
                awaitOnline(db.collection("users").document(user.uid).set(mapOf("groupId" to groupId)))
                prefs.edit().putBoolean(KEY_UPLOADED_LOCAL, includeLocal).apply()
                attach(groupId)
                onDataChanged(true)
                _events.tryEmit(CloudEvent("共有グループに参加しました", false))
            }
        }
    }

    /** 参加用の招待コードを発行する（7日間有効）。 */
    fun createInvite() {
        val groupId = dataSource?.groupId ?: return
        scope.launch {
            busy {
                val user = auth.currentUser ?: error("ログインしてください")
                val code = generateCode()
                val expiresAt = System.currentTimeMillis() + INVITE_VALID_DAYS * 24 * 3600 * 1000
                awaitOnline(
                    db.collection("invites").document(code).set(
                        mapOf(
                            "groupId" to groupId,
                            "createdBy" to user.uid,
                            "createdAt" to System.currentTimeMillis(),
                            "expiresAt" to Timestamp(Date(expiresAt))
                        )
                    )
                )
                _state.update { it.copy(inviteCode = code, inviteExpiresAt = expiresAt) }
            }
        }
    }

    /**
     * 共有を終了してグループから抜ける。[copyToDevice] が true なら、グループのデータを端末内へコピーする。
     * 端末内のデータを共有に追加していた場合は端末内データをグループの内容で置き換え、
     * 追加していなかった場合は元の端末内データに追記する。
     */
    fun leaveGroup(copyToDevice: Boolean) {
        val ds = dataSource ?: return
        scope.launch {
            busy {
                val user = auth.currentUser ?: error("ログインしてください")
                val snapshot = ds.snapshotAll()
                val defs = snapshot.vehicles.associate { it.id to MaintenanceItemDefRepository.getItemDefs(app, it.id) }
                detaching = true
                ds.stop()
                try {
                    awaitOnline(memberRef(ds.groupId, user.uid).delete())
                } catch (e: Exception) {
                    // 退出に失敗したので共有を続ける
                    detaching = false
                    ds.start()
                    throw e
                }
                runCatching { awaitOnline(db.collection("users").document(user.uid).delete()) }
                detach()
                if (copyToDevice) copyToLocal(snapshot, defs, replace = prefs.getBoolean(KEY_UPLOADED_LOCAL, false))
                prefs.edit().remove(KEY_UPLOADED_LOCAL).apply()
                onDataChanged(true)
                _events.tryEmit(CloudEvent(if (copyToDevice) "共有を終了し、データをこの端末にコピーしました" else "共有を終了しました", false))
            }
        }
    }

    /** 他のメンバーの操作などで、このグループにアクセスできなくなったとき。 */
    private fun handleAccessLost() {
        if (detaching) return
        val ds = dataSource ?: return
        detaching = true
        scope.launch {
            val snapshot = ds.snapshotAll()
            val defs = snapshot.vehicles.associate { it.id to MaintenanceItemDefRepository.getItemDefs(app, it.id) }
            ds.stop()
            detach()
            runCatching { copyToLocal(snapshot, defs, replace = prefs.getBoolean(KEY_UPLOADED_LOCAL, false)) }
            prefs.edit().remove(KEY_UPLOADED_LOCAL).apply()
            onDataChanged(true)
            _events.tryEmit(CloudEvent("共有グループにアクセスできなくなりました。最後に同期したデータを端末にコピーしました", true))
        }
    }

    // ── 同期の開始・停止 ────────────────────────────────────────────────────

    private fun attach(groupId: String) {
        detach()
        detaching = false
        val ds = CloudDataSource(db, groupId, app) { handleAccessLost() }
        dataSource = ds
        repository.attachCloud(ds)
        ds.start()
        MaintenanceItemDefRepository.onUserSaved = { vehicleId, defs ->
            ds.pushItemDefs(vehicleId, MaintenanceItemDefRepository.toJson(defs))
        }
        collectJobs = listOf(
            scope.launch { ds.changes.collect { onDataChanged(false) } },
            scope.launch { ds.status.collect { s -> _state.update { it.copy(syncStatus = s) } } },
            scope.launch { ds.writeErrors.collect { _events.emit(CloudEvent("同期できなかった変更があります: $it", true)) } }
        )
        val me = auth.currentUser?.uid
        membersRegistration = db.collection("groups").document(groupId).collection("members")
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                val members = snap.documents.map { d ->
                    CloudMember(d.id, d.getString("name").orEmpty(), d.getString("email").orEmpty(), d.id == me)
                }.sortedWith(compareByDescending<CloudMember> { it.isMe }.thenBy { it.name })
                _state.update { it.copy(members = members) }
            }
        prefs.edit().putString(KEY_GROUP, groupId).apply()
        _state.update { it.copy(groupId = groupId, inviteCode = null, inviteExpiresAt = null) }
    }

    private fun detach() {
        collectJobs.forEach { it.cancel() }
        collectJobs = emptyList()
        membersRegistration?.remove()
        membersRegistration = null
        dataSource?.stop()
        dataSource = null
        repository.detachCloud()
        MaintenanceItemDefRepository.onUserSaved = null
        prefs.edit().remove(KEY_GROUP).apply()
        _state.update { it.copy(groupId = null, members = emptyList(), syncStatus = null, inviteCode = null, inviteExpiresAt = null) }
    }

    // ── 端末内データとのやり取り ─────────────────────────────────────────────

    private suspend fun uploadLocal(groupId: String) {
        val local = repository.local
        val data = LocalData(local.getAllVehicles(), local.getAllFuelRecords(), local.getAllMaintenanceRecords())
        CloudDataSource(db, groupId, app) {}.uploadAll(data)
    }

    private suspend fun copyToLocal(data: LocalData, defs: Map<Int, List<com.takumi.drivecare.MaintenanceItemDef>>, replace: Boolean) {
        val local = repository.local
        if (replace) {
            local.getAllVehicles().forEach { MaintenanceItemDefRepository.removeVehicle(app, it.id) }
            local.replaceAllData(emptyList(), emptyList(), emptyList())
        }
        val vehicleIds = HashMap<Int, Int>()
        data.vehicles.forEach { v ->
            val newId = local.insertVehicle(v.copy(id = 0)).toInt()
            vehicleIds[v.id] = newId
            defs[v.id]?.let { MaintenanceItemDefRepository.storeLocally(app, newId, it) }
        }
        data.fuelRecords.sortedBy { it.timestamp }.forEach { r ->
            val vid = vehicleIds[r.vehicleId] ?: return@forEach
            local.insertFuelRecord(r.copy(id = 0, vehicleId = vid))
        }
        data.maintenanceRecords.sortedBy { it.timestamp }.forEach { r ->
            val vid = vehicleIds[r.vehicleId] ?: return@forEach
            local.insertMaintenanceRecord(r.copy(id = 0, vehicleId = vid))
        }
        // クラウド用の車両IDで保存していた整備項目の設定は不要になる
        data.vehicles.forEach { MaintenanceItemDefRepository.removeVehicle(app, it.id) }
    }

    // ── 共通処理 ────────────────────────────────────────────────────────────

    private fun memberRef(groupId: String, uid: String): DocumentReference =
        db.collection("groups").document(groupId).collection("members").document(uid)

    private fun memberMap(user: FirebaseUser, code: String?): Map<String, Any> = buildMap {
        put("name", user.displayName.orEmpty())
        put("email", user.email.orEmpty())
        put("joinedAt", System.currentTimeMillis())
        if (code != null) put("code", code)
    }

    private fun generateCode(): String {
        val random = SecureRandom()
        return buildString { repeat(CODE_LENGTH) { append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]) } }
    }

    private fun normalizeCode(raw: String): String =
        raw.uppercase().filter { it.isLetterOrDigit() }

    /** サーバーとの通信を待つ。オフラインのまま永遠に待たないよう時間で打ち切る。 */
    private suspend fun <T> awaitOnline(task: Task<T>): T = withTimeout(NETWORK_TIMEOUT_MS) { task.await() }

    /** 処理中表示と、失敗時のエラー通知をまとめて行う。 */
    private suspend fun busy(block: suspend () -> Unit) {
        _state.update { it.copy(busy = true) }
        try {
            block()
        } catch (e: TimeoutCancellationException) {
            _events.tryEmit(CloudEvent("通信がタイムアウトしました。ネットワークに接続してもう一度お試しください", true))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "cloud operation failed", e)
            _events.tryEmit(CloudEvent(describe(e), true))
        } finally {
            _state.update { it.copy(busy = false) }
        }
    }

    private fun describe(e: Exception): String = when {
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "権限がありません。招待コードが無効か、すでに無効になっている可能性があります"
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "サーバーに接続できません。ネットワークを確認してください"
        else -> e.localizedMessage?.takeIf { it.isNotBlank() } ?: "操作に失敗しました"
    }
}
