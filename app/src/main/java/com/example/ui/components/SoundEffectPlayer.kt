package com.example.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SoundEffectPlayer(context: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var soundPool: SoundPool? = null

  @Volatile
  private var clickSoundId: Int = 0

  @Volatile
  private var isLoaded: Boolean = false

  init {
    val appContext = context.applicationContext
    scope.launch {
      kotlinx.coroutines.delay(150)
      runCatching {
        val attrs = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
        val pool = SoundPool.Builder()
          .setMaxStreams(4)
          .setAudioAttributes(attrs)
          .build()
        pool.setOnLoadCompleteListener { _, sampleId, status ->
          if (status == 0 && sampleId == clickSoundId) {
            isLoaded = true
          }
        }
        clickSoundId = pool.load(appContext, R.raw.key_click, 1)
        soundPool = pool
      }
    }
  }

  fun playClick(enabled: Boolean = true) {
    if (!enabled || !isLoaded) return
    val id = clickSoundId
    if (id != 0) {
      soundPool?.play(id, 0.55f, 0.55f, 1, 0, 1.0f)
    }
  }

  fun release() {
    scope.cancel()
    soundPool?.release()
    soundPool = null
    isLoaded = false
  }
}

val LocalSoundEffectPlayer = staticCompositionLocalOf<SoundEffectPlayer?> { null }
