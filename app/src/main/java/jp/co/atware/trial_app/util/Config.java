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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * 設定情報
 */
public class Config extends Application {

    private static Config INSTANCE;

    public static Config getInstance() {
        return INSTANCE;
    }

    public static Config getInstance(Context context) {
        return INSTANCE;
    }

    private SharedPreferences config;

    public void onCreate() {
        super.onCreate();
        config = getApplicationContext().getSharedPreferences("config", Context.MODE_PRIVATE);
        INSTANCE = this;
    }

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

    public boolean isSSL() {
        return Boolean.valueOf(get(Keys.SSL));
    }

    public boolean setSSL(Boolean ssl) {
        return set(Keys.SSL, ssl.toString());
    }

    public boolean isOCSP() {
        return Boolean.valueOf(get(Keys.OCSP));
    }

    public boolean setOCSP(Boolean ocsp) {
        return set(Keys.OCSP, ocsp.toString());
    }

    public String getHost() {
        return get(Keys.HOST);
    }

    public boolean setHost(String host) {
        return set(Keys.HOST, host);
    }

    public Integer getPort() {
        return Integer.valueOf(get(Keys.PORT));
    }

    public boolean setPort(String port) {
        return set(Keys.PORT, port);
    }

    public String getPath() {
        return get(Keys.PATH);
    }

    public boolean setPath(String path) {
        return set(Keys.PATH, path);
    }

    public String getAccessToken() {
        return get(Keys.ACCESS_TOKEN);
    }

    public boolean setAccessToken(String accessToken) {
        return set(Keys.ACCESS_TOKEN, accessToken);
    }

    public String getRefreshToken() {
        return get(Keys.REFRESH_TOKEN);
    }

    public boolean setRefreshToken(String refreshToken) {
        return set(Keys.REFRESH_TOKEN, refreshToken);
    }

    public void removeAccessToken() {
        Editor editor = config.edit();
        editor.remove(Keys.ACCESS_TOKEN.name());
        editor.apply();
    }

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

    private boolean set(Keys key, String value) {
        if (isEmpty(value) || !isChanged(key, value)) {
            return false;
        }
        Editor editor = config.edit();
        editor.putString(key.name(), value.trim());
        editor.apply();
        return true;
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isChanged(Keys key, String newValue) {
        String oldValue = get(key);
        return isEmpty(oldValue) || !oldValue.equals(newValue.trim());
    }

}
