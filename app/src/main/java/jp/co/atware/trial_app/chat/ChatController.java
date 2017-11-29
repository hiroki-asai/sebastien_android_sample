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
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.nttdocomo.flow.ErrorEventHandler;
import com.nttdocomo.flow.EventHandler;
import com.nttdocomo.flow.StringEventHandler;
import com.nttdocomo.sebastien.Sebastien;
import com.nttdocomo.sebastien.util.NluMetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jp.co.atware.trial_app.MainActivity;
import jp.co.atware.trial_app.R;
import jp.co.atware.trial_app.chat.Chat.Person;
import jp.co.atware.trial_app.chat.ChatStartHandler.ChatMode;
import jp.co.atware.trial_app.chat.DeviceInfo.PlayTTS;
import jp.co.atware.trial_app.chat.TextStartHandler.TextChatCallBack;
import jp.co.atware.trial_app.chat.VoiceStartHandler.VoiceChatCallBack;
import jp.co.atware.trial_app.fragment.Progress;
import jp.co.atware.trial_app.fragment.SimpleAlertDialog;
import jp.co.atware.trial_app.fragment.UpdateAccessToken;
import jp.co.atware.trial_app.fragment.UserDashboard;
import jp.co.atware.trial_app.util.Config;

/**
 * 対話コントローラ
 */
public class ChatController implements TextChatCallBack, VoiceChatCallBack {

    private static final int TOKEN_EXPIRED = 40102;

    private static final long STOP_TIME = 20000;
    private static final long SHOW_CONNECTING_TIME = 2000;
    private static final long SHOW_WAITING_TIME = 2000;

    private final Sebastien sdk = new Sebastien();
    private final AtomicBoolean init = new AtomicBoolean();
    private final AtomicBoolean start = new AtomicBoolean();
    private final AtomicReference<ChatMode> chatMode = new AtomicReference<>();

    private final MainActivity activity;
    private final ActionBar actionBar;
    private final View mask;
    private final List<Chat> chatList = new ArrayList<>();
    private final ChatAdapter chatAdapter;
    private final ListView chatArea;
    private final LinearLayout inputArea;
    private final EditText editText;
    private final InputMethodManager inputMethodManager;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable stopTask = new Runnable() {
        @Override
        public void run() {
            hideWaiting();
            stopVoice();
            Toast.makeText(activity, "無言状態20秒でスリープ", Toast.LENGTH_SHORT).show();
        }
    };
    private final Runnable showConnectingTask = new Runnable() {
        @Override
        public void run() {
            if (!start.get()) {
                connecting = Progress.newInstance("サーバ接続中", false);
                connecting.show(activity.getSupportFragmentManager(), "connecting");
            }
        }
    };
    private final Runnable showWaitingTask = new Runnable() {
        @Override
        public void run() {
            if (start.get()) {
                waiting = Progress.newInstance("サーバ応答待ち", true);
                waiting.show(activity.getSupportFragmentManager(), "waiting");
            }
        }
    };

    private Progress connecting;
    private Progress waiting;
    private MenuItem voiceMenu;
    private MenuItem textMenu;

    public ChatController(MainActivity activity) {
        this.activity = activity;
        actionBar = activity.getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_sentiment_neutral_white_24px);

        mask = activity.findViewById(R.id.mask);
        mask.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        chatAdapter = new ChatAdapter(activity, R.layout.activity_main, chatList);
        chatArea = (ListView) activity.findViewById(R.id.chat_area);
        chatArea.setAdapter(chatAdapter);
        inputArea = (LinearLayout) activity.findViewById(R.id.input_area);
        editText = (EditText) activity.findViewById(R.id.edit_text);
        inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        // 送信ボタンクリック時の処理
        Button submit = (Button) activity.findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String text = editText.getText().toString();
                if (!start.get() && !text.isEmpty()) {
                    show(new Chat(Person.USER, text));
                    setMenuEnabled(false);
                    startSdk(new TextStartHandler(text, ChatController.this));
                    editText.setText("");
                    hideKeyboard();
                }
            }
        });
    }

    /**
     * SDKの初期化
     */
    public void initSdk() {
        Config config = Config.getInstance(activity);
        String accessToken = config.getAccessToken();
        if (accessToken != null) {
            // SDKのセットアップ
            sdk.set("UseSSL", config.isSSL());
            sdk.set("EnableOCSP", config.isOCSP());
            sdk.set("OutputGain", 1.00);
            sdk.setHost(config.getHost());
            sdk.setPort(config.getPort());
            sdk.setURLPath(config.getPath());
            sdk.setAccessToken(accessToken);
            sdk.setContext(activity.getApplicationContext());
            // メタデータ受信時の処理
            sdk.setOnMetaOut(new StringEventHandler() {
                @Override
                public void run(String metaData) {
                    Chat chat = ChatParser.parse(metaData);
                    if (chat != null) {
                        onChatReceived(chat);
                    }
                }
            });
            sdk.init();
            init.set(true);
        } else {
            // ユーザダッシュボードのログイン画面を表示
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.base_layout, new UserDashboard()).commitAllowingStateLoss();
        }
    }

    /**
     * 発話情報受信時の処理
     *
     * @param chat 発話情報
     */
    private void onChatReceived(Chat chat) {
        switch (chat.person) {
            case USER:
                if (chatMode.get() == ChatMode.VOICE) {
                    timerOff();
                    showWaiting();
                }
                break;
            case NLU:
                hideWaiting();
                if (chatMode.get() == ChatMode.VOICE) {
                    timerOn();
                }
                break;
        }
        show(chat);
        if (chat.replyMessage != null) {
            if (chatMode.get() == ChatMode.VOICE) {
                NluMetaData meta = new NluMetaData();
                meta.voiceText = chat.replyMessage;
                meta.clientData = new HashMap<>();
                meta.clientData.put(DeviceInfo.KEY, new DeviceInfo(Build.MODEL, PlayTTS.ON));
                sdk.putMeta(meta);
            } else {
                sdk.putText(chat.replyMessage);
            }
        } else if (chatMode.get() == ChatMode.TEXT) {
            stopSdk();
        }
    }

    /**
     * SDKの開始
     *
     * @param onStart 開始ハンドラ
     */
    private void startSdk(final ChatStartHandler onStart) {
        showConnecting();
        sdk.setMicMute(onStart.getMode() == ChatMode.TEXT);
        sdk.start(onStart, new ErrorEventHandler() {
            @Override
            public void run(int errCode, String message) {
                start.set(false);
                hideConnecting();
                actionBar.setIcon(R.drawable.ic_sentiment_very_dissatisfied_white_24px);
                stop(onStart.getMode());
                setMenuEnabled(true);
                if (errCode == TOKEN_EXPIRED) {
                    new UpdateAccessToken()
                            .show(activity.getSupportFragmentManager(), "update_access_token");
                } else {
                    SimpleAlertDialog.newInstance("接続エラー", message)
                            .show(activity.getSupportFragmentManager(), "connection_failed");
                }
            }
        });
    }

    /**
     * SDKの停止
     */
    private void stopSdk() {
        if (start.get()) {
            sdk.stop(new EventHandler() {
                @Override
                public void run() {
                    start.set(false);
                    actionBar.setIcon(R.drawable.ic_sentiment_neutral_white_24px);
                }
            });
        }
    }

    /**
     * 発話表示
     *
     * @param chat 発話情報
     */
    private synchronized void show(Chat chat) {
        if (chat.switchAgent != null) {
            switch (chat.switchAgent) {
                case PERSONAL:
                    chatArea.setBackgroundColor(Color.WHITE);
                    break;
                case EXPERT:
                    chatArea.setBackgroundResource(R.color.expertAgent);
                    break;
            }
        }
        if (chat.text != null) {
            chatList.add(chat);
            chatAdapter.notifyDataSetChanged();
            chatArea.setSelection(chatList.size());
        }
    }

    /**
     * チャットメニューを設定
     *
     * @param menu オプションメニュー
     */
    public void setMenu(Menu menu) {
        voiceMenu = menu.findItem(R.id.voice_mode);
        textMenu = menu.findItem(R.id.text_mode);
    }

    /**
     * チャットメニューの有効/無効化
     *
     * @param enable 有効化する場合にtrue
     */
    private void setMenuEnabled(boolean enable) {
        voiceMenu.setEnabled(enable);
        textMenu.setEnabled(enable);
    }

    /**
     * 停止タイマON
     */
    private void timerOn() {
        timerOff();
        handler.postDelayed(stopTask, STOP_TIME);
    }

    /**
     * 停止タイマOFF
     */
    private void timerOff() {
        handler.removeCallbacks(stopTask);
    }

    /**
     * サーバ接続中ダイアログを表示
     */
    private void showConnecting() {
        handler.postDelayed(showConnectingTask, SHOW_CONNECTING_TIME);
    }

    /**
     * サーバ接続中ダイアログを隠す
     */
    private void hideConnecting() {
        handler.removeCallbacks(showConnectingTask);
        if (connecting != null) {
            connecting.dismiss();
            connecting = null;
        }
    }

    /**
     * サーバ応答待ちダイアログを表示
     */
    private void showWaiting() {
        handler.postDelayed(showWaitingTask, SHOW_WAITING_TIME);
    }

    /**
     * サーバ応答待ちダイアログを隠す
     */
    private void hideWaiting() {
        handler.removeCallbacks(showWaitingTask);
        if (waiting != null) {
            waiting.dismiss();
            waiting = null;
        }
    }

    /**
     * ソフトキーボードを隠す
     */
    private void hideKeyboard() {
        View focus = activity.getCurrentFocus();
        if (focus != null) {
            inputMethodManager.hideSoftInputFromWindow(focus.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * 音声対話のON/OFF
     */
    public void toggleVoice() {
        if (chatMode.get() == ChatMode.VOICE) {
            stopVoice();
        } else {
            startVoice();
        }
    }

    /**
     * テキストチャットのON/OFF
     */
    public void toggleText() {
        if (chatMode.get() == ChatMode.TEXT) {
            stopText();
        } else {
            startText();
        }
    }

    /**
     * 音声対話の開始
     */
    private void startVoice() {
        if (init.get()) {
            setMenuEnabled(false);
            stopText();
            chatMode.set(ChatMode.VOICE);
            voiceMenu.setIcon(R.drawable.ic_mic_white_24px);
            startSdk(new VoiceStartHandler(this));
        }
    }

    /**
     * テキストチャットの開始
     */
    private void startText() {
        if (init.get()) {
            stopVoice();
            chatMode.set(ChatMode.TEXT);
            textMenu.setIcon(R.drawable.ic_keyboard_hide_white_24px);
            mask.setVisibility(View.GONE);
            inputArea.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onVoiceStart() {
        start.set(true);
        hideConnecting();
        actionBar.setIcon(R.drawable.ic_mood_white_24px);
        mask.setVisibility(View.GONE);
        timerOn();
        setMenuEnabled(true);
    }

    @Override
    public void onTextStart(String voiceText) {
        start.set(true);
        hideConnecting();
        actionBar.setIcon(R.drawable.ic_mood_white_24px);
        sdk.putText(voiceText);
        setMenuEnabled(true);
        showWaiting();
    }

    /**
     * 対話停止
     */
    public void stop() {
        stop(chatMode.get());
    }

    /**
     * 対話停止
     *
     * @param mode 対話モード
     */
    private void stop(ChatMode mode) {
        if (mode == ChatMode.VOICE) {
            stopVoice();
        } else if (mode == ChatMode.TEXT) {
            stopText();
        }
    }

    /**
     * 音声対話の停止
     */
    private void stopVoice() {
        timerOff();
        chatMode.set(null);
        voiceMenu.setIcon(R.drawable.ic_mic_off_white_24px);
        mask.setVisibility(View.VISIBLE);
        stopSdk();
    }

    /**
     * テキストチャットの停止
     */
    private void stopText() {
        chatMode.set(null);
        textMenu.setIcon(R.drawable.ic_keyboard_white_24px);
        editText.setText("");
        hideKeyboard();
        inputArea.setVisibility(View.GONE);
        mask.setVisibility(View.VISIBLE);
        stopSdk();
    }

    /**
     * プロセスの終了
     */
    public void quit() {
        if (start.get()) {
            sdk.stop(new EventHandler() {
                @Override
                public void run() {
                    Process.killProcess(Process.myPid());
                }
            });
        } else {
            Process.killProcess(Process.myPid());
        }
    }
}
