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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.util.List;

import jp.co.atware.trial_app.MainActivity;
import jp.co.atware.trial_app.R;
import jp.co.atware.trial_app.util.ApiClient;
import jp.co.atware.trial_app.util.ApiClient.ApiCallBack;
import jp.co.atware.trial_app.util.Config;
import jp.co.atware.trial_app.util.LoginClient;
import jp.co.atware.trial_app.util.LoginClient.LoginCallBack;
import okhttp3.Cookie;

import static jp.co.atware.trial_app.util.URLConstants.USER_DASHBOARD;


/**
 * ユーザダッシュボード画面を表示
 * ログインが成功するとアクセストークンを取得する
 */
public class UserDashboard extends Fragment implements LoginCallBack, ApiCallBack {


    private boolean update = false;
    private Progress progress;

    /**
     * アクセストークン更新用のインスタンスを取得
     *
     * @return 更新用のUserDashboardインスタンス
     */
    public static UserDashboard forUpdate() {
        UserDashboard uds = new UserDashboard();
        uds.update = true;
        return uds;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.login_uds, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        WebView uds = (WebView) view.findViewById(R.id.uds);
        uds.setWebViewClient(new LoginClient(this));
        uds.getSettings().setJavaScriptEnabled(true);
        uds.getSettings().setUserAgentString("Mozilla/5.0 Google");
        uds.loadUrl(USER_DASHBOARD);
    }

    @Override
    public void onLoginSuccess(List<Cookie> cookies) {
        progress = Progress.newInstance("認証情報取得中", false);
        progress.setTargetFragment(this, 0);
        progress.show(getFragmentManager(), "progress");
        ApiClient api = new ApiClient(cookies, this);
        if (update) {
            api.update();
        } else {
            api.create();
        }
    }

    @Override
    public void onRequestSuccess() {
        if (progress != null) {
            progress.dismiss();
        }
        if (update) {
            SimpleAlertDialog.newInstance("有効期限を更新しました", null)
                    .show(getFragmentManager(), "request_success");
        }
        ((MainActivity) getActivity()).controller.initSdk();
        getFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public void onRequestFailed(String message) {
        if (progress != null) {
            progress.dismiss();
        }
        if (update) {
            Config.getInstance().removeAccessToken();
        }
        KillProcessDialog dialog = KillProcessDialog.newInstance("認証情報取得失敗", message);
        dialog.setTargetFragment(this, 1);
        dialog.show(getFragmentManager(), "request_failed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progress = null;
    }

}
