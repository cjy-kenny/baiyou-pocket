package com.baiyou.pocket.desktop;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/** 桌面端合成音效（正弦波短音，与 Android 端 ToneGenerator 对位的提示音）。
 *  无音频设备时静默失败。 */
public final class Beep {
    private Beep() {
    }

    public static void play(int kind) {
        try {
            switch (kind) {
                case 1: tone(990, 110); break;
                case 2: tone(300, 170); break;
                case 3: tone(660, 90); tone(880, 90); tone(1320, 140); break;
                default: tone(660, 80); break;
            }
        } catch (Throwable ignore) {
        }
    }

    private static void tone(int hz, int ms) throws Exception {
        float rate = 22050f;
        int n = (int) (rate * ms / 1000);
        byte[] pcm = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double t = i / rate;
            // 首尾 5ms 淡入淡出防爆音
            double env = Math.min(1, Math.min(i / (rate * 0.005), (n - i) / (rate * 0.005)));
            double v = Math.sin(2 * Math.PI * hz * t) * 0.25 * env;
            short s = (short) (v * 32767);
            pcm[i * 2] = (byte) s;
            pcm[i * 2 + 1] = (byte) (s >> 8);
        }
        SourceDataLine line = AudioSystem.getSourceDataLine(
                new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, (int) rate, 16, 1, 2, (int) rate, false));
        line.open();
        line.start();
        line.write(pcm, 0, pcm.length);
        line.drain();
        line.close();
    }
}
