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
import android.util.Log;

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
import static jp.co.atware.trial_app.util.URLConstants.REQ_DEVICE_TOKEN;
import static jp.co.atware.trial_app.util.URLConstants.UPDATE_DEVICE_TOKEN;

/**
 * APIからアクセストークンを取得
 */

public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String DEVICE_ID = "device_id";
    private static final String DEVICE_TOKEN = "device_token";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String STATUS = "status";
    private static final String VALID = "valid";
    private static final String NONE = "None";

    /**
     * コールバックメソッド
     */
    public interface ApiCallBack {

        /**
         * アクセストークン取得成功時の処理
         */
        void onRequestSuccess();

        /**
         * アクセストークン取得失敗時の処理
         *
         * @param message エラーメッセージ
         */
        void onRequestFailed(String message);
    }

    /**
     * API実行結果を受け取る処理
     */
    private interface Consumer {

        /**
         * API実行結果を受け取る処理
         *
         * @param result API実行結果
         */
        void accept(Map<String, String> result);
    }

    private final ApiCallBack callBack;
    private OkHttpClient client;

    /**
     * コンストラクタ
     *
     * @param callBack コールバックメソッド
     */
    public ApiClient(ApiCallBack callBack) {
        this.callBack = callBack;
    }

    /**
     * 新規のアクセストークンを取得
     *
     * @param cookies ログインCookie
     */
    public void request(final List<Cookie> cookies) {
        client = new OkHttpClient().newBuilder().cookieJar(new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {

            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                return cookies;
            }
        }).build();
        // APIからデバイスIDを取得
        String url = new URLBuilder(ISSUE_DEVICE_ID)
                .append(CLIENT_SECRET, Config.getInstance().getClientSecret()).toString();
        get(url, new Consumer() {
            @Override
            public void accept(Map<String, String> result) {
                String deviceId = result.get(DEVICE_ID);
                if (deviceId == null) {
                    throw new RuntimeException("device_id is not found in response.");
                }
                registerDevice(deviceId);
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
                .append(DEVICE_ID, deviceId).toString();
        get(url, new Consumer() {
            @Override
            public void accept(Map<String, String> result) {
                requestDeviceToken(deviceId);
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
                .append(DEVICE_ID, deviceId).toString();
        get(url, new Consumer() {
            @Override
            public void accept(Map<String, String> result) {
                saveTokens(result);
            }
        });
    }

    /**
     * アクセストークンを更新
     */
    public void update() {
        client = new OkHttpClient().newBuilder().build();
        String url = new URLBuilder(UPDATE_DEVICE_TOKEN)
                .append(REFRESH_TOKEN, Config.getInstance().getRefreshToken()).toString();
        get(url, new Consumer() {
            @Override
            public void accept(Map<String, String> result) {
                saveTokens(result);
            }
        });
    }

    /**
     * GETリクスエストを非同期で実行
     *
     * @param url      URL
     * @param consumer API実行結果を受け取る処理
     */
    private void get(String url, final Consumer consumer) {
        Call call = client.newCall(new Request.Builder().url(url).build());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.w(TAG, "unexpected error occurred.", e);
                failed(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, String> result = mapper.readValue(response.body().string(),
                                new TypeReference<HashMap<String, String>>() {
                                });
                        String status = result.get(STATUS);
                        if (VALID.equals(status)) {
                            consumer.accept(result);
                        } else {
                            failed(call, "status=" + status);
                        }
                    } catch (Exception e) {
                        failed(call, e);
                    }

                } else {
                    failed(call, "code=" + response.code());
                }
            }
        });
    }

    /**
     * アクセストークンとリフレッシュトークンを保存
     *
     * @param result API実行結果
     */
    private void saveTokens(Map<String, String> result) {
        String accessToken = getToken(result, DEVICE_TOKEN);
        String refreshToken = getToken(result, REFRESH_TOKEN);
        Config config = Config.getInstance();
        config.setAccessToken(accessToken);
        config.setRefreshToken(refreshToken);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                callBack.onRequestSuccess();
            }
        });
    }

    /**
     * トークンを取得
     *
     * @param result API実行結果
     * @param key    トークン取得用のkey文字列
     * @return トークン
     */
    private String getToken(Map<String, String> result, String key) {
        String token = result.get(key);
        if (token == null || token.equals(NONE)) {
            throw new RuntimeException(key + " is not found in response.");
        }
        return token;
    }

    /**
     * 失敗時の処理
     *
     * @param call API実行タスク
     * @param e    失敗時の例外
     */
    private void failed(Call call, Exception e) {
        failed(call, (e.getMessage() != null) ? e.getMessage() : e.getClass().getSimpleName());
    }

    /**
     * 失敗時の処理
     *
     * @param call   API実行タスク
     * @param reason 失敗の理由
     */
    private void failed(Call call, String reason) {
        List<String> path = call.request().url().pathSegments();
        final String message = String.format("%s is failed. %s", path.get(path.size() - 1), reason);
        Log.w(TAG, message);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                callBack.onRequestFailed(message);
            }
        });
    }
}
