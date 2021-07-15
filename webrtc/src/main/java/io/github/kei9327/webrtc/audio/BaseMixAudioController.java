/**
 * MIT License
 *
 * Copyright (c) 2021 Janghyeok Park
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
package io.github.kei9327.webrtc.audio;

import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.voiceengine.WebRtcAudioRecord;

import java.nio.ByteBuffer;

public abstract class BaseMixAudioController implements JavaAudioDeviceModule.SamplesMixAvailableCallback, WebRtcAudioRecord.WebRtcAudioRecordSamplesMixAvailableCallback {

    public abstract boolean start();

    public abstract void stop();

    public abstract byte[] onProcessAudioBuffer(int audioFormat, int chanelCount, int sampleRate, ByteBuffer sampleBuffer);

    @Override
    public byte[] onWebRtcAudioRecordSamplesMixAvailable(WebRtcAudioRecord.AudioMixSamples samples) {
        return onWebRtcAudioRecordSamplesMixAvailable(new JavaAudioDeviceModule.AudioMixSamples(samples.getAudioFormat(),
                samples.getChannelCount(), samples.getSampleRate(), samples.getData()));
    }

    @Override
    public byte[] onWebRtcAudioRecordSamplesMixAvailable(JavaAudioDeviceModule.AudioMixSamples samples) {
        return onProcessAudioBuffer(samples.getAudioFormat(), samples.getChannelCount(), samples.getSampleRate(), samples.getData());
    }
}
