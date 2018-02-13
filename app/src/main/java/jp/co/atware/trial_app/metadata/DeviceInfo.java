/*
 * Copyright (c) 2017, atWare, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *  Neither the name of the atWare, Inc. nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL atWare, Inc. BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jp.co.atware.trial_app.metadata;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * デバイス情報
 */
public class DeviceInfo {

    public static final String KEY = "deviceInfo";

    /**
     * 合成音声再生フラグ
     */
    public enum PlayTTS {
        ON, OFF;

        @JsonValue
        public String toValue() {
            return name().toLowerCase();
        }

        public static PlayTTS fromValue(String value) {
            for (PlayTTS playTTS : values()) {
                if (playTTS.toValue().equals(value)) {
                    return playTTS;
                }
            }
            throw new IllegalArgumentException("playTTS:" + value + " is invalid.");
        }
    }

    public String deviceName;
    public PlayTTS playTTS;

    /**
     * デフォルトコンストラクタ
     */
    public DeviceInfo() {

    }

    /**
     * フルコンストラクタ
     *
     * @param deviceName デバイス名
     * @param playTTS    合成音声再生フラグ
     */
    public DeviceInfo(String deviceName, PlayTTS playTTS) {
        this.deviceName = deviceName;
        this.playTTS = playTTS;
    }

}
