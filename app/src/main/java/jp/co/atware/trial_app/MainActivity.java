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

package jp.co.atware.trial_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import jp.co.atware.trial_app.chat.ChatApplication;
import jp.co.atware.trial_app.chat.ChatController;
import jp.co.atware.trial_app.fragment.EditConfig;
import jp.co.atware.trial_app.fragment.Exit;
import jp.co.atware.trial_app.fragment.ResetAccessToken;
import jp.co.atware.trial_app.util.Config;
import jp.co.atware.trial_app.util.URLConstants;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;

/**
 * 対話アプリ画面
 */
public class MainActivity extends AppCompatActivity implements OnMenuItemClickListener {

    private static final int REQUEST_CODE = 1000;
    private static final String[] REQUIRED_PERMISSIONS = {RECORD_AUDIO, READ_PHONE_STATE};

    private ChatApplication app;

    /**
     * Voice Command起動判定
     *
     * @return Voice Commandで起動した場合にtrue
     */
    public boolean isStartVoiceCommand() {
        return getIntent().getAction().equals(Intent.ACTION_VOICE_COMMAND);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Config.getInstance().getAccessToken() != null && isStartVoiceCommand()) {
            app.startOnEnableHFP(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_main);
        app = (ChatApplication) getApplication();
        // 必須権限チェック
        List<String> requests = new ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
                requests.add(permission);
            }
        }
        if (requests.isEmpty()) {
            app.init(this);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(requests.toArray(new String[0]), REQUEST_CODE);
        } else {
            alertPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            int granted = 0;
            for (int result : grantResults) {
                if (result == PERMISSION_GRANTED) {
                    granted++;
                }
            }
            if (permissions.length == granted) {
                app.init(this);
            } else {
                alertPermissions();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * 必須権限が許可されていない場合の警告を表示
     */
    private void alertPermissions() {
        DialogFragment dialog = Exit.newInstance(null, getString(R.string.permission_required));
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(dialog, null);
        ft.commitAllowingStateLoss();
    }

    /**
     * Webブラウザの表示
     *
     * @param url 表示するURL
     */
    public void startBrowser(String url) {
        Intent browser = new Intent(Intent.ACTION_VIEW);
        browser.setData(Uri.parse(url));
        startActivity(browser);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        ChatController chat = ChatController.getInstance();
        switch (item.getItemId()) {
            case R.id.mic:
                if (chat.isVoiceMode()) {
                    chat.stopVoice();
                } else {
                    chat.startVoice(false);
                }
                break;
            case R.id.keyboard:
                if (chat.isTextMode()) {
                    chat.stopText();
                } else {
                    chat.startText();
                }
                break;
            case R.id.link_uds:
                startBrowser(URLConstants.USER_DASHBOARD);
                break;
            case R.id.link_dds:
                startBrowser(URLConstants.DEVELOPER_DASHBOARD);
                break;
            case R.id.app_info:
                startActivity(new Intent(this, AppInfoActivity.class));
                break;
            case R.id.config:
                new EditConfig().show(getSupportFragmentManager(), null);
                break;
            case R.id.initAuth:
                new ResetAccessToken().show(getSupportFragmentManager(), null);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    protected void onPause() {
        app.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        app.onDestroy();
        app = null;
        super.onDestroy();
        Process.killProcess(Process.myPid());
    }

    @Override
    public void onBackPressed() {
        finishAndRemoveTask();
    }

}