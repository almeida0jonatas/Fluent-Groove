package com.example.data

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.model.Track
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates a real playable 28-second stereo PCM WAV synthwave demo track locally
 * and provides complete artist, album, and bundled artwork metadata.
 */
object DemoTrackProvider {

    const val DEMO_ARTIST_NAME = "Lumen Wave"
    const val DEMO_ALBUM_TITLE = "Neon Skies"
    const val DEMO_TRACK_TITLE = "Midnight Horizon"
    private const val DURATION_SECONDS = 28
    private const val SAMPLE_RATE = 22050

    fun ensureDemoTrack(context: Context): Pair<Track, String> {
        val demoDir = File(context.filesDir, "demo")
        if (!demoDir.exists()) demoDir.mkdirs()
        val wavFile = File(demoDir, "demo_midnight_horizon.wav")
        if (!wavFile.exists() || wavFile.length() < 1024L) {
            writeSynthesizedDemoWav(wavFile)
        }

        val artistId = AudioMetadataParser.makeSlugId("art", DEMO_ARTIST_NAME)
        val albumId = AudioMetadataParser.makeSlugId("alb", "$DEMO_ARTIST_NAME-$DEMO_ALBUM_TITLE")
        val trackId = AudioMetadataParser.createTrackIdFromTags(
            DEMO_ARTIST_NAME,
            DEMO_ALBUM_TITLE,
            DEMO_TRACK_TITLE,
            1,
        )
        val artDesign = AudioMetadataParser.createArtDesignForKey("$DEMO_ARTIST_NAME::$DEMO_ALBUM_TITLE")

        val coverUri = "android.resource://${context.packageName}/${R.drawable.img_demo_album_cover}"
        val artistPhotoUri = "android.resource://${context.packageName}/${R.drawable.img_demo_artist_photo}"

        val track = Track(
            id = trackId,
            title = DEMO_TRACK_TITLE,
            artist = DEMO_ARTIST_NAME,
            artistId = artistId,
            album = DEMO_ALBUM_TITLE,
            albumId = albumId,
            trackNumber = 1,
            duration = DURATION_SECONDS,
            genre = "Synthwave",
            year = 2026,
            addedAt = System.currentTimeMillis(),
            fileName = "Midnight Horizon.wav",
            contentUri = Uri.fromFile(wavFile).toString(),
            coverUri = coverUri,
            artDesign = artDesign,
        )

        return track to artistPhotoUri
    }

    private fun writeSynthesizedDemoWav(outputFile: File) {
        val totalFrames = SAMPLE_RATE * DURATION_SECONDS
        val numChannels = 2
        val bitsPerSample = 16
        val byteRate = SAMPLE_RATE * numChannels * (bitsPerSample / 8)
        val blockAlign = numChannels * (bitsPerSample / 8)
        val dataSize = totalFrames * blockAlign

        // Chord progression (Am9 -> Fmaj7 -> Cmaj7 -> G6)
        val bassFreqs = doubleArrayOf(110.00, 87.31, 130.81, 98.00)
        val padChords = arrayOf(
            doubleArrayOf(220.00, 261.63, 329.63, 392.00, 493.88),
            doubleArrayOf(174.61, 220.00, 261.63, 329.63, 392.00),
            doubleArrayOf(261.63, 329.63, 392.00, 493.88, 587.33),
            doubleArrayOf(196.00, 246.94, 293.66, 329.63, 440.00),
        )
        val arpNotes = arrayOf(
            doubleArrayOf(440.00, 523.25, 659.25, 783.99, 659.25, 523.25, 493.88, 659.25),
            doubleArrayOf(349.23, 440.00, 523.25, 659.25, 523.25, 440.00, 392.00, 523.25),
            doubleArrayOf(523.25, 659.25, 783.99, 987.77, 783.99, 659.25, 587.33, 783.99),
            doubleArrayOf(392.00, 493.88, 587.33, 659.25, 587.33, 493.88, 440.00, 587.33),
        )

        val chordDurationSec = 3.5
        val arpStepSec = chordDurationSec / 8.0
        val twoPi = 2.0 * PI

        BufferedOutputStream(FileOutputStream(outputFile), 65536).use { out ->
            // RIFF/WAVE header (44 bytes)
            fun writeAscii(text: String) = out.write(text.toByteArray(Charsets.US_ASCII))
            fun writeIntLE(v: Int) {
                out.write(v and 0xFF)
                out.write((v shr 8) and 0xFF)
                out.write((v shr 16) and 0xFF)
                out.write((v shr 24) and 0xFF)
            }
            fun writeShortLE(v: Int) {
                out.write(v and 0xFF)
                out.write((v shr 8) and 0xFF)
            }

            writeAscii("RIFF")
            writeIntLE(36 + dataSize)
            writeAscii("WAVE")
            writeAscii("fmt ")
            writeIntLE(16)
            writeShortLE(1) // PCM
            writeShortLE(numChannels)
            writeIntLE(SAMPLE_RATE)
            writeIntLE(byteRate)
            writeShortLE(blockAlign)
            writeShortLE(bitsPerSample)
            writeAscii("data")
            writeIntLE(dataSize)

            val buffer = ByteArray(4096)
            var bufIdx = 0

            for (i in 0 until totalFrames) {
                val t = i.toDouble() / SAMPLE_RATE.toDouble()
                val chordIndex = ((t / chordDurationSec).toInt()) % 4
                val chordLocalT = t % chordDurationSec

                // Smooth global envelope (fade-in 1.4s, fade-out 2.4s)
                val fadeIn = (t / 1.4).coerceIn(0.0, 1.0)
                val fadeOut = ((DURATION_SECONDS - t) / 2.4).coerceIn(0.0, 1.0)
                val masterEnv = fadeIn * fadeOut

                // Warm analog bass with gentle 8th-note pulse
                val bassF = bassFreqs[chordIndex]
                val pulseEnv = 0.65 + 0.35 * sin(twoPi * (t / (arpStepSec * 2.0)))
                val bassSample = (sin(twoPi * bassF * t) * 0.75 + sin(twoPi * bassF * 2.0 * t) * 0.25) * 0.28 * pulseEnv

                // Lush detuned stereo synth pad
                val padBreath = 0.78 + 0.22 * sin(twoPi * 0.28 * t)
                val chordEnv = (chordLocalT / 0.18).coerceIn(0.0, 1.0) *
                    (((chordDurationSec - chordLocalT) / 0.25).coerceIn(0.0, 1.0))
                var padL = 0.0
                var padR = 0.0
                val notes = padChords[chordIndex]
                for (nIdx in notes.indices) {
                    val f = notes[nIdx]
                    val detune = 1.0 + (nIdx - 2) * 0.0018
                    padL += sin(twoPi * f * detune * t) * 0.075
                    padR += sin(twoPi * f * (2.0 - detune) * t) * 0.075
                }
                padL *= padBreath * chordEnv
                padR *= padBreath * chordEnv

                // Sparkling electric-piano bell arpeggio
                val stepIdx = ((chordLocalT / arpStepSec).toInt()).coerceIn(0, 7)
                val stepLocalT = chordLocalT - stepIdx * arpStepSec
                val arpFreq = arpNotes[chordIndex][stepIdx]
                val arpEnv = (stepLocalT / 0.012).coerceIn(0.0, 1.0) * exp(-stepLocalT * 4.2)
                val arpTone = (sin(twoPi * arpFreq * t) + 0.35 * sin(twoPi * arpFreq * 2.0 * t) * exp(-stepLocalT * 8.0)) * 0.16 * arpEnv
                val pan = if (stepIdx % 2 == 0) 0.62 else 0.38

                val left = ((bassSample + padL + arpTone * pan) * masterEnv).coerceIn(-0.95, 0.95)
                val right = ((bassSample + padR + arpTone * (1.0 - pan)) * masterEnv).coerceIn(-0.95, 0.95)

                val lPcm = (left * 32767.0).toInt()
                val rPcm = (right * 32767.0).toInt()

                buffer[bufIdx++] = (lPcm and 0xFF).toByte()
                buffer[bufIdx++] = ((lPcm shr 8) and 0xFF).toByte()
                buffer[bufIdx++] = (rPcm and 0xFF).toByte()
                buffer[bufIdx++] = ((rPcm shr 8) and 0xFF).toByte()

                if (bufIdx >= buffer.size) {
                    out.write(buffer, 0, bufIdx)
                    bufIdx = 0
                }
            }
            if (bufIdx > 0) {
                out.write(buffer, 0, bufIdx)
            }
        }
    }
}
