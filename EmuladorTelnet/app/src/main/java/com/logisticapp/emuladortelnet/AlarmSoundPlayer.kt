package com.logisticapp.emuladortelnet

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper
import timber.log.Timber

/**
 * Toca o alarme do host (BEL). Usa o stream de ALARME (não notificação) de
 * propósito: no Android, esse stream ignora "Não perturbe" por padrão — é
 * pra isso que ele existe — então não precisa pedir nenhuma permissão
 * especial pra garantir que o som realmente toque alto.
 *
 * Usado tanto pelo alarme de verdade (MainActivity) quanto pelo botão
 * "Testar"/prévia na tela de configurações, pra soar exatamente igual.
 */
object AlarmSoundPlayer {

    /** Sons de alerta prontos (sintetizados, sem precisar de arquivo de áudio). */
    enum class Preset(val id: String, val label: String, val tone: Int, val repeats: Int) {
        ERRO("tone:erro", "Alerta de erro", ToneGenerator.TONE_SUP_ERROR, 2),
        ALARME("tone:alarme", "Alarme forte", ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1),
        BUZINA("tone:buzina", "Buzina curta", ToneGenerator.TONE_PROP_NACK, 3);

        companion object {
            fun byId(id: String): Preset? = values().find { it.id == id }
        }
    }

    private val alarmAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun play(context: Context, soundUri: String) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val original = audio?.getStreamVolume(AudioManager.STREAM_ALARM)
        try {
            val max = audio?.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            if (audio != null && max != null) {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
            }
        } catch (e: SecurityException) {
            Timber.w(e, "Sem permissão pra subir o volume do alarme — tocando no volume atual")
        }

        val preset = Preset.byId(soundUri)
        when {
            preset != null -> playPreset(preset)
            soundUri.isNotBlank() -> playCustom(context, soundUri)
            else -> playPreset(Preset.ERRO) // sem escolha salva ainda: usa o alerta de erro como padrão
        }

        if (audio != null && original != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    audio.setStreamVolume(AudioManager.STREAM_ALARM, original, 0)
                } catch (e: SecurityException) {
                    // nada a restaurar se não conseguimos nem subir
                }
            }, 2500)
        }
    }

    private fun playPreset(preset: Preset) {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            var delay = 0L
            repeat(preset.repeats) {
                Handler(Looper.getMainLooper()).postDelayed({
                    tone.startTone(preset.tone, 300)
                }, delay)
                delay += 400
            }
            Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, delay + 300)
        } catch (e: Exception) {
            Timber.w(e, "Falha ao tocar alerta '${preset.id}'")
        }
    }

    private fun playCustom(context: Context, soundUri: String) {
        try {
            val player = MediaPlayer()
            player.setAudioAttributes(alarmAttributes)
            player.setDataSource(context, Uri.parse(soundUri))
            player.setOnCompletionListener { it.release() }
            player.setOnErrorListener { mp, _, _ -> mp.release(); true }
            player.prepare()
            player.start()
        } catch (e: Exception) {
            Timber.w(e, "Falha ao tocar som customizado do alarme — usando alerta padrão")
            playPreset(Preset.ERRO)
        }
    }

    /** Nome legível do som escolhido, pra mostrar na tela de configurações. */
    fun displayName(context: Context, soundUri: String): String {
        Preset.byId(soundUri)?.let { return it.label }
        if (soundUri.isBlank()) return "Alerta de erro (padrão)"
        return try {
            RingtoneManager.getRingtone(context, Uri.parse(soundUri))?.getTitle(context)
                ?: "Som importado"
        } catch (e: Exception) {
            "Som importado"
        }
    }
}
