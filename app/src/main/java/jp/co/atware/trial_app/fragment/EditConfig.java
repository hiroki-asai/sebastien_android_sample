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
import android.widget.CheckBox;
import android.widget.EditText;

import jp.co.atware.trial_app.R;
import jp.co.atware.trial_app.chat.ChatApplication;
import jp.co.atware.trial_app.util.Config;
import jp.co.atware.trial_app.util.Config.Keys;


/**
 * 接続設定ダイアログ
 */
public class EditConfig extends DialogFragment {


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Config config = Config.getInstance();
        View configView = getActivity().getLayoutInflater().inflate(R.layout.edit_config, null);
        // SSLを使用
        final CheckBox ssl = (CheckBox) configView.findViewById(R.id.ssl);
        ssl.setChecked(config.isSSL());
        // OCSPを使用
        final CheckBox ocsp = (CheckBox) configView.findViewById(R.id.ocsp);
        ocsp.setChecked(config.isOCSP());
        // ホスト名
        final EditText host = (EditText) configView.findViewById(R.id.edit_host);
        host.setText(config.getHost());
        // ポート番号
        final EditText port = (EditText) configView.findViewById(R.id.edit_port);
        port.setText(config.getPort().toString());
        // URLパス
        final EditText path = (EditText) configView.findViewById(R.id.edit_path);
        path.setText(config.getPath());

        final AlertDialog configDialog = new Builder(getActivity()).setView(configView)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        boolean sslChanged = config.setSSL(ssl.isChecked());
                        boolean ocspChanged = config.setOCSP(ocsp.isChecked());
                        boolean hostChanged = config.setHost(host.getText().toString());
                        boolean portChanged = config.setPort(port.getText().toString());
                        boolean pathChanged = config.setPath(path.getText().toString());
                        if (sslChanged || ocspChanged || hostChanged || portChanged || pathChanged) {
                            ChatApplication app = ChatApplication.getInstance();
                            app.onPause();
                            app.setConnection(config.getAccessToken());
                        }
                    }
                })
                .setNeutralButton(R.string.revert, null)
                .setNegativeButton(R.string.cancel, null).create();
        // 初期値に戻すボタンクリック時の動作
        configDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button restore = configDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                restore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ssl.setChecked(Boolean.valueOf(Keys.SSL.defaultValue));
                        ocsp.setChecked(Boolean.valueOf(Keys.OCSP.defaultValue));
                        host.setText(Keys.HOST.defaultValue);
                        port.setText(Keys.PORT.defaultValue);
                        path.setText(Keys.PATH.defaultValue);
                    }
                });
            }
        });
        return configDialog;
    }

}
