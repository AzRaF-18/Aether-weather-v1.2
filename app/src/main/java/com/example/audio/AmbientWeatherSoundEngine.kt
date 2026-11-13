package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.model.SkyPhase
import com.example.data.model.WeatherIconKind
import com.example.data.model.WeatherReport
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

data class TrafficAudioConfig(
  val carCount: Int = 4,
  val busCount: Int = 1,
) {
  val hasManyCars: Boolean get() = carCount >= 4
  val hasBus: Boolean get() = busCount > 0
}

enum class AmbientSoundType(val label: String) {
  RAIN_SOUNDS("Rain patter & wet street"),
  THUNDER_RAIN("Rain & distant thunder"),
  MIDNIGHT_CRICKETS("Slow midnight crickets"),
  STREET_TRAFFIC_AND_VOICES("Cars, people talking & breeze"),
  SNOW_HUSH("Quiet snow air");

  companion object {
    fun fromWeather(report: WeatherReport): AmbientSoundType =
      when {
        report.condition.iconKind == WeatherIconKind.THUNDERSTORM -> THUNDER_RAIN
        report.condition.isRainy || report.condition.iconKind == WeatherIconKind.RAIN -> RAIN_SOUNDS
        report.condition.iconKind == WeatherIconKind.SNOW -> SNOW_HUSH
        report.skyPhase == SkyPhase.MIDNIGHT ||
          report.skyPhase == SkyPhase.DEEP_NIGHT ||
          report.localMinutesOfDay >= 1320 ||
          report.localMinutesOfDay <= 270 -> MIDNIGHT_CRICKETS
        else -> STREET_TRAFFIC_AND_VOICES
      }

    fun labelFor(type: AmbientSoundType, traffic: TrafficAudioConfig): String =
      if (type == STREET_TRAFFIC_AND_VOICES) {
        when {
          traffic.hasManyCars && traffic.hasBus -> "Cars, buses, horns, voices & breeze"
          !traffic.hasManyCars && traffic.hasBus -> "Cars, bus horn, voices & breeze"
          traffic.hasManyCars && !traffic.hasBus -> "Busy cars, horns, voices & breeze"
          else -> type.label
        }
      } else {
        type.label
      }
  }
}

/**
 * Generates and loops subtle, layered procedural soundscapes via AudioTrack:
 * - Rain patter + water droplets + wet street hush when raining
 * - Slow rhythmic midnight crickets + calm night breeze at midnight
 * - Passing car sounds + distant crowd vocal chatter + wind rustling +
 *   car horn beeps when many cars are present + deeper bus horn beeps when buses are present
 */
class AmbientWeatherSoundEngine {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val lock = Any()

  private var audioTrack: AudioTrack? = null
  private var currentType: AmbientSoundType? = null
  private var currentTraffic: TrafficAudioConfig = TrafficAudioConfig()
  private var isEnabled: Boolean = true
  private var isForeground: Boolean = true
  private var prepareJob: Job? = null

  fun updateState(
    enabled: Boolean,
    soundType: AmbientSoundType?,
    foreground: Boolean,
    trafficConfig: TrafficAudioConfig = TrafficAudioConfig(),
  ) {
    val shouldPlay = enabled && foreground && soundType != null
    val needsNewTrack: Boolean
    synchronized(lock) {
      val prevShouldPlay = isEnabled && isForeground && currentType != null
      val typeChanged = soundType != currentType
      val trafficChanged = soundType == AmbientSoundType.STREET_TRAFFIC_AND_VOICES &&
        trafficConfig != currentTraffic
      isEnabled = enabled
      isForeground = foreground

      if (!shouldPlay) {
        prepareJob?.cancel()
        prepareJob = null
        releaseTrackLocked()
        currentType = soundType
        currentTraffic = trafficConfig
        return
      }

      if (!typeChanged && !trafficChanged && prevShouldPlay && audioTrack != null) {
        return
      }
      currentType = soundType
      currentTraffic = trafficConfig
      needsNewTrack = true
    }

    if (needsNewTrack && soundType != null) {
      prepareJob?.cancel()
      prepareJob = scope.launch {
        val pcm = generateLoopBuffer(soundType, trafficConfig)
        synchronized(lock) {
          if (!isEnabled || !isForeground || currentType != soundType || currentTraffic != trafficConfig) {
            return@launch
          }
          releaseTrackLocked()
          audioTrack = createAndStartLoopingTrack(pcm)
        }
      }
    }
  }

  fun release() {
    prepareJob?.cancel()
    prepareJob = null
    synchronized(lock) {
      releaseTrackLocked()
      currentType = null
    }
    scope.cancel()
  }

  private fun releaseTrackLocked() {
    runCatching {
      audioTrack?.let { track ->
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
          track.pause()
        }
        track.flush()
        track.stop()
        track.release()
      }
    }
    audioTrack = null
  }

  private fun createAndStartLoopingTrack(pcm: ShortArray): AudioTrack? =
    runCatching {
      val byteCount = pcm.size * 2
      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(byteCount)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      val written = track.write(pcm, 0, pcm.size)
      if (written <= 0) {
        track.release()
        return@runCatching null
      }
      track.setLoopPoints(0, pcm.size, -1)
      track.setVolume(SUBTLE_VOLUME)
      track.play()
      track
    }.getOrNull()

  companion object {
    const val SAMPLE_RATE = 22050
    const val LOOP_SECONDS = 6
    const val LOOP_SAMPLES = SAMPLE_RATE * LOOP_SECONDS
    private const val CROSSFADE_SAMPLES = SAMPLE_RATE / 3
    private const val SUBTLE_VOLUME = 0.24f

    fun generateLoopBuffer(
      type: AmbientSoundType,
      traffic: TrafficAudioConfig = TrafficAudioConfig(),
    ): ShortArray {
      val totalGen = LOOP_SAMPLES + CROSSFADE_SAMPLES
      val raw = FloatArray(totalGen)
      val seed = 42L + type.ordinal * 997L + traffic.carCount * 31L + traffic.busCount * 131L
      val rng = java.util.Random(seed)

      var lp1 = 0f
      var lp2 = 0f
      var bpState = 0f
      var vocalLp1 = 0f
      var vocalHp1 = 0f
      var engineLp = 0f

      for (i in 0 until totalGen) {
        val white = (rng.nextFloat() * 2f) - 1f
        val tSec = i.toFloat() / SAMPLE_RATE

        when (type) {
          AmbientSoundType.RAIN_SOUNDS -> {
            lp1 += 0.17f * (white - lp1)
            lp2 += 0.38f * (lp1 - lp2)
            raw[i] = lp2 * 0.44f
          }
          AmbientSoundType.THUNDER_RAIN -> {
            lp1 += 0.19f * (white - lp1)
            lp2 += 0.42f * (lp1 - lp2)
            val rumbleEnv = sin((PI * ((tSec % 6f) / 6f)).toDouble()).toFloat().coerceAtLeast(0f)
            val rumble = sin((2.0 * PI * 58.0 * tSec)).toFloat() * 0.13f * rumbleEnv * rumbleEnv
            raw[i] = (lp2 * 0.48f) + rumble
          }
          AmbientSoundType.MIDNIGHT_CRICKETS -> {
            // Soft calm night breeze bed
            val nightLfo = 0.5f + 0.5f * sin(2.0 * PI * 0.17 * tSec).toFloat()
            lp1 += 0.06f * (white - lp1)
            raw[i] = lp1 * (0.11f + 0.06f * nightLfo)
          }
          AmbientSoundType.STREET_TRAFFIC_AND_VOICES -> {
            // 1. Subtle wind rustle open-air bed
            val windLfo = 0.5f + 0.5f * sin(2.0 * PI * 0.33 * tSec).toFloat()
            lp1 += (0.06f + 0.10f * windLfo) * (white - lp1)
            bpState += 0.03f * (lp1 - bpState)
            val windBed = (lp1 - bpState * 0.65f) * (0.16f + 0.12f * windLfo)

            // 2. Soft distant crowd vocal chatter murmur (syllable-rate modulated mid-band noise)
            val whiteVoice = (rng.nextFloat() * 2f) - 1f
            vocalLp1 += 0.24f * (whiteVoice - vocalLp1)
            vocalHp1 += 0.06f * (vocalLp1 - vocalHp1)
            val vocalBand = vocalLp1 - vocalHp1
            val talkEnv1 = (0.5f + 0.5f * sin(2.0 * PI * 3.8 * tSec + 0.4).toFloat())
            val talkEnv2 = (0.5f + 0.5f * sin(2.0 * PI * 5.3 * tSec + 1.9).toFloat())
            val phraseEnv = (0.5f + 0.5f * sin(2.0 * PI * 0.67 * tSec).toFloat())
            val crowdVoices = vocalBand * (0.14f * talkEnv1 * talkEnv2 * (0.4f + 0.6f * phraseEnv))

            // 3. Distant continuous city traffic hum
            engineLp += 0.045f * (white - engineLp)
            val distantTraffic = engineLp * 0.12f

            raw[i] = windBed + crowdVoices + distantTraffic
          }
          AmbientSoundType.SNOW_HUSH -> {
            val lfo = 0.5f + 0.5f * sin(2.0 * PI * 0.20 * tSec).toFloat()
            lp1 += 0.08f * (white - lp1)
            raw[i] = lp1 * (0.14f + 0.08f * lfo)
          }
        }
      }

      // Layer realistic passing car sounds + conditional car horn beeps (when many cars) & bus horn beeps
      if (type == AmbientSoundType.STREET_TRAFFIC_AND_VOICES) {
        val allCenters = listOf(1.1f, 2.35f, 3.4f, 4.75f, 5.45f)
        val passCount = traffic.carCount.coerceIn(1, allCenters.size)
        allCenters.take(passCount).forEachIndexed { idx, centerSec ->
          val halfDurSec = 0.72f
          val startIdx = ((centerSec - halfDurSec) * SAMPLE_RATE).toInt().coerceAtLeast(0)
          val endIdx = ((centerSec + halfDurSec) * SAMPLE_RATE).toInt().coerceAtMost(totalGen - 1)
          var carNoiseLp = 0f
          for (idxSample in startIdx..endIdx) {
            val dt = (idxSample.toFloat() / SAMPLE_RATE) - centerSec
            val norm = (dt / halfDurSec).coerceIn(-1f, 1f)
            val env = (1f - norm * norm) * (1f - norm * norm) // Smooth bell-shaped pass-by
            val dopplerFreq = if (idx % 2 == 0) 92f - norm * 16f else 110f - norm * 20f
            val engineWave =
              sin(2.0 * PI * dopplerFreq * dt).toFloat() * 0.55f +
                sin(2.0 * PI * dopplerFreq * 2.0 * dt).toFloat() * 0.25f
            val tireNoise = (rng.nextFloat() * 2f) - 1f
            carNoiseLp += 0.14f * (tireNoise - carNoiseLp)
            raw[idxSample] += env * (engineWave * 0.055f + carNoiseLp * 0.14f)
          }
        }

        // Car horn beeps ONLY when there are many cars (e.g., 4+ cars on the street)
        if (traffic.hasManyCars) {
          // Crisp double car horn "beep-beep"
          addCarHornBeep(raw, startSec = 1.35f, durSec = 0.11f, f1 = 415f, f2 = 523f, amp = 0.042f, totalGen = totalGen)
          addCarHornBeep(raw, startSec = 1.54f, durSec = 0.13f, f1 = 415f, f2 = 523f, amp = 0.044f, totalGen = totalGen)
          // Second distant car horn "beep"
          addCarHornBeep(raw, startSec = 4.65f, durSec = 0.14f, f1 = 440f, f2 = 554f, amp = 0.032f, totalGen = totalGen)
        }

        // Bus horn beeps when buses are present (deeper, brassier dual air-horn "honk-hooonk")
        if (traffic.hasBus) {
          addBusHornBeep(raw, startSec = 2.75f, durSec = 0.18f, amp = 0.052f, totalGen = totalGen)
          addBusHornBeep(raw, startSec = 3.04f, durSec = 0.30f, amp = 0.058f, totalGen = totalGen)
        }
      }

      // Layer slow midnight cricket chirps (~1 chirp every 1.5s, 4 pulses per chirp)
      if (type == AmbientSoundType.MIDNIGHT_CRICKETS) {
        val primaryChirps = listOf(0.55f, 2.05f, 3.65f, 5.10f)
        primaryChirps.forEach { startSec ->
          addCricketChirp(raw, startSec, baseFreq = 4080f, amplitude = 0.075f, totalGen = totalGen)
        }
        val distantChirps = listOf(1.30f, 4.35f)
        distantChirps.forEach { startSec ->
          addCricketChirp(raw, startSec, baseFreq = 3820f, amplitude = 0.036f, totalGen = totalGen)
        }
      }

      // Layer raindrop patters for rain & storm soundscapes
      if (type == AmbientSoundType.RAIN_SOUNDS || type == AmbientSoundType.THUNDER_RAIN) {
        val dropCount = if (type == AmbientSoundType.THUNDER_RAIN) 52 else 64
        val dropLen = (SAMPLE_RATE * 0.045f).toInt()
        for (d in 0 until dropCount) {
          val startIdx = rng.nextInt(LOOP_SAMPLES)
          val freq = 1050f + rng.nextFloat() * 1550f
          val amp = 0.05f + rng.nextFloat() * 0.07f
          for (k in 0 until dropLen) {
            val idx = startIdx + k
            if (idx >= totalGen) break
            val dt = k.toFloat() / SAMPLE_RATE
            val env = exp(-dt * 95f)
            val tone = sin(2.0 * PI * freq * dt).toFloat()
            raw[idx] += amp * env * tone
          }
        }
      }

      // Equal-power crossfade tail into head for a click-free seamless loop
      val output = ShortArray(LOOP_SAMPLES)
      for (i in 0 until LOOP_SAMPLES) {
        val sample = if (i < CROSSFADE_SAMPLES) {
          val fadeIn = i.toFloat() / CROSSFADE_SAMPLES
          val fadeOut = 1f - fadeIn
          val gainIn = sqrt(fadeIn)
          val gainOut = sqrt(fadeOut)
          raw[i] * gainIn + raw[LOOP_SAMPLES + i] * gainOut
        } else {
          raw[i]
        }
        val clamped = (sample * 0.72f).coerceIn(-0.95f, 0.95f)
        output[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
      }
      return output
    }

    private fun addCricketChirp(
      raw: FloatArray,
      startSec: Float,
      baseFreq: Float,
      amplitude: Float,
      totalGen: Int,
    ) {
      val pulses = 4
      val pulseDurSec = 0.022f
      val pulseStrideSec = 0.042f
      for (p in 0 until pulses) {
        val pStart = ((startSec + p * pulseStrideSec) * SAMPLE_RATE).toInt()
        val pLen = (pulseDurSec * SAMPLE_RATE).toInt()
        for (k in 0 until pLen) {
          val idx = pStart + k
          if (idx >= totalGen) break
          val dt = k.toFloat() / SAMPLE_RATE
          val env = sin(PI * (k.toFloat() / pLen)).toFloat()
          val tone = sin(2.0 * PI * baseFreq * dt).toFloat()
          raw[idx] += env * tone * amplitude
        }
      }
    }

    private fun addCarHornBeep(
      raw: FloatArray,
      startSec: Float,
      durSec: Float,
      f1: Float,
      f2: Float,
      amp: Float,
      totalGen: Int,
    ) {
      val hornStart = (startSec * SAMPLE_RATE).toInt()
      val hornSamples = (durSec * SAMPLE_RATE).toInt().coerceAtLeast(1)
      for (k in 0 until hornSamples) {
        val idx = hornStart + k
        if (idx >= totalGen) break
        val dt = k.toFloat() / SAMPLE_RATE
        val progress = k.toFloat() / hornSamples
        val attack = (progress / 0.12f).coerceIn(0f, 1f)
        val release = ((1f - progress) / 0.16f).coerceIn(0f, 1f)
        val env = attack.coerceAtMost(release)
        val tone =
          sin(2.0 * PI * f1 * dt).toFloat() * 0.54f +
            sin(2.0 * PI * f2 * dt).toFloat() * 0.34f +
            sin(2.0 * PI * f1 * 2.0 * dt).toFloat() * 0.12f
        raw[idx] += env * tone * amp
      }
    }

    private fun addBusHornBeep(
      raw: FloatArray,
      startSec: Float,
      durSec: Float,
      amp: Float,
      totalGen: Int,
    ) {
      val hornStart = (startSec * SAMPLE_RATE).toInt()
      val hornSamples = (durSec * SAMPLE_RATE).toInt().coerceAtLeast(1)
      for (k in 0 until hornSamples) {
        val idx = hornStart + k
        if (idx >= totalGen) break
        val dt = k.toFloat() / SAMPLE_RATE
        val progress = k.toFloat() / hornSamples
        val attack = (progress / 0.10f).coerceIn(0f, 1f)
        val release = ((1f - progress) / 0.18f).coerceIn(0f, 1f)
        val env = attack.coerceAtMost(release)
        // Rich, deep brassy dual-tone air horn characteristic of city buses
        val f1 = 175.0
        val f2 = 220.0
        val tone =
          sin(2.0 * PI * f1 * dt).toFloat() * 0.42f +
            sin(2.0 * PI * f2 * dt).toFloat() * 0.32f +
            sin(2.0 * PI * (f1 * 2.0) * dt).toFloat() * 0.16f +
            sin(2.0 * PI * (f2 * 2.0) * dt).toFloat() * 0.10f
        raw[idx] += env * tone * amp
      }
    }
  }
}
