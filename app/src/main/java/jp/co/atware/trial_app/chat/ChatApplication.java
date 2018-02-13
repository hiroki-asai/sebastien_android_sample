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
import android.graphics.Color;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;
import android.widget.ListView;

import com.nttdocomo.flow.EventHandler;
import com.nttdocomo.flow.StringEventHandler;
import com.nttdocomo.sebastien.Sebastien;
import com.nttdocomo.sebastien.util.NluMetaData;

import java.io.File;
import java.io.IOException;
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
import jp.co.atware.trial_app.metadata.DeviceInfo;
import jp.co.atware.trial_app.metadata.DeviceInfo.PlayTTS;
import jp.co.atware.trial_app.metadata.MetaData;
import jp.co.atware.trial_app.metadata.MetaDataParser;
import jp.co.atware.trial_app.metadata.SwitchAgent.AgentType;
import jp.co.atware.trial_app.util.Config;

/**
 * 対話アプリ
 */
public class ChatApplication extends Application {

    private static final long HTTP_CACHE_SIZE = 10 * 1024 * 1024;

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

    private final Sebastien sdk = new Sebastien();
    private final List<Balloon> balloonList = new ArrayList<>();
    private final BalloonAdapter balloonAdapter = new BalloonAdapter(balloonList);
    private final AudioAdapter audioAdapter = AudioAdapter.getInstance();
    private final ChatController chat = ChatController.getInstance();
    private final MetaDataParser parser = new MetaDataParser();
    private final AtomicReference<AgentType> switchAfterUtt = new AtomicReference<>();
    private final Queue<Balloon> playAfterUtt = new LinkedList<>();

    private ListView chatView;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        // HTTPキャッシュのインストール
        try {
            HttpResponseCache.install(new File(getCacheDir(), "http"), HTTP_CACHE_SIZE);
        } catch (IOException e) {
            Log.w("ChatApplication", "HttpResponseCache is not installed.");
        }
    }

    /**
     * 初期化
     *
     * @param activity 対話アプリのMainActivity
     */
    public void init(MainActivity activity) {
        // WebViewキャッシュの削除
        new WebView(activity).clearCache(true);
        chatView = (ListView) activity.findViewById(R.id.chat_area);
        chatView.setOnScrollListener(balloonAdapter);
        chatView.setAdapter(balloonAdapter);
        chat.init(activity);
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
        // HTTPキャッシュを削除
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            try {
                cache.delete();
            } catch (IOException e) {
                Log.w("ChatApplication", "HttpResponseCache is not deleted.");
            }
        }
    }

    /**
     * 接続情報設定
     *
     * @param accessToken アクセストークン
     */
    public void setConnection(String accessToken) {
        Config config = Config.getInstance();
        sdk.set("UseSSL", config.isSSL());
        sdk.set("EnableOCSP", config.isOCSP());
        sdk.set("OutputGain", 1.00);
        sdk.setHost(config.getHost());
        sdk.setPort(config.getPort());
        sdk.setURLPath(config.getPath());
        sdk.setAccessToken(accessToken);
        sdk.setContext(getApplicationContext());
        // メタデータ受信時の処理
        sdk.setOnMetaOut(new StringEventHandler() {
            @Override
            public void run(String metaData) {
                Log.d("OnMetaOut", metaData);
                MetaData meta = parser.parse(metaData);
                if (meta != null) {
                    onMetaOut(meta);
                }
            }
        });
        // 合成音声再生開始時の処理
        sdk.setOnPlayStart(new StringEventHandler() {
            @Override
            public void run(String s) {
                audioAdapter.pause();
                chat.setSubtitle(R.string.playing_voice);
            }
        });
        // 合成音声再生終了時の処理
        sdk.setOnPlayEnd(new StringEventHandler() {
            @Override
            public void run(String s) {
                // 合成音声再生後のエージェント切り替え
                onSwitchAgent(switchAfterUtt.get());
                // 合成音声再生後のメディア再生
                synchronized (this) {
                    Balloon balloon = playAfterUtt.poll();
                    if (balloon != null && playAfterUtt.isEmpty()) {
                        audioAdapter.playAfterUtt(balloon);
                    } else if (!audioAdapter.isPlaying()) {
                        chat.setSubtitle(R.string.ready_to_talk);
                    }
                }
            }
        });
    }

    /**
     * メタデータ受信時の処理
     *
     * @param meta メタデータ
     */
    private synchronized void onMetaOut(MetaData meta) {
        if (meta.utterance && meta.postback != null) {
            playAfterUtt.offer(null);
        }
        if (meta.switchAgent != null) {
            if (chat.isVoiceMode() && meta.switchAgent.afterUtt) {
                switchAfterUtt.set(meta.switchAgent.agentType);
            } else {
                switchAfterUtt.set(null);
                onSwitchAgent(meta.switchAgent.agentType);
            }
        }
        switch (meta.type) {
            case SPEECHREC_RESULT:
                if (chat.isVoiceMode()) {
                    chat.clearAutoStop();
                    chat.setWaiting();
                }
                break;
            case NLU_RESULT:
                chat.clearWaiting();
                if (chat.isVoiceMode()) {
                    chat.setAutoStop();
                } else {
                    stop();
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
            putPostback(meta.postback.payload, meta.postback.clientData);
        }
    }

    /**
     * 吹き出しを表示
     *
     * @param balloon 吹き出し
     */
    public synchronized void show(Balloon balloon) {
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
    public synchronized void clearPlayAfterUtt() {
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
     * postbackを送信
     *
     * @param text       送信文字列
     * @param clientData クライアント情報
     */
    public void putPostback(String text, Map clientData) {
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
     * SDKの開始
     *
     * @param onStart 開始時の処理
     */
    void start(ChatStartHandler onStart) {
        chat.setStatus(ChatStatus.STARTING);
        sdk.setMicMute(onStart.mode == ChatMode.TEXT);
        sdk.start(onStart, new ChatErrorHandler(onStart.mode));
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
