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

package jp.co.atware.trial_app.chat;

import com.nttdocomo.flow.EventHandler;

import jp.co.atware.trial_app.R;
import jp.co.atware.trial_app.balloon.AudioAdapter;
import jp.co.atware.trial_app.chat.ChatController.ChatMode;
import jp.co.atware.trial_app.chat.ChatController.ChatStatus;

/**
 * 対話開始時の処理
 */
class ChatStartHandler extends EventHandler {

    final ChatMode mode;
    final Object data;

    /**
     * 音声対話用のインスタンスを生成
     *
     * @return ChatStartHandlerインスタンス
     */
    static ChatStartHandler forVoiceMode() {
        return new ChatStartHandler(ChatMode.VOICE, null);
    }

    /**
     * テキストチャット用のインスタンスを生成
     *
     * @param data 送信データ
     * @return ChatStartHandlerインスタンス
     */
    static ChatStartHandler forTextMode(Object data) {
        return new ChatStartHandler(ChatMode.TEXT, data);
    }

    /**
     * コンストラクタ
     *
     * @param mode 対話モード
     * @param data 送信データ
     */
    private ChatStartHandler(ChatMode mode, Object data) {
        this.mode = mode;
        this.data = data;
    }

    @Override
    public void run() {
        ChatController chat = ChatController.getInstance();
        chat.setStatus(ChatStatus.START);
        if (mode == ChatMode.VOICE) {
            chat.setAutoStop();
            if (AudioAdapter.getInstance().isPlaying()) {
                ChatApplication.getInstance().mute();
            } else {
                chat.setSubtitle(R.string.ready_to_talk);
            }
        } else {
            ChatApplication.getInstance().put(data);
            chat.setWaiting();
        }
    }

}
