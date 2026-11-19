package com.gamss.android.feature.game.clicker.component

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.gamss.android.feature.game.R
import kotlin.random.Random

private const val MAX_STREAMS = 6
private const val MIN_RATE = 0.94f
private const val RATE_RANGE = 0.12f

@Composable
internal fun rememberClickerKeySound(): ClickerKeySound {
    val context = LocalContext.current
    val sound = remember(context) { ClickerKeySound(context) }
    DisposableEffect(sound) {
        onDispose { sound.release() }
    }
    return sound
}

/** 게임 소리로 미디어 볼륨만 따른다. 무음 모드여도 미디어 볼륨이 켜져 있으면 난다. */
internal class ClickerKeySound(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val soundId = pool.load(context, R.raw.game_clicker_key_sound, 1)

    // 같은 소리가 똑같이 반복되지 않게 재생 속도를 조금씩 바꾼다.
    fun play() {
        pool.play(soundId, 1f, 1f, 0, 0, MIN_RATE + Random.nextFloat() * RATE_RANGE)
    }

    fun release() {
        pool.release()
    }
}
