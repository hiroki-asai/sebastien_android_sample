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

package jp.co.atware.trial_app.chat;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicReference;

import jp.co.atware.trial_app.MainActivity;
import jp.co.atware.trial_app.R;
import jp.co.atware.trial_app.balloon.AudioAdapter;
import jp.co.atware.trial_app.balloon.Balloon;
import jp.co.atware.trial_app.fragment.Alert;
import jp.co.atware.trial_app.fragment.Progress;

/**
 * 対話アプリの操作
 */
public class ChatController implements View.OnClickListener {

    private static final long STOP_TIME = 20000;
    private static final long START_TIME = 2000;
    private static final long SHOW_STARTING_TIME = 2000;
    private static final long SHOW_WAITING_TIME = 2000;
    private static final int MAX_RETRY = 10;
    private static final int MAX_LENGTH = 200;

    /**
     * 対話モード
     */
    public enum ChatMode {
        VOICE, TEXT;
    }

    /**
     * 対話状態
     */
    public enum ChatStatus {
        STARTING, START, STOP;
    }

    private static volatile ChatController INSTANCE = null;

    /**
     * Singletonインスタンスを取得
     *
     * @return ChatControllerインスタンス
     */
    public static ChatController getInstance() {
        if (INSTANCE == null) {
            synchronized (ChatController.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ChatController();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 音声対話の自動終了
     */
    private final Runnable autoStop = new Runnable() {
        @Override
        public void run() {
            if (status.get() == ChatStatus.START) {
                stopVoice();
                String message = getActivity().getString(R.string.timeout);
                show(Alert.newInstance(null, message));
            }
        }
    };

    /**
     * 音声対話の自動開始
     */
    private final Runnable autoStart = new Runnable() {
        @Override
        public void run() {
            if (status.get() == ChatStatus.STOP) {
                ChatApplication.getInstance().start(ChatStartHandler.forVoiceMode());
            }
        }
    };

    /**
     * 接続中ダイアログを表示
     */
    private final Runnable showStarting = new Runnable() {
        @Override
        public void run() {
            if (status.get() != ChatStatus.START) {
                String message = getActivity().getString(R.string.starting);
                starting = Progress.newInstance(message, false);
                show(starting);
            }
        }
    };

    /**
     * 応答待ちダイアログを表示
     */
    private final Runnable showWaiting = new Runnable() {
        @Override
        public void run() {
            if (status.get() == ChatStatus.START) {
                String message = getActivity().getString(R.string.waiting);
                waiting = Progress.newInstance(message, true);
                show(waiting);
            }
        }
    };

    private final AtomicReference<ChatMode> mode = new AtomicReference<>();
    private final AtomicReference<ChatStatus> status = new AtomicReference<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Toolbar toolBar;
    private MenuItem mic;
    private MenuItem keyboard;
    private TextView message;

    private LinearLayout inputArea;
    private EditText editText;

    private Progress starting;
    private Progress waiting;
    private int retryCount;

    private ChatController() {

    }

    /**
     * 初期化
     *
     * @param activity 対話アプリのアクティビティ
     */
    void init(MainActivity activity) {
        // ToolBar関連
        toolBar = (Toolbar) activity.findViewById(R.id.tool_bar);
        toolBar.inflateMenu(R.menu.main);
        Menu menu = toolBar.getMenu();
        mic = menu.findItem(R.id.mic);
        keyboard = menu.findItem(R.id.keyboard);
        toolBar.setOnMenuItemClickListener(activity);
        message = (TextView) activity.findViewById(R.id.message);
        // EditText関連
        inputArea = (LinearLayout) activity.findViewById(R.id.input_area);
        editText = (EditText) activity.findViewById(R.id.edit_text);
        activity.findViewById(R.id.submit).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        // 送信ボタンタップ時の処理
        String text = editText.getText().toString();
        if (status.get() == ChatStatus.START || text.isEmpty()) {
            return;
        }
        if (MAX_LENGTH < text.length()) {
            Toast.makeText(getActivity(), R.string.length_over, Toast.LENGTH_SHORT).show();
        } else {
            ChatApplication app = ChatApplication.getInstance();
            app.show(new Balloon(Balloon.BalloonType.USER_VOICE, text));
            setSubtitle(R.string.starting);
            app.start(ChatStartHandler.forTextMode(text));
            hideKeyboard();
        }
    }

    /**
     * インスタンスの破棄
     */
    void destroy() {
        INSTANCE = null;
        clearAutoStop();
        clearAutoStart();
        clearStarting();
        clearWaiting();
        toolBar = null;
        mic = null;
        keyboard = null;
        message = null;
        inputArea = null;
        editText = null;
    }

    /**
     * 音声対話判定
     *
     * @return 音声対話中の場合にtrue
     */
    public boolean isVoiceMode() {
        return mode.get() == ChatMode.VOICE;
    }

    /**
     * テキストチャット判定
     *
     * @return テキストチャット中の場合にtrue
     */
    public boolean isTextMode() {
        return mode.get() == ChatMode.TEXT;
    }

    /**
     * 音声対話開始
     */
    public void startVoice() {
        AudioAdapter.getInstance().pause();
        stopText();
        mode.set(ChatMode.VOICE);
        mic.setIcon(R.drawable.ic_mic_white_24px);
        setSubtitle(R.string.starting);
        status.set(ChatStatus.STARTING);
        ChatApplication.getInstance().start(ChatStartHandler.forVoiceMode());
    }

    /**
     * 音声対話終了
     */
    public void stopVoice() {
        clearAutoStop();
        clearAutoStart();
        mode.set(null);
        mic.setIcon(R.drawable.ic_mic_off_white_24px);
        ChatApplication.getInstance().stop();
    }

    /**
     * テキストチャット開始
     */
    public void startText() {
        stopVoice();
        mode.set(ChatMode.TEXT);
        keyboard.setIcon(R.drawable.ic_keyboard_hide_white_24px);
        showMessage(R.string.send_please);
        inputArea.setVisibility(View.VISIBLE);
    }

    /**
     * テキストチャット終了
     */
    public void stopText() {
        mode.set(null);
        keyboard.setIcon(R.drawable.ic_keyboard_white_24px);
        showMessage(R.string.help);
        hideKeyboard();
        inputArea.setVisibility(View.GONE);
        ChatApplication.getInstance().stop();
    }

    /**
     * 対話状態を設定
     *
     * @param status 対話状態
     */
    void setStatus(ChatStatus status) {
        if (status == ChatStatus.STARTING) {
            setMenuEnabled(false);
            setStarting();
        } else {
            setMenuEnabled(true);
            clearStarting();
            if (status == ChatStatus.START) {
                clearAutoStart();
            } else {
                clearAutoStop();
                clearWaiting();
            }
            this.status.set(status);
        }
    }

    /**
     * メニューボタン操作の有効/無効化
     *
     * @param enable 有効化する場合にtrue
     */
    private void setMenuEnabled(boolean enable) {
        mic.setEnabled(enable);
        keyboard.setEnabled(enable);
    }

    /**
     * ツールバーのサブタイトルを設定
     *
     * @param resId stringリソースID
     */
    public void setSubtitle(@StringRes int resId) {
        toolBar.setSubtitle(resId);
        if (resId == R.string.stop) {
            toolBar.setSubtitleTextColor(ContextCompat.getColor(toolBar.getContext(), R.color.user_balloon));
        } else {
            toolBar.setSubtitleTextColor(Color.WHITE);
        }
        switch (resId) {
            case R.string.stop:
                if (isTextMode()) {
                    showMessage(R.string.send_please);
                } else {
                    showMessage(R.string.help);
                }
                break;
            case R.string.starting:
                showMessage(R.string.starting_now);
                break;
            case R.string.ready_to_talk:
                showMessage(R.string.talk_please);
                break;
            case R.string.playing_voice:
            case R.string.playing_media:
                showMessage(R.string.wait_please);
                break;
        }
    }

    /**
     * メッセージを表示
     *
     * @param resId stringリソースID
     */
    private void showMessage(@StringRes int resId) {
        message.setText(resId);
        if (resId == R.string.talk_please) {
            setToolBarColor(R.color.colorAccent);
        } else {
            setToolBarColor(R.color.colorPrimary);
            if (resId == R.string.help) {
                message.requestFocus(View.FOCUS_UP);
            }
        }
    }

    /**
     * ツールバーの背景色を設定
     *
     * @param resId colorリソースID
     */
    private void setToolBarColor(@ColorRes int resId) {
        toolBar.setBackgroundResource(resId);
        message.setBackgroundResource(resId);
    }

    /**
     * ダイアログを表示
     *
     * @param dialog DialogFragmentインスタンス
     */
    void show(DialogFragment dialog) {
        dialog.show(getActivity().getSupportFragmentManager(), null);
    }

    /**
     * 自動終了タイマをセット
     */
    void setAutoStop() {
        handler.postDelayed(autoStop, STOP_TIME);
    }

    /**
     * 自動終了タイマをクリア
     */
    void clearAutoStop() {
        handler.removeCallbacks(autoStop);
    }

    /**
     * 自動開始タイマをセット
     *
     * @return 自動開始タイマをセットし場合にtrue
     */
    boolean setAutoStart() {
        if (status.get() == ChatStatus.STARTING) {
            return false;
        } else {
            setStatus(ChatStatus.STOP);
        }
        if (retryCount++ < MAX_RETRY) {
            handler.postDelayed(autoStart, START_TIME);
            return true;
        }
        return false;
    }

    /**
     * 自動開始タイマをクリア
     */
    private void clearAutoStart() {
        retryCount = 0;
        handler.removeCallbacks(autoStart);
    }

    /**
     * 接続中ダイアログ表示タイマをセット
     */
    private void setStarting() {
        handler.postDelayed(showStarting, SHOW_STARTING_TIME);
    }

    /**
     * 接続中ダイアログ表示タイマをクリア
     */
    private void clearStarting() {
        handler.removeCallbacks(showStarting);
        if (starting != null) {
            starting.dismiss();
            starting = null;
        }
    }

    /**
     * 応答待ちダイアログ表示タイマをセット
     */
    void setWaiting() {
        handler.postDelayed(showWaiting, SHOW_WAITING_TIME);
    }

    /**
     * 応答待ちダイアログ表示タイマをクリア
     */
    void clearWaiting() {
        handler.removeCallbacks(showWaiting);
        if (waiting != null) {
            waiting.dismiss();
            waiting = null;
        }
    }

    /**
     * ソフトキーボードを隠す
     */
    private void hideKeyboard() {
        editText.setText("");
        View focus = getActivity().getCurrentFocus();
        if (focus != null) {
            InputMethodManager im = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(focus.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * MainActivityを取得
     *
     * @return MainActivity
     */
    private MainActivity getActivity() {
        return (MainActivity) editText.getContext();
    }

}
