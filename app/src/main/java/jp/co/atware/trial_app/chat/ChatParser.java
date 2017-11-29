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

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jp.co.atware.trial_app.chat.Chat.Agent;
import jp.co.atware.trial_app.chat.Chat.Person;


/**
 * 発話情報解析
 */
public class ChatParser {

    private static final String SPEECHREC_RESULT = "speechrec_result";
    private static final String NLU_RESULT = "nlu_result";
    private static final String TYPE = "type";
    private static final String SENTENCES = "sentences";
    private static final String VOICE_TEXT = "voiceText";
    private static final String SYSTEM_TEXT = "systemText";
    private static final String EXPRESSION = "expression";
    private static final String SWITCH_AGENT = "switchAgent";
    private static final String AGENT_TYPE = "agentType";
    private static final String POSTBACK = "postback";
    private static final String PAYLOAD = "payload";
    private static final String EMPTY = "";

    /**
     * 発話情報解析
     *
     * @param metaData メタデータ
     * @return 発話情報
     */
    public static Chat parse(String metaData) {
        Log.d("metadata", metaData);
        try {
            Map<?, ?> map = new ObjectMapper().readValue(metaData, LinkedHashMap.class);
            if (map.containsKey(SPEECHREC_RESULT)) {
                return getUserChat(map);
            } else if (NLU_RESULT.equals(map.get(TYPE))) {
                return getNluChat(map);
            }
        } catch (IOException e) {
            Log.d("metadata", "parse failed.", e);
        }
        return null;
    }

    /**
     * ユーザの発話情報を取得
     *
     * @param map メタ情報Map
     * @return ユーザの発話情報
     */
    private static Chat getUserChat(Map<?, ?> map) {
        Object value = findValue(map, SENTENCES);
        if (value instanceof List) {
            for (Object e : (List) value) {
                if (e instanceof Map) {
                    String voiceText = toString(findValue((Map) e, VOICE_TEXT));
                    if (!voiceText.isEmpty()) {
                        Chat chat = new Chat(Person.USER);
                        chat.text = voiceText;
                        return chat;
                    }
                }
            }
        }
        return null;
    }

    /**
     * NLUの発話情報を取得
     *
     * @param map メタ情報Map
     * @return NLUの発話情報
     */
    private static Chat getNluChat(Map<?, ?> map) {
        Chat chat = new Chat(Person.NLU);
        chat.text = getSystemText(map);
        chat.switchAgent = getSwitchAgent(map);
        chat.replyMessage = getReplyMessage(map);
        return chat;
    }

    /**
     * システム発話文字列の取得
     *
     * @param map メタ情報Map
     * @return システム発話文字列
     */
    private static String getSystemText(Map<?, ?> map) {
        Object systemText = findValue(map, SYSTEM_TEXT);
        Object expression = null;
        if ((systemText instanceof Map) &&
                (expression = ((Map) systemText).get(EXPRESSION)) != null) {
            return expression.toString();
        }
        return null;
    }

    /**
     * エージェント切替情報の取得
     *
     * @param map メタ情報Map
     * @return 切り替えられたエージェント
     */
    private static Agent getSwitchAgent(Map<?, ?> map) {
        Object value = findValue(map, SWITCH_AGENT);
        Object agentType = null;
        if ((value instanceof Map) &&
                (agentType = ((Map) value).get(AGENT_TYPE)) != null) {
            return (agentType.toString().equals("1")) ? Agent.PERSONAL : Agent.EXPERT;
        }
        return null;
    }

    /**
     * 返信メッセージを取得
     *
     * @param map メタ情報Map
     * @return 返信メッセージ
     */
    private static String getReplyMessage(Map<?, ?> map) {
        Object value = findValue(map, POSTBACK);
        Object message = null;
        if ((value instanceof Map) &&
                (message = ((Map) value).get(PAYLOAD)) != null) {
            return message.toString();
        }
        return null;
    }

    /**
     * Mapから指定したキーの値を取得
     *
     * @param target 取得対象のMap
     * @param key    キー
     * @return 値
     */
    private static Object findValue(Map<?, ?> target, String key) {
        Deque<Map<?, ?>> deque = new LinkedList<>();
        deque.add(target);
        while (!deque.isEmpty()) {
            Map<?, ?> map = deque.pollFirst();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
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
        return null;
    }

    /**
     * Mapリスト判定
     *
     * @param target 判定対象のObject
     * @return Mapを格納するリストの場合にtrue
     */
    private static boolean isMapList(Object target) {
        if (target instanceof List && !((List) target).isEmpty()) {
            return ((List) target).get(0) instanceof Map;
        }
        return false;
    }

    /**
     * オブジェクトを文字列に変換
     *
     * @param o オブジェクト
     * @return 変換された文字列
     */
    private static String toString(Object o) {
        if (o != null) {
            return o.toString();
        }
        return EMPTY;
    }

}
