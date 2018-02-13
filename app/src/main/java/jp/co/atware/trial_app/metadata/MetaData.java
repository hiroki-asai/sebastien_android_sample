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

import java.util.ArrayList;
import java.util.List;

import jp.co.atware.trial_app.balloon.Balloon;

/**
 * メタデータ
 */
public class MetaData {

    /**
     * メタデータ区分
     */
    public enum MetaDataType {
        SPEECHREC_RESULT, NLU_RESULT;
    }

    public final MetaDataType type;
    public final List<Balloon> balloons = new ArrayList<>();
    public SwitchAgent switchAgent;
    public Postback postback;
    public boolean utterance;

    /**
     * コンストラクタ
     *
     * @param type メタデータ区分
     */
    public MetaData(MetaDataType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetaData metaData = (MetaData) o;

        if (utterance != metaData.utterance) return false;
        if (type != metaData.type) return false;
        if (!balloons.equals(metaData.balloons)) return false;
        if (switchAgent != null ? !switchAgent.equals(metaData.switchAgent) : metaData.switchAgent != null)
            return false;
        return postback != null ? postback.equals(metaData.postback) : metaData.postback == null;

    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + balloons.hashCode();
        result = 31 * result + (switchAgent != null ? switchAgent.hashCode() : 0);
        result = 31 * result + (postback != null ? postback.hashCode() : 0);
        result = 31 * result + (utterance ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MetaData{" +
                "type=" + type +
                ", balloons=" + balloons +
                ", switchAgent=" + switchAgent +
                ", postback=" + postback +
                ", utterance=" + utterance +
                '}';
    }
}
