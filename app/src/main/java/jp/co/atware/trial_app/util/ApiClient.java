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

package jp.co.atware.trial_app.util;

import android.os.Handler;
import android.os.Looper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static jp.co.atware.trial_app.util.URLConstants.DEVICE_REGISTRATION;
import static jp.co.atware.trial_app.util.URLConstants.ISSUE_DEVICE_ID;
import static jp.co.atware.trial_app.util.URLConstants.PARAM_CLIENT_SECRET;
import static jp.co.atware.trial_app.util.URLConstants.PARAM_DEVICE_ID;
import static jp.co.atware.trial_app.util.URLConstants.PARAM_REFRESH_TOKEN;
import static jp.co.atware.trial_app.util.URLConstants.REQ_DEVICE_TOKEN;
import static jp.co.atware.trial_app.util.URLConstants.UPDATE_DEVICE_TOKEN;
import static jp.co.atware.trial_app.util.URLConstants.VALUE_CLIENT_SECRET;

/**
 * APIからアクセストークンを取得
 */

public class ApiClient implements CookieJar {

    /**
     * コールバックメソッド
     */
    public interface ApiCallBack {

        /**
         * アクセストークン取得成功時の処理
         *
         * @param accessToken アクセストークン
         */
        void onRequestSuccess(String accessToken);

        /**
         * アクセストークン取得失敗時の処理
         *
         * @param message エラーメッセージ
         */
        void onRequestFailed(String message);
    }

    private final List<Cookie> cookies;
    private final ApiCallBack callBack;
    private final OkHttpClient client = new OkHttpClient().newBuilder().cookieJar(this).build();
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * コンストラクタ
     *
     * @param cookies  ユーザダッシュボードログイン時のCookie
     * @param callBack コールバックメソッド
     */
    public ApiClient(List<Cookie> cookies, ApiCallBack callBack) {
        this.cookies = cookies;
        this.callBack = callBack;
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        // do nothing
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        return cookies;
    }

    /**
     * アクセストークンを更新
     */
    public void update() {
        Config config = Config.getInstance();
        String url = new URLBuilder(UPDATE_DEVICE_TOKEN)
                .append(PARAM_REFRESH_TOKEN, config.getRefreshToken()).toString();
        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                failed("update_device_token failed." + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (saveTokens(response)) {
                    // アクセストークンの更新を通知
                    success();
                } else {
                    failed("update_device_token failed.");
                }
            }
        });
    }

    /**
     * 新規のアクセストークンを取得
     */
    public void create() {
        // APIからデバイスIDを取得
        String url = new URLBuilder(ISSUE_DEVICE_ID)
                .append(PARAM_CLIENT_SECRET, VALUE_CLIENT_SECRET).toString();
        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                failed("issue_device_id failed." + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 取得したデバイスIDを登録
                String deviceId = toMap(response).get("device_id");
                if (deviceId != null) {
                    registerDevice(deviceId);
                } else {
                    failed("issue_device_id failed.");
                }
            }
        });
    }

    /**
     * デバイスIDを登録
     *
     * @param deviceId デバイスID
     */
    private void registerDevice(final String deviceId) {
        String url = new URLBuilder(DEVICE_REGISTRATION)
                .append(PARAM_DEVICE_ID, deviceId).toString();
        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                failed("device_registration failed." + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 登録したデバイスIDでアクセストークンを取得
                if ("valid".equals(toMap(response).get("status"))) {
                    requestDeviceToken(deviceId);
                } else {
                    failed("device_registration failed.");
                }
            }
        });
    }

    /**
     * アクセストークンを取得
     *
     * @param deviceId デバイスID
     */
    private void requestDeviceToken(String deviceId) {
        String url = new URLBuilder(REQ_DEVICE_TOKEN)
                .append(PARAM_DEVICE_ID, deviceId).toString();
        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                failed("req_device_token failed." + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (saveTokens(response)) {
                    // アクセストークンの取得を通知
                    success();
                } else {
                    failed("req_device_token failed.");
                }
            }
        });
    }

    /**
     * アクセストークンとリフレッシュトークンを保存
     *
     * @param response レスポンス
     * @return 保存が完了した場合にtrue
     * @throws IOException 保存失敗
     */
    private boolean saveTokens(Response response) throws IOException {
        Map<String, String> values = toMap(response);
        String accessToken = values.get("device_token");
        String refreshToken = values.get("refresh_token");
        if (accessToken != null && refreshToken != null) {
            Config config = Config.getInstance();
            config.setAccessToken(accessToken);
            config.setRefreshToken(refreshToken);
            return true;
        }
        return false;
    }

    /**
     * GETリクスエストを非同期で実行
     *
     * @param url      URL
     * @param callback 実行後のコールバック
     */
    private void get(String url, Callback callback) {
        Request req = new Request.Builder().url(url).build();
        Call call = client.newCall(req);
        call.enqueue(callback);
    }

    /**
     * JSON形式のレスポンスをMapに変換
     *
     * @param response レスポンス
     * @return 変換されたMap
     * @throws IOException 変換失敗
     */
    private Map<String, String> toMap(Response response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response.body().string(),
                new TypeReference<HashMap<String, String>>() {
                });
    }

    /**
     * 成功時の処理
     */
    private void success() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                callBack.onRequestSuccess(Config.getInstance().getAccessToken());
            }
        });
    }

    /**
     * 失敗時の処理
     *
     * @param message エラーメッセージ
     */
    private void failed(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                callBack.onRequestFailed(message);
            }
        });
    }
}
