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

package jp.co.atware.trial_app.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.View;
import android.widget.Button;

import jp.co.atware.trial_app.R;
import jp.co.atware.trial_app.chat.ChatApplication;
import jp.co.atware.trial_app.util.ApiClient;
import jp.co.atware.trial_app.util.ApiClient.ApiCallBack;
import jp.co.atware.trial_app.util.Config;

/**
 * 認証情報更新ダイアログ
 */
public class UpdateAccessToken extends DialogFragment implements ApiCallBack {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        final AlertDialog updateDialog = new Builder(getActivity()).setTitle(R.string.update_token_title)
                .setMessage(R.string.update_token_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.update_token, null).create();

        updateDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button update = updateDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                update.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new ApiClient(UpdateAccessToken.this).update();
                    }
                });
            }
        });
        return updateDialog;
    }

    @Override
    public void onRequestSuccess(String accessToken) {
        ChatApplication.getInstance().setConnection(accessToken);
        Alert.newInstance(null, getString(R.string.update_token_success))
                .show(getFragmentManager(), null);
        dismiss();
    }

    @Override
    public void onRequestFailed(String message) {
        Config.getInstance().removeAccessToken();
        Exit.newInstance(getString(R.string.request_token_failed), message)
                .show(getFragmentManager(), null);
        dismiss();
    }
}
