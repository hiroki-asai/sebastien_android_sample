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

import com.nttdocomo.flow.ErrorEventHandler;

import jp.co.atware.trial_app.R;
import jp.co.atware.trial_app.chat.ChatController.ChatMode;
import jp.co.atware.trial_app.chat.ChatController.ChatStatus;
import jp.co.atware.trial_app.fragment.Alert;
import jp.co.atware.trial_app.fragment.UpdateAccessToken;

/**
 * 対話エラー時の処理
 */
class ChatErrorHandler extends ErrorEventHandler {

    private static final int TOKEN_EXPIRED = 40102;

    private final ChatMode mode;

    /**
     * コンストラクタ
     *
     * @param mode 対話モード
     */
    ChatErrorHandler(ChatMode mode) {
        this.mode = mode;
    }

    @Override
    public void run(int errCode, String message) {
        ChatController chat = ChatController.getInstance();
        // 音声対話中に接続エラーが発生した場合は自動接続を行う
        if (mode == ChatMode.VOICE && errCode != TOKEN_EXPIRED && chat.setAutoStart()) {
            return;
        }
        chat.setStatus(ChatStatus.STOP);
        chat.setSubtitle(R.string.stop);
        if (mode == ChatMode.VOICE) {
            chat.stopVoice();
        } else {
            chat.stopText();
        }
        if (errCode == TOKEN_EXPIRED) {
            chat.show(new UpdateAccessToken());
        } else {
            chat.show(Alert.newInstance(ChatApplication.getInstance().getApplicationContext()
                    .getString(R.string.failed), message));
        }
    }
}
