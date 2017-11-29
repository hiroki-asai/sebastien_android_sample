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
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import jp.co.atware.trial_app.chat.ChatController;
import jp.co.atware.trial_app.fragment.ConfigDialog;
import jp.co.atware.trial_app.fragment.InitAuthDialog;
import jp.co.atware.trial_app.fragment.KillProcessDialog;
import jp.co.atware.trial_app.util.URLConstants;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;

/**
 * 対話アプリ
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1000;

    public ChatController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_main);
        controller = new ChatController(this);
        // SDKの初期化
        if (checkPermissions(RECORD_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)) {
            controller.initSdk();
        }
    }

    /**
     * 必須権限チェック
     *
     * @param required 必須な権限名の配列
     * @return 必須権限が許可されている場合にtrue
     */
    private boolean checkPermissions(String... required) {
        List<String> requests = new ArrayList<>();
        for (String permission : required) {
            if (ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
                requests.add(permission);
            }
        }
        if (requests.isEmpty()) {
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(requests.toArray(new String[0]), REQUEST_CODE);
        } else {
            alertPermissions();
        }
        return false;
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
                // SDKの初期化
                controller.initSdk();
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
        DialogFragment dialog = KillProcessDialog.newInstance("以下の許可が必要です", "ストレージ、マイク");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(dialog, "permission_denied");
        ft.commitAllowingStateLoss();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        controller.setMenu(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.voice_mode:
                controller.toggleVoice();
                break;
            case R.id.text_mode:
                controller.toggleText();
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
                new ConfigDialog().show(getSupportFragmentManager(), "config");
                break;
            case R.id.initAuth:
                new InitAuthDialog().show(getSupportFragmentManager(), "init_auth");
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * Webブラウザの表示
     *
     * @param url 表示するURL
     */
    private void startBrowser(String url) {
        Intent browser = new Intent(Intent.ACTION_VIEW);
        browser.setData(Uri.parse(url));
        startActivity(browser);
    }

    @Override
    protected void onPause() {
        controller.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        controller = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        controller.quit();
    }

}