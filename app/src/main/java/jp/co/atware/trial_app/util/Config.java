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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import jp.co.atware.trial_app.chat.ChatApplication;

/**
 * 設定情報
 */
public class Config {

    private static volatile Config INSTANCE = null;

    /**
     * Singletonインスタンスを取得
     *
     * @return Configインスタンス
     */
    public static Config getInstance() {
        if (INSTANCE == null) {
            synchronized (Config.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Config();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * SharedPreferencesのキー値と初期値を定義
     */
    public enum Keys {
        SSL("true"),
        OCSP("true"),
        HOST("spf.sebastien.ai"),
        PORT("443"),
        PATH("/talk"),
        ACCESS_TOKEN(null),
        REFRESH_TOKEN(null);

        public String defaultValue;

        Keys(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    private SharedPreferences config;

    /**
     * コンストラクタ
     */
    private Config() {
        this.config = ChatApplication.getInstance().getApplicationContext()
                .getSharedPreferences("config", Context.MODE_PRIVATE);
    }

    /**
     * SSL使用の可否を取得
     *
     * @return SSLを使用する場合にtrue
     */
    public boolean isSSL() {
        return Boolean.valueOf(get(Keys.SSL));
    }

    /**
     * SSL使用の可否を設定
     *
     * @param ssl SSLを使用する場合にtrue
     * @return SSL使用の可否が変更された場合にtrue
     */
    public boolean setSSL(Boolean ssl) {
        return set(Keys.SSL, ssl.toString());
    }

    /**
     * OCSP使用の可否を取得
     *
     * @return OCSPを使用する場合にtrue
     */
    public boolean isOCSP() {
        return Boolean.valueOf(get(Keys.OCSP));
    }

    /**
     * OCSP使用の可否を設定
     *
     * @param ocsp OCSPを使用する場合にtrue
     * @return OCSP使用の可否が変更された場合にtrue
     */
    public boolean setOCSP(Boolean ocsp) {
        return set(Keys.OCSP, ocsp.toString());
    }

    /**
     * ホスト名を取得
     *
     * @return ホスト名
     */
    public String getHost() {
        return get(Keys.HOST);
    }

    /**
     * ホスト名を設定
     *
     * @param host ホスト名
     * @return ホスト名が変更された場合にtrue
     */
    public boolean setHost(String host) {
        return set(Keys.HOST, host);
    }

    /**
     * ポート番号を取得
     *
     * @return ポート番号
     */
    public Integer getPort() {
        return Integer.valueOf(get(Keys.PORT));
    }

    /**
     * ポート番号を設定
     *
     * @param port ポート番号
     * @return ポート番号が変更された場合にtrue
     */
    public boolean setPort(String port) {
        return set(Keys.PORT, port);
    }

    /**
     * URLパスを取得
     *
     * @return URLパス
     */
    public String getPath() {
        return get(Keys.PATH);
    }

    /**
     * URLパスを設定
     *
     * @param path URLパス
     * @return URLパスが変更された場合にtrue
     */
    public boolean setPath(String path) {
        return set(Keys.PATH, path);
    }

    /**
     * アクセストークンを取得
     *
     * @return アクセストークン
     */
    public String getAccessToken() {
        return get(Keys.ACCESS_TOKEN);
    }

    /**
     * アクセストークンを設定
     *
     * @param accessToken アクセストークン
     * @return アクセストークンが変更された場合にtrue
     */
    public boolean setAccessToken(String accessToken) {
        return set(Keys.ACCESS_TOKEN, accessToken);
    }

    /**
     * リフレッシュトークンを取得
     *
     * @return リフレッシュトークン
     */
    public String getRefreshToken() {
        return get(Keys.REFRESH_TOKEN);
    }

    /**
     * リフレッシュトークンを設定
     *
     * @param refreshToken リフレッシュトークン
     * @return リフレッシュトークンが変更された場合にtrue
     */
    public boolean setRefreshToken(String refreshToken) {
        return set(Keys.REFRESH_TOKEN, refreshToken);
    }

    /**
     * アクセストークンを削除
     */
    public void removeAccessToken() {
        Editor editor = config.edit();
        editor.remove(Keys.ACCESS_TOKEN.name());
        editor.apply();
    }

    /**
     * SharedPreferencesから値を取得
     *
     * @param key キー値
     * @return 値
     */
    private String get(Keys key) {
        String value = config.getString(key.name(), null);
        if (isEmpty(value)) {
            if (key.defaultValue != null) {
                Editor editor = config.edit();
                editor.putString(key.name(), key.defaultValue);
                editor.apply();
            }
            return key.defaultValue;
        }
        return value;
    }

    /**
     * SharedPreferencesに値を保存
     *
     * @param key   キー値
     * @param value 値
     * @return 値が変更された場合にtrue
     */
    private boolean set(Keys key, String value) {
        if (isEmpty(value) || !isChanged(key, value)) {
            return false;
        }
        Editor editor = config.edit();
        editor.putString(key.name(), value.trim());
        editor.apply();
        return true;
    }

    /**
     * 空文字列判定
     *
     * @param s 文字列
     * @return 文字列が空の場合にtrue
     */
    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 保存された値が変更されたかを判定
     *
     * @param key      キー値
     * @param newValue 新しい値
     * @return 保存された値が変更された場合にtrue
     */
    private boolean isChanged(Keys key, String newValue) {
        String oldValue = get(key);
        return isEmpty(oldValue) || !oldValue.equals(newValue.trim());
    }

}
