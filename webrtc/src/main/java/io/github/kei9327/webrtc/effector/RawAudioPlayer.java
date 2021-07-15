/**
 * MIT License
 *
 * Copyright (c) 2016 UCloud Media Development Kits
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.github.kei9327.webrtc.effector;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Process;

import java.util.LinkedList;

public class RawAudioPlayer {

    private static final String TAG = "RawAudioPlayer";

    private static final int STREAM_TYPE = AudioManager.STREAM_MUSIC;
    private static final int BACKUP_STREAM_TYPE = AudioManager.STREAM_VOICE_CALL;

    private static final int SAMPLE_RATE_IN_HZ = 44100;

    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO;
    private static final int BACKUP_CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private final Object syncObject = new Object();

    private final LinkedList<byte[]> audioBuffer = new LinkedList<>();

    private final LinkedList<byte[]> audioPool = new LinkedList<>();

    private boolean isRunning;

    private final int bufferSize;

    public RawAudioPlayer(int bufferSize) {
        isRunning = false;
        this.bufferSize = bufferSize;
    }

    public void start() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        synchronized (syncObject) {
            audioBuffer.clear();
        }
        PlayThread playThread = new PlayThread(bufferSize);
        playThread.start();
    }

    public void stop() {
        isRunning = false;
    }

    public void sendAudio(byte[] data) {
        synchronized (syncObject) {
            if (!isRunning) {
                return;
            }
            byte[] tempData = get(data.length);
            System.arraycopy(data, 0, tempData, 0, tempData.length);
            audioBuffer.add(tempData);
        }
    }

    public boolean isPlaying() {
        return isRunning;
    }

    private final class PlayThread extends Thread {

        private final int bufferSize;

        private PlayThread(int bufferSize) {
            super("RawAudioPlayer-PlayThread");
            this.bufferSize = bufferSize;
        }

        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            // AudioTrack이 두개인 이유는 합성용과 스피커 송출용 두가지 나눠지기 때문임.
            AudioTrack audioTrack = null;
            AudioTrack backupAudioTrack = null;
            while (isRunning && (audioTrack == null || (audioTrack.getState() != AudioRecord.STATE_INITIALIZED))) {
                if (audioTrack != null) {
                    audioTrack.release();
                }
                audioTrack = new AudioTrack(STREAM_TYPE, SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize, AudioTrack.MODE_STREAM);
                backupAudioTrack = new AudioTrack(BACKUP_STREAM_TYPE, SAMPLE_RATE_IN_HZ, BACKUP_CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize, AudioTrack.MODE_STREAM);
                yield();
            }
            audioTrack.play();
            backupAudioTrack.play();
            while (isRunning) {
                synchronized (syncObject) {
                    if (!audioBuffer.isEmpty()) {
                        byte[] data = audioBuffer.removeFirst();
                        audioTrack.write(data, 0, data.length);
                        backupAudioTrack.write(data, 0, data.length);
                        release(data);
                    }
                }
            }
            audioTrack.stop();
            audioTrack.release();
        }
    }

    private byte[] get(int size) {
        synchronized (syncObject) {
            if (audioPool.size() > 0) {
                return audioPool.removeFirst();
            }
            else {
                return new byte[size];
            }
        }
    }

    private void release(byte[] data) {
        synchronized (syncObject) {
            if (audioPool.size() < 2) {
                audioPool.add(data);
            }
        }
    }
}