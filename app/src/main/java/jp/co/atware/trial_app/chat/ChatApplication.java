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

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ViewConfiguration;
import android.webkit.WebView;
import android.widget.ListView;

import com.nttdocomo.flow.EventHandler;
import com.nttdocomo.flow.StringEventHandler;
import com.nttdocomo.speak.Speak;
import com.nttdocomo.speak.Speak.OnConnectedWithHFP;
import com.nttdocomo.speak.util.NluMetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import jp.co.atware.trial_app.MainActivity;
import jp.co.atware.trial_app.R;
import jp.co.atware.trial_app.balloon.AudioAdapter;
import jp.co.atware.trial_app.balloon.Balloon;
import jp.co.atware.trial_app.balloon.BalloonAdapter;
import jp.co.atware.trial_app.chat.ChatController.ChatMode;
import jp.co.atware.trial_app.chat.ChatController.ChatStatus;
import jp.co.atware.trial_app.fragment.UserDashboard;
import jp.co.atware.trial_app.metadata.DeviceInfo;
import jp.co.atware.trial_app.metadata.DeviceInfo.PlayTTS;
import jp.co.atware.trial_app.metadata.MetaData;
import jp.co.atware.trial_app.metadata.MetaDataParser;
import jp.co.atware.trial_app.metadata.Postback;
import jp.co.atware.trial_app.metadata.SwitchAgent.AgentType;
import jp.co.atware.trial_app.util.Config;

/**
 * 対話アプリ
 */
public class ChatApplication extends Application {

    private static final float SCROLL_WEIGHT = 2f;
    private static final DeviceInfo TTS_ON = new DeviceInfo(Build.MODEL, PlayTTS.ON);
    private static final DeviceInfo TTS_OFF = new DeviceInfo(Build.MODEL, PlayTTS.OFF);

    private static ChatApplication INSTANCE;

    /**
     * Singletonインスタンスを取得
     *
     * @return ChatApplicationインスタンス
     */
    public static ChatApplication getInstance() {
        return INSTANCE;
    }

    private final Speak sdk = new Speak();
    private final List<Balloon> balloonList = new ArrayList<>();
    private final BalloonAdapter balloonAdapter = new BalloonAdapter(balloonList);
    private final AudioAdapter audioAdapter = AudioAdapter.getInstance();
    private final ChatController chat = ChatController.getInstance();
    private final MetaDataParser parser = new MetaDataParser();
    private final AtomicReference<AgentType> switchAfterUtt = new AtomicReference<>();
    private final AtomicReference<Postback> postBackAfterUtt = new AtomicReference<>();
    private final Queue<Balloon> playAfterUtt = new LinkedList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ListView chatView;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
    }

    /**
     * 初期化
     *
     * @param activity 対話アプリのMainActivity
     */
    public void init(final MainActivity activity) {
        // WebViewキャッシュの削除
        new WebView(activity).clearCache(true);
        chatView = (ListView) activity.findViewById(R.id.chat_area);
        chatView.setOnScrollListener(balloonAdapter);
        chatView.setAdapter(balloonAdapter);
        chatView.setFriction(ViewConfiguration.getScrollFriction() * SCROLL_WEIGHT);
        chat.init(activity);
        sdk.set("EnableOCSP", true);
        sdk.set("OutputGain", 1.00);
        // メタデータ受信時の処理
        sdk.setOnMetaOut(new StringEventHandler() {
            @Override
            public void run(String metaData) {
                Log.d("OnMetaOut", metaData);
                final MetaData meta = parser.parse(metaData);
                if (meta != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onMetaOut(meta);
                        }
                    });
                }
            }
        });
        // 合成音声再生開始時の処理
        sdk.setOnPlayStart(new StringEventHandler() {
            @Override
            public void run(String s) {
                if (chat.isVoiceMode()) {
                    chat.clearAutoStop();
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        audioAdapter.pause();
                        chat.setSubtitle(R.string.playing_voice);
                    }
                });
            }
        });
        // 合成音声再生終了時の処理
        sdk.setOnPlayEnd(new StringEventHandler() {
            @Override
            public void run(String s) {
                if (chat.isVoiceMode()) {
                    chat.setAutoStop();
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // 合成音声再生後のエージェント切り替え
                        AgentType agentType = switchAfterUtt.get();
                        if (agentType != null) {
                            switchAfterUtt.set(null);
                            onSwitchAgent(agentType);
                        }
                        // 合成音声再生後のpostback送信
                        Postback postback = postBackAfterUtt.get();
                        if (postback != null) {
                            postBackAfterUtt.set(null);
                            putMeta(postback.payload, postback.clientData);
                        }
                        // 合成音声再生後のメディア再生
                        Balloon balloon = playAfterUtt.poll();
                        if (balloon != null && playAfterUtt.isEmpty()) {
                            audioAdapter.playAfterUtt(balloon);
                        } else if (!audioAdapter.isPlaying()) {
                            chat.setSubtitle(R.string.ready_to_talk);
                        }
                    }
                });

            }
        });
        sdk.enableBluetoothSupport();
        sdk.setContext(getApplicationContext());
        if (Config.getInstance().getAccessToken() == null) {
            // ユーザダッシュボードのログイン画面を表示
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.base_layout, new UserDashboard()).commitAllowingStateLoss();
        } else if (activity.isStartVoiceCommand()) {
            startOnEnableHFP(true);
        }
    }


    /**
     * HFPが有効化された時に音声対話を開始
     *
     * @param init 音声対話開始時に#INITを送信する場合にtrue
     */
    public void startOnEnableHFP(final boolean init) {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta != null && bta.getProfileConnectionState(BluetoothProfile.HEADSET)
                == BluetoothAdapter.STATE_CONNECTED) {
            sdk.setOnConnectedWithHFP(new OnConnectedWithHFP() {
                @Override
                public void onConnected() {
                    sdk.setOnConnectedWithHFP(null);
                    chat.startVoice(init);
                }
            });
        }
    }

    /**
     * 一時停止時の処理
     */
    public void onPause() {
        if (chat.isVoiceMode()) {
            chat.stopVoice();
        } else if (chat.isTextMode()) {
            chat.stopText();
        }
        audioAdapter.pause();
    }

    /**
     * 終了時の処理
     */
    public void onDestroy() {
        INSTANCE = null;
        audioAdapter.destroy();
        chat.destroy();
        chatView = null;
    }

    /**
     * メタデータ受信時の処理
     *
     * @param meta メタデータ
     */
    private void onMetaOut(MetaData meta) {
        if (meta.utterance && meta.postback != null) {
            playAfterUtt.offer(null);
        }
        if (meta.switchAgent != null) {
            if (meta.switchAgent.afterUtt && chat.isVoiceMode()) {
                switchAfterUtt.set(meta.switchAgent.agentType);
            } else {
                onSwitchAgent(meta.switchAgent.agentType);
            }
        }
        switch (meta.type) {
            case SPEECHREC_RESULT:
                if (chat.isVoiceMode()) {
                    chat.setWaiting();
                }
                break;
            case NLU_RESULT:
                chat.clearWaiting();
                if (chat.isTextMode()) {
                    stop();
                } else if (!meta.utterance) {
                    chat.clearAutoStop();
                    chat.setAutoStop();
                }
                break;
        }
        for (Balloon balloon : meta.balloons) {
            if (balloon.payloads == null || balloon.payloads.isEmpty()) {
                continue;
            }
            balloon.position = balloonList.size();
            switch (balloon.action) {
                case PLAY:
                    audioAdapter.playWithoutView(balloon);
                    break;
                case PLAY_AFTER_UTT:
                    playAfterUtt.offer(balloon);
                    break;
            }
            show(balloon);
        }
        if (meta.postback != null) {
            if (meta.postback.afterUtt && chat.isVoiceMode()) {
                postBackAfterUtt.set(meta.postback);
            } else {
                putMeta(meta.postback.payload, meta.postback.clientData);
            }
        }
    }

    /**
     * 吹き出しを表示
     *
     * @param balloon 吹き出し
     */
    public void show(Balloon balloon) {
        balloonList.add(balloon);
        balloonAdapter.notifyDataSetChanged();
        scrollDown();
    }

    /**
     * 最下部までスクロール
     */
    public synchronized void scrollDown() {
        chatView.setSelection(balloonList.size());
    }

    /**
     * エージェント切り替え時の処理
     *
     * @param agentType エージェント区分
     */
    private void onSwitchAgent(AgentType agentType) {
        if (agentType == AgentType.MAIN) {
            chatView.setBackgroundColor(Color.WHITE);
        } else if (agentType == AgentType.EXPERT) {
            chatView.setBackgroundResource(R.color.expertAgent);
        }
    }

    /**
     * 合成音声再生後のメディア再生をクリア
     */
    public void clearPlayAfterUtt() {
        playAfterUtt.clear();
    }

    /**
     * データ送信
     *
     * @param data 送信データ
     */
    public void put(Object data) {
        if (data instanceof NluMetaData) {
            sdk.putMeta((NluMetaData) data);
        } else if (data != null) {
            sdk.putText(data.toString());
        }
    }

    /**
     * NLUメタデータを送信
     *
     * @param text       送信文字列
     * @param clientData クライアント情報
     */
    public void putMeta(String text, Map clientData) {
        NluMetaData meta = new NluMetaData();
        meta.voiceText = text;
        meta.clientData = (clientData != null) ? clientData : new HashMap<>();
        if (chat.isVoiceMode()) {
            meta.clientData.put(DeviceInfo.KEY, TTS_ON);
            sdk.putMeta(meta);
        } else {
            meta.clientData.put(DeviceInfo.KEY, TTS_OFF);
            start(ChatStartHandler.forTextMode(meta));
        }
    }

    /**
     * 音声入力OFF
     */
    public void mute() {
        sdk.mute();
    }

    /**
     * 音声入力ON
     */
    public void unmute() {
        sdk.unmute();
    }

    /**
     * 合成音声再生キャンセル
     */
    public void cancelPlay() {
        sdk.cancelPlay();
    }

    /**
     * HFPモード判定
     *
     * @return HFPモードの場合にtrue
     */
    public boolean isEnabledHFP() {
        return sdk.isEnabledHFP();
    }

    /**
     * SDKの開始
     *
     * @param onStart 開始時の処理
     */
    void start(ChatStartHandler onStart) {
        chat.setStatus(ChatStatus.STARTING);
        sdk.setMicMute(onStart.mode == ChatMode.TEXT);
        Config config = Config.getInstance();
        sdk.set("UseSSL", config.isSSL());
        sdk.setHost(config.getHost());
        sdk.setPort(config.getPort());
        sdk.setURLPath(config.getPath());
        sdk.setAccessToken(config.getAccessToken());
        sdk.start(onStart, new ChatErrorHandler(onStart));
    }

    /**
     * SDKの停止
     */
    void stop() {
        clearPlayAfterUtt();
        chat.setStatus(ChatStatus.STOP);
        chat.setSubtitle(R.string.stop);
        sdk.stop(new EventHandler() {
            @Override
            public void run() {
            }
        });
    }

}
