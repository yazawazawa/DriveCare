package com.takumi.drivecare.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// アプリ全体で使う動きの部品。いずれも「一度だけ再生される入場アニメーション」か「操作への即時フィードバック」。

/** 1つの画面内で、部品が順番に現れるための番号を配る。 */
class StaggerState {
    var next = 0
}

/** null のときは入場アニメーションを付けない（ダイアログの中など）。 */
val LocalStagger = staticCompositionLocalOf<StaggerState?> { null }

/**
 * 画面を開いたとき、上の部品から順に、下からふわっと浮かび上がるように現れる。
 * 画面に入ったときに1回だけ再生される。再コンポーズでは再生されない。
 */
fun Modifier.staggerIn(maxSteps: Int = 8, stepMillis: Long = 55L): Modifier = composed {
    val stagger = LocalStagger.current
    val index = remember { stagger?.let { s -> s.next.also { s.next = it + 1 } } ?: 0 }
    val progress = remember { Animatable(if (stagger == null) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(minOf(index, maxSteps) * stepMillis)
            progress.animateTo(1f, spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessLow))
        }
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 26.dp.toPx()
    }
}

/** ダイアログやトーストが、少し小さい状態からぽんっと現れる。 */
fun Modifier.popIn(initialScale: Float = 0.92f): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
    }
    graphicsLayer {
        val s = initialScale + (1f - initialScale) * progress.value
        scaleX = s
        scaleY = s
        alpha = progress.value.coerceIn(0f, 1f)
    }
}

/**
 * 最初は 0 から [target] まで動き、その後 [target] が変わると現在の値から滑らかに動く値。
 * （animateFloatAsState は初回に動かないため、数値のカウントアップやプログレスバー用に用意）
 */
@Composable
fun animateFromZero(target: Float, durationMillis: Int = 800): State<Float> {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animatable.animateTo(target, tween(durationMillis, easing = FastOutSlowInEasing))
    }
    return animatable.asState()
}

/** 数値が 0 から目標値までカウントアップして表示される。値が無ければ「—」。 */
@Composable
fun CountUpText(
    value: Float?,
    format: (Float) -> String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animated by animateFromZero(value ?: 0f)
    Text(
        text = if (value == null) "—" else format(animated),
        style = style,
        color = color,
        modifier = modifier
    )
}

/** 選択を切り替えたときの、ごく軽い振動。 */
@Composable
fun rememberTick(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } }
}

/** 保存など、決定操作のときの、少ししっかりした振動。 */
@Composable
fun rememberConfirmHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } }
}
