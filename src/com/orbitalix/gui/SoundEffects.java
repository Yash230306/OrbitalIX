package com.orbitalix.gui;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Procedural sci-fi sound effects synthesizer utilizing standard JDK audio APIs.
 * Generates custom synthesized waveforms (sine, square, FM chirps) in memory without any external audio files.
 * Demonstrates: audio signal synthesis, encapsulation, Singleton pattern.
 */
public class SoundEffects {

    private static final SoundEffects INSTANCE = new SoundEffects();
    private boolean soundEnabled = true;

    private SoundEffects() {}

    public static SoundEffects getInstance() {
        return INSTANCE;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    public void toggleSound() {
        this.soundEnabled = !this.soundEnabled;
    }

    /**
     * Plays a brief high-tech interface chirp.
     */
    public void playClick() {
        if (!soundEnabled) return;
        playToneAsync(1200, 1800, 35, 0.15f);
    }

    /**
     * Plays a dual-tone sci-fi telemetry beep.
     */
    public void playTelemetryTick() {
        if (!soundEnabled) return;
        playToneAsync(880, 880, 20, 0.08f);
    }

    /**
     * Plays a tactical warning alarm chirp.
     */
    public void playWarning() {
        if (!soundEnabled) return;
        new Thread(() -> {
            playToneSync(650, 750, 80, 0.22f);
            try { Thread.sleep(40); } catch (InterruptedException ignored) {}
            playToneSync(750, 850, 90, 0.25f);
        }).start();
    }

    /**
     * Plays an urgent emergency klaxon alarm for critical failures.
     */
    public void playCritical() {
        if (!soundEnabled) return;
        new Thread(() -> {
            for (int i = 0; i < 2; i++) {
                playToneSync(950, 480, 110, 0.35f);
                try { Thread.sleep(30); } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    /**
     * Plays a deep hum/chime indicating autonomous safe mode / recovery action.
     */
    public void playRecovery() {
        if (!soundEnabled) return;
        playToneAsync(320, 580, 240, 0.25f);
    }

    private void playToneAsync(double startFreq, double endFreq, int durationMs, float volume) {
        new Thread(() -> playToneSync(startFreq, endFreq, durationMs, volume)).start();
    }

    private void playToneSync(double startFreq, double endFreq, int durationMs, float volume) {
        int sampleRate = 44100;
        int numSamples = (sampleRate * durationMs) / 1000;
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double progress = (double) i / numSamples;
            double currentFreq = startFreq + (endFreq - startFreq) * progress;
            double angle = 2.0 * Math.PI * currentFreq * i / sampleRate;

            // Apply smooth attack and decay envelope to prevent audio clicking
            double envelope = 1.0;
            if (progress < 0.1) {
                envelope = progress / 0.1;
            } else if (progress > 0.7) {
                envelope = (1.0 - progress) / 0.3;
            }

            short sample = (short) (Math.sin(angle) * 32767 * volume * envelope);
            // 16-bit signed PCM big-endian
            buffer[2 * i] = (byte) ((sample >> 8) & 0xFF);
            buffer[2 * i + 1] = (byte) (sample & 0xFF);
        }

        try {
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, true);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format, buffer.length);
            line.start();
            line.write(buffer, 0, buffer.length);
            line.drain();
            line.close();
        } catch (LineUnavailableException | IllegalArgumentException ignored) {
            // Audio device not available or muted in background
        }
    }
}
