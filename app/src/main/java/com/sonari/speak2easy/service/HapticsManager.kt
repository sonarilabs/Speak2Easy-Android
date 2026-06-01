package com.sonari.speak2easy.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Light haptic feedback, gated on the user's preference. Mirrors iOS HapticsManager
 * (playCorrect / playIncorrect / playSelection). [enabled] is kept in sync with DataStore
 * by [com.sonari.speak2easy.di.AppContainer].
 */
class HapticsManager(context: Context) {

    @Volatile
    var enabled: Boolean = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // EFFECT_TICK is intentionally soft for frequent selection ticks; EFFECT_CLICK was too strong.
    fun playSelection() = play(VibrationEffect.EFFECT_TICK)
    fun playCorrect() = play(VibrationEffect.EFFECT_CLICK)
    fun playIncorrect() = play(VibrationEffect.EFFECT_DOUBLE_CLICK)

    private fun play(effectId: Int) {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching { v.vibrate(VibrationEffect.createPredefined(effectId)) }
    }
}
