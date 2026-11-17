package com.takumi.drivecare.cloud

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.takumi.drivecare.DriveCareDataSource
import com.takumi.drivecare.FuelRecord
import com.takumi.drivecare.FuelType
import com.takumi.drivecare.MaintenanceItemDefRepository
import com.takumi.drivecare.MaintenanceRecord
import com.takumi.drivecare.Vehicle
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "CloudDataSource"

/** クラウドの文書ID（文字列）から、アプリ内で使う Int の ID を決める。同じ文書IDなら端末が違っても同じ値になる。 */
fun stableId(docId: String): Int = (docId.hashCode() and 0x7FFFFFFF).let { if (it == 0) 1 else it }

/** 同期の状態表示用。 */
data class SyncStatus(
    /** 端末内にあり、まだサーバーへ届いていない変更がある */
    val hasPendingWrites: Boolean = false,
    /** サーバーに接続できておらず、端末内のキャッシュを表示している */
    val fromCache: Boolean = true
)

/** 端末内データ（共有を始める前 / 終えた後）。 */
data class LocalData(
    val vehicles: List<Vehicle>,
    val fuelRecords: List<FuelRecord>,
    val maintenanceRecords: List<MaintenanceRecord>
)

/**
 * Firestore 上の共有グループ `groups/{groupId}` を [DriveCareDataSource] として扱う。
 *
 * - 変更はまず端末内のキャッシュへ即時反映し、サーバーへは待たずに書き込む（オフラインでも操作でき、復帰時に自動送信）。
 * - 他の端末の変更は snapshot リスナーで受け取り、[changes] で通知する。
 * - 最後に書いた方が勝つ（文書単位）。
 */
class CloudDataSource(
    private val db: FirebaseFirestore,
    val groupId: String,
    private val appContext: Context,
    private val onAccessLost: () -> Unit
) : DriveCareDataSource {

    private class Entry<T>(val docId: String, val value: T, val order: Long)

    private val group = db.collection("groups").document(groupId)
    private val vehiclesCol: CollectionReference = group.collection("vehicles")
    private val fuelCol: CollectionReference = group.collection("fuelRecords")
    private val maintCol: CollectionReference = group.collection("maintenanceRecords")

    @Volatile private var vehicles: List<Entry<Vehicle>> = emptyList()
    @Volatile private var fuel: List<Entry<FuelRecord>> = emptyList()
    @Volatile private var maint: List<Entry<MaintenanceRecord>> = emptyList()

    private val lock = Any()
    private val ready = CompletableDeferred<Unit>()
    private val received = BooleanArray(3)
    private val pending = BooleanArray(3)
    private val fromCache = booleanArrayOf(true, true, true)
    private val registrations = mutableListOf<ListenerRegistration>()

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    /** データが変わるたびに通知される（自分の変更・他の端末の変更の両方）。 */
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _writeErrors = MutableSharedFlow<String>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    /** サーバーに書き込みを拒否された場合などのエラー。 */
    val writeErrors: SharedFlow<String> = _writeErrors.asSharedFlow()

    // ── リスナー ─────────────────────────────────────────────────────────

    fun start() {
        listen(0, vehiclesCol) { snap ->
            val list = snap.documents.mapNotNull { d -> parseVehicle(d)?.let { Entry(d.id, it, d.getLong("createdAt") ?: 0L) } }
                .sortedWith(compareBy<Entry<Vehicle>> { it.order }.thenBy { it.docId })
            vehicles = list
            // 他の端末で変更された整備項目の設定を取り込む
            snap.documents.forEach { d ->
                d.getString("itemDefsJson")?.let { MaintenanceItemDefRepository.applyRemoteJson(appContext, stableId(d.id), it) }
            }
        }
        listen(1, fuelCol) { snap ->
            fuel = snap.documents.mapNotNull { d -> parseFuel(d)?.let { Entry(d.id, it, it.timestamp) } }
        }
        listen(2, maintCol) { snap ->
            maint = snap.documents.mapNotNull { d -> parseMaintenance(d)?.let { Entry(d.id, it, it.timestamp) } }
        }
    }

    fun stop() {
        registrations.forEach { it.remove() }
        registrations.clear()
    }

    private fun listen(index: Int, col: CollectionReference, apply: (com.google.firebase.firestore.QuerySnapshot) -> Unit) {
        registrations += col.addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
            if (error != null) {
                Log.e(TAG, "listen failed: ${col.path}", error)
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) onAccessLost()
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            synchronized(lock) {
                apply(snap)
                pending[index] = snap.metadata.hasPendingWrites()
                fromCache[index] = snap.metadata.isFromCache
                received[index] = true
                if (received.all { it }) ready.complete(Unit)
                _status.value = SyncStatus(pending.any { it }, fromCache.any { it })
            }
            _changes.tryEmit(Unit)
        }
    }

    /** 最初のデータが届くまで待つ（キャッシュがあれば即座に届く）。届かなくても一定時間で先へ進む。 */
    private suspend fun awaitReady() {
        if (!ready.isCompleted) withTimeoutOrNull(4_000) { ready.await() }
    }

    // ── 読み取り ─────────────────────────────────────────────────────────

    override suspend fun getAllVehicles(): List<Vehicle> { awaitReady(); return vehicles.map { it.value } }
    override suspend fun getAllFuelRecords(): List<FuelRecord> { awaitReady(); return fuel.map { it.value } }
    override suspend fun getAllMaintenanceRecords(): List<MaintenanceRecord> { awaitReady(); return maint.map { it.value } }

    /** 現在キャッシュされているデータ一式（共有を終えるときに端末へコピーする用）。 */
    suspend fun snapshotAll(): LocalData {
        awaitReady()
        return LocalData(vehicles.map { it.value }, fuel.map { it.value }, maint.map { it.value })
    }

    // ── 書き込み ─────────────────────────────────────────────────────────

    override suspend fun insertVehicle(vehicle: Vehicle): Long {
        val docId = newDocId(vehicles)
        val v = vehicle.copy(id = stableId(docId))
        val createdAt = System.currentTimeMillis()
        synchronized(lock) { vehicles = (vehicles + Entry(docId, v, createdAt)).sortedWith(compareBy<Entry<Vehicle>> { it.order }.thenBy { it.docId }) }
        fire(vehiclesCol.document(docId).set(vehicleMap(v, createdAt)))
        return v.id.toLong()
    }

    override suspend fun updateVehicle(vehicle: Vehicle) {
        val docId = vehicles.docIdFor(vehicle.id) ?: return
        synchronized(lock) { vehicles = vehicles.map { if (it.docId == docId) Entry(docId, vehicle, it.order) else it } }
        // 整備項目の設定（itemDefsJson）を消さないよう merge で書く
        fire(vehiclesCol.document(docId).set(vehicleMap(vehicle, null), SetOptions.merge()))
    }

    override suspend fun deleteVehicle(vehicle: Vehicle) {
        val docId = vehicles.docIdFor(vehicle.id) ?: return
        val fuelDocs = fuel.filter { it.value.vehicleId == vehicle.id }.map { it.docId }
        val maintDocs = maint.filter { it.value.vehicleId == vehicle.id }.map { it.docId }
        synchronized(lock) {
            vehicles = vehicles.filter { it.docId != docId }
            fuel = fuel.filter { it.value.vehicleId != vehicle.id }
            maint = maint.filter { it.value.vehicleId != vehicle.id }
        }
        val refs = fuelDocs.map { fuelCol.document(it) } + maintDocs.map { maintCol.document(it) } + vehiclesCol.document(docId)
        refs.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it) }
            fire(batch.commit())
        }
    }

    override suspend fun insertFuelRecord(record: FuelRecord): Long {
        val vehicleDoc = vehicles.docIdFor(record.vehicleId) ?: throw IllegalStateException("車両が見つかりません")
        val docId = newDocId(fuel)
        val r = record.copy(id = stableId(docId))
        synchronized(lock) { fuel = fuel + Entry(docId, r, r.timestamp) }
        fire(fuelCol.document(docId).set(fuelMap(r, vehicleDoc)))
        return r.id.toLong()
    }

    override suspend fun updateFuelRecord(record: FuelRecord) {
        val docId = fuel.docIdFor(record.id) ?: return
        val vehicleDoc = vehicles.docIdFor(record.vehicleId) ?: return
        synchronized(lock) { fuel = fuel.map { if (it.docId == docId) Entry(docId, record, record.timestamp) else it } }
        fire(fuelCol.document(docId).set(fuelMap(record, vehicleDoc)))
    }

    override suspend fun deleteFuelRecord(record: FuelRecord) {
        val docId = fuel.docIdFor(record.id) ?: return
        synchronized(lock) { fuel = fuel.filter { it.docId != docId } }
        fire(fuelCol.document(docId).delete())
    }

    override suspend fun insertMaintenanceRecord(record: MaintenanceRecord): Long {
        val vehicleDoc = vehicles.docIdFor(record.vehicleId) ?: throw IllegalStateException("車両が見つかりません")
        val docId = newDocId(maint)
        val r = record.copy(id = stableId(docId))
        synchronized(lock) { maint = maint + Entry(docId, r, r.timestamp) }
        fire(maintCol.document(docId).set(maintMap(r, vehicleDoc)))
        return r.id.toLong()
    }

    override suspend fun updateMaintenanceRecord(record: MaintenanceRecord) {
        val docId = maint.docIdFor(record.id) ?: return
        val vehicleDoc = vehicles.docIdFor(record.vehicleId) ?: return
        synchronized(lock) { maint = maint.map { if (it.docId == docId) Entry(docId, record, record.timestamp) else it } }
        fire(maintCol.document(docId).set(maintMap(record, vehicleDoc)))
    }

    override suspend fun deleteMaintenanceRecord(record: MaintenanceRecord) {
        val docId = maint.docIdFor(record.id) ?: return
        synchronized(lock) { maint = maint.filter { it.docId != docId } }
        fire(maintCol.document(docId).delete())
    }

    /** 共有中のデータ全置換は、他のメンバーのデータも消してしまうため行わない。 */
    override suspend fun replaceAllData(
        vehicles: List<Vehicle>,
        fuelRecords: List<FuelRecord>,
        maintenanceRecords: List<MaintenanceRecord>
    ) {
        throw UnsupportedOperationException("共有中はデータを置き換えられません")
    }

    /** ユーザーが整備項目の設定を変更したときに、その車両の文書へ反映する。 */
    fun pushItemDefs(vehicleId: Int, defsJson: String) {
        val docId = vehicles.docIdFor(vehicleId) ?: return
        fire(vehiclesCol.document(docId).set(mapOf("itemDefsJson" to defsJson), SetOptions.merge()))
    }

    // ── 端末内データのアップロード ────────────────────────────────────────

    /**
     * 端末内のデータをこのグループへ追加する（共有を始めるとき）。サーバーへの書き込み完了まで待つ。
     * 整備項目の設定も、新しい車両IDへ付け替えて保存する。
     */
    suspend fun uploadAll(data: LocalData) {
        val vehicleDocs = HashMap<Int, String>()
        val ops = ArrayList<(WriteBatch) -> Unit>()
        val now = System.currentTimeMillis()

        data.vehicles.forEachIndexed { index, v ->
            val docId = UUID.randomUUID().toString()
            vehicleDocs[v.id] = docId
            val defs = MaintenanceItemDefRepository.getItemDefs(appContext, v.id)
            val defsJson = MaintenanceItemDefRepository.toJson(defs)
            MaintenanceItemDefRepository.storeLocally(appContext, stableId(docId), defs)
            val map = vehicleMap(v, now + index) + ("itemDefsJson" to defsJson)
            ops += { b -> b.set(vehiclesCol.document(docId), map) }
        }
        data.fuelRecords.forEach { r ->
            val vehicleDoc = vehicleDocs[r.vehicleId] ?: return@forEach
            ops += { b -> b.set(fuelCol.document(UUID.randomUUID().toString()), fuelMap(r, vehicleDoc)) }
        }
        data.maintenanceRecords.forEach { r ->
            val vehicleDoc = vehicleDocs[r.vehicleId] ?: return@forEach
            ops += { b -> b.set(maintCol.document(UUID.randomUUID().toString()), maintMap(r, vehicleDoc)) }
        }
        ops.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { it(batch) }
            // オフラインだと完了しないため、一定時間で打ち切ってユーザーに知らせる
            withTimeout(30_000) { batch.commit().await() }
        }
    }

    // ── ヘルパー ─────────────────────────────────────────────────────────

    private fun fire(task: com.google.android.gms.tasks.Task<*>) {
        task.addOnFailureListener { e ->
            Log.e(TAG, "write failed", e)
            _writeErrors.tryEmit(e.localizedMessage ?: "同期に失敗しました")
        }
    }

    private fun <T> List<Entry<T>>.docIdFor(id: Int): String? = firstOrNull { stableId(it.docId) == id }?.docId

    /** 既存の文書とID（Int）が衝突しない新しい文書IDを作る。 */
    private fun <T> newDocId(existing: List<Entry<T>>): String {
        val used = existing.mapTo(HashSet()) { stableId(it.docId) }
        while (true) {
            val candidate = UUID.randomUUID().toString()
            if (stableId(candidate) !in used) return candidate
        }
    }

    private fun vehicleMap(v: Vehicle, createdAt: Long?): Map<String, Any> = buildMap {
        put("maker", v.maker)
        put("model", v.model)
        put("tankCapacity", v.tankCapacity)
        put("fuelType", v.fuelType.name)
        if (createdAt != null) put("createdAt", createdAt)
        put("updatedAt", FieldValue.serverTimestamp())
    }

    private fun fuelMap(r: FuelRecord, vehicleDoc: String): Map<String, Any> = mapOf(
        "vehicleId" to vehicleDoc,
        "odometer" to r.odometer,
        "liters" to r.liters,
        "price" to r.price,
        "unitPrice" to r.unitPrice,
        "isFullTank" to r.isFullTank,
        "timestamp" to r.timestamp,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun maintMap(r: MaintenanceRecord, vehicleDoc: String): Map<String, Any> = mapOf(
        "vehicleId" to vehicleDoc,
        "timestamp" to r.timestamp,
        "odometer" to r.odometer,
        "carWashDone" to r.carWashDone,
        "engineOilDone" to r.engineOilDone,
        "oilElementDone" to r.oilElementDone,
        "wiperDone" to r.wiperDone,
        "tireDone" to r.tireDone,
        "airCleanerCleaningDone" to r.airCleanerCleaningDone,
        "airCleanerReplacementDone" to r.airCleanerReplacementDone,
        "comment" to r.comment,
        "customItemsDone" to r.customItemsDone,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun parseVehicle(d: DocumentSnapshot): Vehicle? {
        val maker = d.getString("maker") ?: return null
        val model = d.getString("model") ?: return null
        val tank = d.getDouble("tankCapacity") ?: return null
        val fuelType = FuelType.entries.firstOrNull { it.name == d.getString("fuelType") } ?: FuelType.REGULAR
        return Vehicle(stableId(d.id), maker, model, tank, fuelType)
    }

    private fun parseFuel(d: DocumentSnapshot): FuelRecord? {
        val vehicleDoc = d.getString("vehicleId") ?: return null
        return FuelRecord(
            id = stableId(d.id),
            vehicleId = stableId(vehicleDoc),
            odometer = d.getDouble("odometer") ?: return null,
            liters = d.getDouble("liters") ?: return null,
            price = d.getLong("price")?.toInt() ?: return null,
            unitPrice = d.getDouble("unitPrice") ?: return null,
            isFullTank = d.getBoolean("isFullTank") ?: true,
            timestamp = d.getLong("timestamp") ?: return null
        )
    }

    private fun parseMaintenance(d: DocumentSnapshot): MaintenanceRecord? {
        val vehicleDoc = d.getString("vehicleId") ?: return null
        return MaintenanceRecord(
            id = stableId(d.id),
            vehicleId = stableId(vehicleDoc),
            timestamp = d.getLong("timestamp") ?: return null,
            odometer = d.getDouble("odometer") ?: return null,
            carWashDone = d.getBoolean("carWashDone") ?: false,
            engineOilDone = d.getBoolean("engineOilDone") ?: false,
            oilElementDone = d.getBoolean("oilElementDone") ?: false,
            wiperDone = d.getBoolean("wiperDone") ?: false,
            tireDone = d.getBoolean("tireDone") ?: false,
            airCleanerCleaningDone = d.getBoolean("airCleanerCleaningDone") ?: false,
            airCleanerReplacementDone = d.getBoolean("airCleanerReplacementDone") ?: false,
            comment = d.getString("comment") ?: "",
            customItemsDone = d.getString("customItemsDone") ?: ""
        )
    }
}
