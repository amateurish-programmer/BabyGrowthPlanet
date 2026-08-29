package com.baobei.growthplanet.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import com.baobei.growthplanet.R
import com.baobei.growthplanet.settings.PlanetTheme

class MusicController(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player?.pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player?.setVolume(0.025f, 0.025f)
            AudioManager.AUDIOFOCUS_GAIN -> if (active && enabled) { player?.setVolume(0.10f, 0.10f); player?.start() }
        }
    }
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener(focusListener).setWillPauseWhenDucked(false).build()
    private var player: MediaPlayer? = null
    private var theme = PlanetTheme.SPACE
    private var active = false
    private var enabled = true

    fun setEnabled(value: Boolean) {
        enabled = value
        if (value && active) play(theme) else pause()
    }

    fun play(newTheme: PlanetTheme) {
        active = true
        if (!enabled) return
        if (newTheme != theme || player == null) {
            player?.release()
            theme = newTheme
            val resource = when (theme) {
                PlanetTheme.SPACE -> R.raw.music_space
                PlanetTheme.FOREST -> R.raw.music_forest
                PlanetTheme.RAINBOW -> R.raw.music_rainbow
            }
            player = MediaPlayer.create(context, resource)?.apply {
                isLooping = true
                setVolume(0.10f, 0.10f)
            }
        }
        if (audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) player?.start()
    }

    fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    fun resume() {
        active = true
        play(theme)
    }

    fun onStop() { active = false; pause() }
    fun release() { player?.release(); player = null }
}
