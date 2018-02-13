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

package jp.co.atware.trial_app.balloon;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import jp.co.atware.trial_app.MainActivity;

import static android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK;

/**
 * Web表示View
 */
public class HtmlView extends WebView {

    private static final String BLANK = "about:blank";

    private ProgressBar progress;

    /**
     * コンストラクタ
     *
     * @param context コンテキスト
     */
    public HtmlView(Context context) {
        this(context, null);
    }

    /**
     * コンストラクタ
     *
     * @param context コンテキスト
     * @param attrs   属性
     */
    public HtmlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * コンストラクタ
     *
     * @param context      コンテキスト
     * @param attrs        属性
     * @param defStyleAttr スタイル属性
     */
    public HtmlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /**
     * コンストラクタ
     *
     * @param context      コンテキスト
     * @param attrs        属性
     * @param defStyleAttr スタイル属性
     * @param defStyleRes  スタイルリソース
     */
    public HtmlView(final Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        WebSettings settings = getSettings();
        // キャッシュ設定
        settings.setCacheMode(LOAD_CACHE_ELSE_NETWORK);
        // 表示設定
        setInitialScale(1);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        // タップするとブラウザを起動
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && getUrl() != null) {
                    ((MainActivity) context).startBrowser(getUrl());
                }
                return true;
            }
        });
        // ロード完了後にWebViewを表示
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!BLANK.equals(url)) {
                    show();
                }
            }
        });
    }

    /**
     * プログレスバーを設定
     *
     * @param progress プログレスバー
     */
    void setProgress(ProgressBar progress) {
        this.progress = progress;
    }

    /**
     * プログレスバーを表示
     */
    void showProgress() {
        setVisibility(GONE);
        progress.setVisibility(VISIBLE);
    }

    /**
     * WebViewを表示
     */
    private void show() {
        progress.setVisibility(GONE);
        setVisibility(VISIBLE);
    }

    /**
     * URLをロード
     *
     * @param payload 吹き出し表示情報
     */
    void load(Payload payload) {
        if (payload.url.equals(getUrl())) {
            // 既にロード済み
            show();
        } else {
            showProgress();
            if (getUrl() != null) {
                // ロード済みのWebViewに違うURLをロードする場合
                stopLoading();
                loadUrl(BLANK);
            }
            loadUrl(payload.url);
        }
    }
}