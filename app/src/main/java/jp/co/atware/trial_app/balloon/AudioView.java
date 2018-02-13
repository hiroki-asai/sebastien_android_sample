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
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import jp.co.atware.trial_app.R;
import jp.co.atware.trial_app.chat.ChatApplication;

/**
 * 音声再生View
 */
public class AudioView extends LinearLayout implements OnSeekBarChangeListener {

    private final TextView title;
    private final SeekBar seekBar;
    private final ImageButton play;
    private final ImageButton stop;

    private String url;

    /**
     * コンストラクタ
     *
     * @param context コンテキスト
     */
    public AudioView(Context context) {
        this(context, null);
    }

    /**
     * コンストラクタ
     *
     * @param context コンテキスト
     * @param attrs   属性
     */
    public AudioView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * コンストラクタ
     *
     * @param context      コンテキスト
     * @param attrs        属性
     * @param defStyleAttr スタイル属性
     */
    public AudioView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
    public AudioView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        View root = LayoutInflater.from(context).inflate(R.layout.audio, this);
        title = (TextView) root.findViewById(R.id.media_title);
        seekBar = (SeekBar) root.findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(this);
        play = (ImageButton) root.findViewById(R.id.play);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatApplication.getInstance().clearPlayAfterUtt();
                AudioAdapter.getInstance().play(AudioView.this);
            }
        });
        stop = (ImageButton) root.findViewById(R.id.stop);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioAdapter.getInstance().stop(AudioView.this);
            }
        });
    }

    /**
     * URLをセット
     *
     * @param url URL
     */
    void setUrl(String url) {
        this.url = url;
        this.title.setText(Uri.parse(url).getLastPathSegment());
    }

    /**
     * URLを取得
     *
     * @return URL
     */
    String getUrl() {
        return url;
    }

    /**
     * 再生時間をセット
     *
     * @param duration 再生時間
     */
    void setDuration(int duration) {
        seekBar.setMax(duration);
    }

    /**
     * 再生ポイントをセット
     *
     * @param progress 再生ポイント
     */
    void setProgress(int progress) {
        seekBar.setProgress(progress);
    }

    /**
     * SeekBarを有効化
     */
    void enableSeekBar() {
        seekBar.setEnabled(true);
    }

    /**
     * SeekBarを無効化
     */
    void disableSeekBar() {
        seekBar.setEnabled(false);
    }

    /**
     * 再生開始時の処理
     */
    void onStart() {
        play.setImageResource(R.drawable.ic_pause_black_48px);
        enableSeekBar();
    }

    /**
     * 一時停止時の処理
     */
    void onPause() {
        play.setImageResource(R.drawable.ic_play_arrow_black_48px);
    }

    /**
     * 再生完了時の処理
     */
    void onComplete() {
        seekBar.setProgress(0);
        play.setImageResource(R.drawable.ic_play_arrow_black_48px);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            AudioAdapter.getInstance().seekTo(this, progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

}