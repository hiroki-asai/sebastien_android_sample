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

package jp.co.atware.trial_app.balloon;

import android.support.annotation.LayoutRes;

import java.util.Collections;
import java.util.List;

import jp.co.atware.trial_app.R;

/**
 * 吹き出し
 */
public class Balloon {

    /**
     * 吹き出し種別
     */
    public enum BalloonType {
        USER_VOICE(R.layout.balloon_voice_user),
        AI_VOICE(R.layout.balloon_voice_ai),
        AUDIO(R.layout.balloon_audio),
        IMAGE(R.layout.balloon_image),
        HTML(R.layout.balloon_html),
        BUTTON(R.layout.balloon_button),
        COMPOUND(R.layout.balloon_compound);

        public final int layout;

        BalloonType(@LayoutRes int layout) {
            this.layout = layout;
        }
    }

    /**
     * 表示アクション種別
     */
    public enum Action {
        SCROLL, PLAY, PLAY_AFTER_UTT, DO_NOTHING;
    }

    public final BalloonType type;
    public List<Payload> payloads;
    public Action action = Action.DO_NOTHING;
    public Integer position;

    /**
     * コンストラクタ
     *
     * @param type 吹き出し種別
     */
    public Balloon(BalloonType type) {
        this.type = type;
    }

    /**
     * フルコンストラクタ
     *
     * @param type     吹き出し種別
     * @param payloads 吹き出し表示データ
     */
    public Balloon(BalloonType type, List<Payload> payloads) {
        this(type);
        this.payloads = payloads;
    }

    /**
     * 吹き出し表示データが単一文字列のみの場合に使用可能な簡易コンストラクタ
     *
     * @param type 吹き出し種別
     * @param text 文字列
     */
    public Balloon(BalloonType type, String text) {
        this(type);
        if (text != null) {
            Payload payload = new Payload();
            switch (type) {
                case USER_VOICE:
                case AI_VOICE:
                    payload.text = text;
                    break;
                case IMAGE:
                    payload.url = text;
                    action = Action.SCROLL;
                    break;
                case AUDIO:
                case HTML:
                    payload.url = text;
                    break;
                default:
                    throw new IllegalArgumentException("balloon type not allowed.");
            }
            this.payloads = Collections.singletonList(payload);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Balloon balloon = (Balloon) o;

        if (type != balloon.type) return false;
        if (payloads != null ? !payloads.equals(balloon.payloads) : balloon.payloads != null)
            return false;
        return action == balloon.action;

    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (payloads != null ? payloads.hashCode() : 0);
        result = 31 * result + (action != null ? action.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Balloon{" +
                "type=" + type +
                ", payloads=" + payloads +
                ", action=" + action +
                '}';
    }
}
