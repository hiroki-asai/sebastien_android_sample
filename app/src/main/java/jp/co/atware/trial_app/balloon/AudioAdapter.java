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

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import jp.co.atware.trial_app.R;
import jp.co.atware.trial_app.balloon.Balloon.Action;
import jp.co.atware.trial_app.chat.ChatApplication;
import jp.co.atware.trial_app.chat.ChatController;

/**
 * AudioViewにMediaPlayerを適用
 */
public class AudioAdapter implements OnCompletionListener, OnPreparedListener {

    private static final int UPDATE_INTERVAL = 500;

    private static volatile AudioAdapter INSTANCE = null;

    /**
     * Singletonインスタンスを取得
     *
     * @return AudioAdapterインスタンス
     */
    public static AudioAdapter getInstance() {
        if (INSTANCE == null) {
            synchronized (AudioAdapter.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AudioAdapter();
                }
            }
        }
        return INSTANCE;
    }

    private final MediaPlayer player = new MediaPlayer();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean prepare = new AtomicBoolean();
    private final AtomicBoolean complete = new AtomicBoolean();

    /**
     * プログレスバー更新タスク
     */
    private final Runnable progressTask = new Runnable() {
        @Override
        public void run() {
            if (player.isPlaying()) {
                if (playView != null && playView.getTag().equals(tag)) {
                    playView.setProgress(player.getCurrentPosition());
                }
                startProgress();
            }
        }
    };

    private AudioView playView;
    private AudioView afterUtt;
    private Object tag;

    /**
     * コンストラクタ
     */
    private AudioAdapter() {
        player.setOnCompletionListener(this);
        player.setOnPreparedListener(this);
    }

    /**
     * 合成音声再生後に再生されるAudioViewを設定
     *
     * @param afterUtt 合成音声再生後に再生されるAudioView
     */
    void setAfterUtt(AudioView afterUtt) {
        this.afterUtt = afterUtt;
    }

    /**
     * AudioViewにMediaPlayerの再生状態を適用する
     *
     * @param view 表示されるAudioView
     */
    void adapt(AudioView view) {
        if (view.getTag().equals(tag)) {
            synchronized (this) {
                stopProgress();
                playView = view;
                view.enableSeekBar();
                view.setDuration(player.getDuration());
                int progress = player.getCurrentPosition();
                if (complete.get()) {
                    view.onComplete();
                } else if (0 < progress) {
                    view.setProgress(progress);
                }
                if (player.isPlaying()) {
                    view.onStart();
                    startProgress();
                } else {
                    view.onPause();
                }
            }
        } else {
            view.onComplete();
            view.disableSeekBar();
        }
    }

    /**
     * AudioViewの再生ボタンタップ時の処理
     *
     * @param view 再生ボタンがタップされたAudioView
     */
    synchronized void play(AudioView view) {
        if (prepare.get()) {
            return;
        }
        stopProgress();
        complete.set(false);

        if (!view.getTag().equals(tag)) {
            if (playView != null) {
                playView.onComplete();
                playView.disableSeekBar();
            }
            playView = view;
            tag = view.getTag();
            prepare(view.getUrl());
        } else if (player.isPlaying()) {
            player.pause();
            view.onPause();
            resumeVoiceChat();
        } else {
            playOnPrepared(view);
        }
    }

    /**
     * AudioViewが表示されていない状態でメディアを再生
     *
     * @param balloon 吹き出し
     */
    public synchronized void playWithoutView(Balloon balloon) {
        if (prepare.get()) {
            return;
        }
        stopProgress();
        complete.set(false);

        if (playView != null) {
            playView.onComplete();
            playView.disableSeekBar();
        }
        tag = balloon.position;
        prepare(balloon.payloads.get(0).url);
    }

    /**
     * 合成音声再生後にメディアを再生
     *
     * @param balloon 吹き出し
     */
    public void playAfterUtt(Balloon balloon) {
        balloon.action = Action.DO_NOTHING;
        if (afterUtt != null && afterUtt.getTag().equals(balloon.position)) {
            play(afterUtt);
        } else {
            playWithoutView(balloon);
        }
    }

    /**
     * メディア再生の準備
     *
     * @param url メディアURL
     */
    private void prepare(String url) {
        try {
            prepare.set(true);
            if (player.isPlaying()) {
                player.stop();
            }
            player.reset();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(url);
            player.prepareAsync();
        } catch (Exception e) {
            prepare.set(false);
            Log.d("AudioAdapter", "unexpected error occurred.", e);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (playView != null) {
            playView.setDuration(player.getDuration());
        }
        playOnPrepared(playView);
        prepare.set(false);
    }

    /**
     * 準備済みのMediaPlayerを再生
     *
     * @param view 再生されるAudioView
     */
    private void playOnPrepared(AudioView view) {
        ChatController chat = ChatController.getInstance();
        if (chat.isVoiceMode()) {
            ChatApplication app = ChatApplication.getInstance();
            app.cancelPlay();
            app.mute();
            chat.setSubtitle(R.string.playing_media);
        }
        player.start();
        if (view != null) {
            view.onStart();
            startProgress();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        resumeVoiceChat();
        complete.set(true);
        if (playView != null && playView.getTag().equals(tag)) {
            playView.onComplete();
        }
    }

    /**
     * プログレスバーの更新を開始
     */
    private void startProgress() {
        handler.postDelayed(progressTask, UPDATE_INTERVAL);
    }

    /**
     * プログレスバーの更新を終了
     */
    private void stopProgress() {
        handler.removeCallbacks(progressTask);
    }

    /**
     * 音声対話に復帰
     */
    private void resumeVoiceChat() {
        ChatController chat = ChatController.getInstance();
        if (chat.isVoiceMode()) {
            ChatApplication.getInstance().unmute();
            chat.setSubtitle(R.string.ready_to_talk);
        }
    }

    /**
     * AudioViewのプログレスバー変更時の処理
     *
     * @param view     プログレスバーが変更されたAudioView
     * @param progress 再生ポイント
     */
    void seekTo(AudioView view, int progress) {
        if (view.getTag().equals(tag)) {
            player.seekTo(progress);
        }
    }

    /**
     * AudioViewの終了ボタンタップ時の処理
     *
     * @param view 終了ボタンがタップされたAudioView
     */
    void stop(AudioView view) {
        if (view.getTag().equals(tag)) {
            complete.set(true);
            stopProgress();
            if (player.isPlaying()) {
                player.pause();
                resumeVoiceChat();
            }
            player.seekTo(0);
            view.onComplete();
        }
    }

    /**
     * 音声再生中の判定
     *
     * @return 音声再生中の場合にtrue
     */
    public boolean isPlaying() {
        return player.isPlaying();
    }

    /**
     * 一時停止
     */
    public void pause() {
        stopProgress();
        if (player.isPlaying()) {
            player.pause();
            if (playView != null) {
                playView.onPause();
            }
        }
    }

    /**
     * インスタンスの破棄
     */
    public void destroy() {
        INSTANCE = null;
        if (player.isPlaying()) {
            player.stop();
        }
        stopProgress();
        player.reset();
        player.release();
        playView = null;
        tag = null;
    }
}
