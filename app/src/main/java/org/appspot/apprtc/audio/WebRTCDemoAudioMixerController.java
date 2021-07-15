/**
 * MIT License
 * <p>
 * Copyright (c) 2021 Janghyeok Park
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.appspot.apprtc.audio;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import io.github.kei9327.webrtc.audio.BaseMixAudioController;
import io.github.kei9327.webrtc.effector.AverageAudioMixer;
import io.github.kei9327.webrtc.effector.RawAudioPlayer;

public class WebRTCDemoAudioMixerController extends BaseMixAudioController {

    // Default audio data format is PCM 16 bit per sample.
    // Guaranteed to be supported by all devices.
    private static final int BITS_PER_SAMPLE = 16;
    // Requested size of each recorded buffer provided to the client.
    private static final int CALLBACK_BUFFER_SIZE_MS = 10;
    // Average number of callbacks per second.
    private static final int BUFFERS_PER_SECOND = 1000 / CALLBACK_BUFFER_SIZE_MS;

    private InputStream bgmInputStream;
    private byte[] bgm;
    private Context context;
    private AverageAudioMixer mMixer;
    private float bgmAudioLevel = 1.0f;
    private float micAudioLevel = 1.0f;
    private RawAudioPlayer rawAudioPlayer;
    private boolean isLooper = false;

    private int bufferSize;

    private byte[] originCacheBuffer;
    private boolean isInitial = false;

    public WebRTCDemoAudioMixerController(Context context, boolean isLooper) {
        this.context = context;
        this.isLooper = isLooper;
    }

    @Override
    public boolean start() {
        return mMixer != null;
    }

    private void init(int chanelCount, int sampleRate) {
        if (!isInitial) {
            synchronized (this) {

                int bytesPerFrame = chanelCount * (BITS_PER_SAMPLE / 8);
                bufferSize = bytesPerFrame * (sampleRate / BUFFERS_PER_SECOND);
                replay();

                isInitial = true;
            }
        }
    }

    private void replay() {
        try {
            bgmInputStream = context.getAssets().open("raw_audio_mix_bg.pcm");
            bgmInputStream.mark(bgmInputStream.available());
            bgm = new byte[bufferSize];
            mMixer = new AverageAudioMixer();
        } catch (Exception e) {
            bgmInputStream = null;
            Log.e(WebRTCDemoAudioMixerController.class.getCanonicalName(), "RawAudioMixFilter->onInit failed.");
        }
        rawAudioPlayer = new RawAudioPlayer(bufferSize);
    }

    @Override
    public void stop() {
        try {
            if (bgmInputStream != null) {
                bgmInputStream.close();
                bgmInputStream = null;
            }
            bgm = null;
            if (mMixer != null) {
                mMixer.dispose();
                mMixer = null;
            }
            if (rawAudioPlayer != null) {
                rawAudioPlayer.stop();
                rawAudioPlayer = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] onProcessAudioBuffer(int audioFormat, int chanelCount, int sampleRate, ByteBuffer sampleBuffer) {
        init(chanelCount, sampleRate);

        try {
            if (bgmInputStream == null || bgmInputStream.read(bgm, 0, bufferSize) < bufferSize) {
                handleReadException();
                return Arrays.copyOfRange(sampleBuffer.array(), sampleBuffer.arrayOffset(),
                        sampleBuffer.capacity() + sampleBuffer.arrayOffset());
            }
        } catch (Exception e) {
            handleReadException();
            return Arrays.copyOfRange(sampleBuffer.array(), sampleBuffer.arrayOffset(),
                    sampleBuffer.capacity() + sampleBuffer.arrayOffset());
        }

        if (rawAudioPlayer != null) {
            if (bgm != null) {
                if (!rawAudioPlayer.isPlaying()) {
                    rawAudioPlayer.start();
                }
                rawAudioPlayer.sendAudio(bgm);
            } else {
                rawAudioPlayer.stop();
            }
        }


        int len = sampleBuffer.limit();
        if (originCacheBuffer == null) {
            originCacheBuffer = new byte[len];
        }
        sampleBuffer.position(0);
        sampleBuffer.get(originCacheBuffer, 0, sampleBuffer.limit());

        // TODO : 여기에 트랙 추가하면 효과음 처리 가능
        byte[][] bytes = {mMixer.scale(bgm, bufferSize, bgmAudioLevel),
                mMixer.scale(originCacheBuffer, bufferSize, micAudioLevel)};

        return mMixer.mixRawAudioBytes(bytes);

    }

    //just support PCM s16le,
    // background music pcm:  samplerate = 44100 channels = 2, pcm format = s16le if different with UAudioProfile demo may be error.
    private void handleReadException() {
        if (isLooper) {
            replay();
        }
        if (rawAudioPlayer != null) {
            rawAudioPlayer.stop();
        }
    }

    public void adjustBGMAudioLevel(float level) {
        bgmAudioLevel = level;
    }

    public void adjustLocalAudioAudioLevel(float level) {
        this.micAudioLevel = level;
    }

}
