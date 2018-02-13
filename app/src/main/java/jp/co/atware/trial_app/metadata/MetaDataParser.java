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

package jp.co.atware.trial_app.metadata;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jp.co.atware.trial_app.balloon.Balloon;
import jp.co.atware.trial_app.balloon.Balloon.Action;
import jp.co.atware.trial_app.balloon.Balloon.BalloonType;
import jp.co.atware.trial_app.balloon.BalloonButton;
import jp.co.atware.trial_app.balloon.BalloonButton.ButtonType;
import jp.co.atware.trial_app.balloon.Payload;


/**
 * メタデータ解析
 */
public class MetaDataParser {

    static final String SPEECHREC_RESULT = "speechrec_result";
    static final String NLU_RESULT = "nlu_result";
    static final String TYPE = "type";
    static final String SENTENCES = "sentences";
    static final String VOICE_TEXT = "voiceText";
    static final String SYSTEM_TEXT = "systemText";
    static final String EXPRESSION = "expression";
    static final String SWITCH_AGENT = "switchAgent";
    static final String AGENT_ID = "agentId";
    static final String AGENT_TYPE = "agentType";
    static final String POSTBACK = "postback";
    static final String BALLOON = "balloon";
    static final String TEXT = "text";
    static final String MEDIA = "media";
    static final String URL = "url";
    static final String IMAGE_URL = "imageUrl";
    static final String TEMPLATE = "template";
    static final String PAYLOAD = "payload";
    static final String BUTTON = "button";
    static final String BUTTONS = "buttons";
    static final String TITLE = "title";
    static final String COMPOUND = "compound";
    static final String TRAYS = "trays";
    static final String OPTION = "option";
    static final String AFTER_UTT = "afterUtt";
    static final String CLIENT_DATA = "clientData";
    static final String CONTENT_TYPE = "contentType";
    static final String IMAGE = "image";
    static final String AUDIO = "audio";
    static final String UTTERANCE = "utterance";
    static final String TEXT_HTML = "text/html";

    /**
     * メタデータ解析
     *
     * @param metaData メタデータ文字列
     * @return 解析結果
     */
    public MetaData parse(String metaData) {
        try {
            Map metaMap = new ObjectMapper().readValue(metaData, LinkedHashMap.class);
            if (metaMap.containsKey(SPEECHREC_RESULT)) {
                return parseSpeechRec(metaMap);
            } else if (NLU_RESULT.equals(metaMap.get(TYPE))) {
                return parseNluResult(metaMap);
            }
        } catch (IOException e) {
            Log.e("Parser", "unexpected error occurred.", e);
        }
        return null;
    }

    /**
     * 音声認識結果を解析
     *
     * @param metaMap メタデータMap
     * @return 解析結果
     */
    MetaData parseSpeechRec(Map metaMap) {
        List<Map> sentences = findMapList(metaMap, SENTENCES);
        if (sentences != null) {
            for (Map sentence : sentences) {
                String voiceText = findString(sentence, VOICE_TEXT);
                if (voiceText != null && !voiceText.isEmpty()) {
                    MetaData result = new MetaData(MetaData.MetaDataType.SPEECHREC_RESULT);
                    result.balloons.add(new Balloon(BalloonType.USER_VOICE, voiceText));
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * NLU処理結果を解析
     *
     * @param metaMap メタデータMap
     * @return 解析結果
     */
    MetaData parseNluResult(Map metaMap) {
        MetaData result = new MetaData(MetaData.MetaDataType.NLU_RESULT);
        result.utterance = !isEmpty(findString(findMap(metaMap, SYSTEM_TEXT), UTTERANCE));

        Map optionMap = findMap(metaMap, OPTION);
        // switchAgentとpostbackを取得
        result.switchAgent = getSwitchAgent(optionMap);
        result.postback = getPostback(optionMap);
        //　吹き出しを取得
        List<Map> balloonList = findMapList(optionMap, BALLOON);
        if (balloonList != null) {
            for (Map balloonMap : balloonList) {
                Balloon balloon = getBalloon(balloonMap);
                if (balloon != null) {
                    result.balloons.add(balloon);
                }
            }
            // メディア音声の自動再生フラグを設定
            for (Balloon balloon : result.balloons) {
                if (balloon.type == BalloonType.AUDIO) {
                    if (!result.utterance) {
                        // 発話が無ければ即再生
                        balloon.action = Action.PLAY;
                    } else if(result.postback == null){
                        // 発話後に再生
                        balloon.action = Action.PLAY_AFTER_UTT;
                    }
                    break;
                }
            }
        } else {
            // 吹き出しが無い場合はexpressionを吹き出しとする
            result.balloons.add((new Balloon(BalloonType.AI_VOICE,
                    findString(findMap(metaMap, SYSTEM_TEXT), EXPRESSION))));
        }
        return result;
    }

    /**
     * 吹き出しを取得
     *
     * @param balloonMap 吹き出し情報Map
     * @return 吹き出し
     */
    Balloon getBalloon(Map balloonMap) {
        String balloonType = findString(balloonMap, TYPE);
        if (balloonType != null) {
            Map payloadMap = findMap(balloonMap, PAYLOAD);
            switch (balloonType) {
                case TEXT:
                    String expression = findString(payloadMap, EXPRESSION);
                    if (!isEmpty(expression)) {
                        return new Balloon(BalloonType.AI_VOICE, findString(payloadMap, EXPRESSION));
                    }
                    break;
                case MEDIA:
                    return getMediaBalloon(payloadMap);
                case TEMPLATE:
                    String payloadType = findString(payloadMap, TYPE);
                    if (payloadType != null) {
                        switch (payloadType) {
                            case BUTTON:
                                return new Balloon(BalloonType.BUTTON, getButtonPayload(payloadMap));
                            case COMPOUND:
                                return new Balloon(BalloonType.COMPOUND, getCompoundPayload(payloadMap));
                        }
                    }
                    break;
            }
        }
        return null;
    }

    /**
     * メデイア形式の吹き出しを取得
     *
     * @param payloadMap 表示情報Map
     * @return 吹き出し
     */
    Balloon getMediaBalloon(Map payloadMap) {
        String contentType = findString(payloadMap, CONTENT_TYPE);
        String url = findString(payloadMap, URL);
        if (contentType == null || url == null) {
            return null;
        }
        if (contentType.startsWith(IMAGE)) {
            return new Balloon(BalloonType.IMAGE, url);
        } else if (contentType.startsWith(AUDIO)) {
            return new Balloon(BalloonType.AUDIO, url);
        } else if (contentType.equals(TEXT_HTML)){
            return new Balloon(BalloonType.HTML, url);
        }
        return new Balloon(BalloonType.AI_VOICE, contentType + "\n" + url);
    }

    /**
     * ボタン表示情報を取得
     *
     * @param payloadMap 表示情報Map
     * @return ボタン表示情報
     */
    List<Payload> getButtonPayload(Map payloadMap) {
        Payload payload = new Payload();
        payload.text = findString(payloadMap, TEXT);
        payload.buttons = getButtons(findMapList(payloadMap, BUTTONS));
        return Collections.singletonList(payload);
    }

    /**
     * 複合テンプレートの表示情報を取得
     *
     * @param payloadMap 表示情報Map
     * @return 複合テンプレートの表示情報
     */
    List<Payload> getCompoundPayload(Map payloadMap) {
        List<Map> trays = findMapList(payloadMap, TRAYS);
        if (trays != null) {
            List<Payload> payloadList = new ArrayList<>();
            for (Map trayMap : trays) {
                Payload payload = new Payload();
                payload.title = findString(trayMap, TITLE);
                payload.url = findString(trayMap, IMAGE_URL);
                payload.text = findString(trayMap, TEXT);
                payload.buttons = getButtons(findMapList(trayMap, BUTTONS));
                payloadList.add(payload);
            }
            return payloadList;
        }
        return null;
    }

    /**
     * ボタン情報リストを取得
     *
     * @param buttons ボタン情報MapのList
     * @return ボタン情報リスト
     */
    List<BalloonButton> getButtons(List<Map> buttons) {
        if (buttons != null) {
            List<BalloonButton> buttonList = new ArrayList<>();
            for (Map buttonMap : buttons) {
                ButtonType type = ButtonType.of(findString(buttonMap, TYPE));
                if (type == null) {
                    continue;
                }
                BalloonButton button = new BalloonButton(type);
                button.title = findString(buttonMap, TITLE);
                button.value = (type == ButtonType.WEB_URL) ?
                        findString(buttonMap, URL) : findString(findMap(buttonMap, PAYLOAD), TEXT);
                if (button.title != null && button.value != null) {
                    buttonList.add(button);
                }
            }
            return buttonList;
        }
        return null;
    }

    /**
     * エージェント切り替え情報を取得
     *
     * @param optionMap オプション情報Map
     * @return エージェント切り替え情報
     */
    SwitchAgent getSwitchAgent(Map optionMap) {
        Map map = findMap(optionMap, SWITCH_AGENT);
        if (map != null) {
            SwitchAgent result = new SwitchAgent();
            result.agentId = findString(map, AGENT_ID);
            result.agentType = SwitchAgent.AgentType.of(findString(map, AGENT_TYPE));
            result.afterUtt = Boolean.valueOf(findString(map, AFTER_UTT));
            return result;
        }
        return null;
    }

    /**
     * サーバ返信情報を取得
     *
     * @param optionMap オプション情報Map
     * @return サーバ返信情報
     */
    Postback getPostback(Map optionMap) {
        Map map = findMap(optionMap, POSTBACK);
        if (map != null) {
            Postback result = new Postback();
            result.payload = findString(map, PAYLOAD);
            result.clientData = findMap(map, CLIENT_DATA);
            return result;
        }
        return null;
    }

    /**
     * 指定したキーの文字列を取得
     *
     * @param map 取得対象のMap
     * @param key キー
     * @return 文字列
     */
    String findString(Map map, String key) {
        Object value = findValue(map, key);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    /**
     * 指定したキーのMapを取得
     *
     * @param map 取得対象のMap
     * @param key キー
     * @return Map
     */
    Map findMap(Map map, String key) {
        Object value = findValue(map, key);
        if (value instanceof Map) {
            return (Map) value;
        }
        return null;
    }

    /**
     * 指定したキーのMapリストを取得
     *
     * @param map 取得対象のMap
     * @param key キー
     * @return Mapリスト
     */
    List<Map> findMapList(Map map, String key) {
        Object value = findValue(map, key);
        if (isMapList(value)) {
            return (List<Map>) value;
        }
        return null;
    }

    /**
     * 指定したキーの値を取得
     *
     * @param map 取得対象のMap
     * @param key キー
     * @return 値
     */
    Object findValue(Map map, String key) {
        if (map != null) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
            Deque<Map> deque = new LinkedList<>();
            deque.add(map);
            while (!deque.isEmpty()) {
                Map<?, ?> target = deque.pollFirst();
                for (Map.Entry<?, ?> entry : target.entrySet()) {
                    Object value = entry.getValue();
                    if (entry.getKey().equals(key)) {
                        return value;
                    }
                    if (value instanceof Map) {
                        deque.offerFirst((Map) value);
                    } else if (isMapList(value)) {
                        for (Object e : (List) value) {
                            deque.offerLast((Map) e);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Mapリスト判定
     *
     * @param value 判定対象のObject
     * @return Mapを格納するリストの場合にtrue
     */
    boolean isMapList(Object value) {
        if (value instanceof List && !((List) value).isEmpty()) {
            return ((List) value).get(0) instanceof Map;
        }
        return false;
    }

    /**
     * 空文字判定
     *
     * @param s 文字列
     * @return 文字列がnullまたは空文字の場合にtrue
     */
    boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
