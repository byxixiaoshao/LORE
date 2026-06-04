package com.bicy.novel.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.airbnb.lottie.compose.LottieAnimation
import androidx.compose.animation.core.FastOutSlowInEasing
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isDataLoaded: Boolean = true,
    onAnimationComplete: () -> Unit
) {
    // 尝试加载JSON格式的Lottie动画
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("LORE.json")
    )
    
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )
    
    var hasAnimationCompleted by remember { mutableStateOf(false) }
    var startFadeOut by remember { mutableStateOf(false) }
    
    // 淡出动画
    val fadeAlpha = remember { Animatable(1f) }
    
    // 动画完成后等待数据加载
    LaunchedEffect(progress) {
        if (progress == 1.0f && !hasAnimationCompleted) {
            hasAnimationCompleted = true
        }
    }
    
    // 动画完成且数据加载完成后开始淡出
    LaunchedEffect(hasAnimationCompleted, isDataLoaded) {
        if (hasAnimationCompleted && isDataLoaded && !startFadeOut) {
            delay(150) // 短暂延迟，让动画完整显示
            startFadeOut = true
        }
    }
    
    // 淡出动画执行
    LaunchedEffect(startFadeOut) {
        if (startFadeOut) {
            fadeAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = FastOutSlowInEasing
                )
            )
            onAnimationComplete()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(fadeAlpha.value)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize(0.6f)
        )
    }
}
