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

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

import jp.co.atware.trial_app.balloon.Balloon;
import jp.co.atware.trial_app.balloon.Balloon.Action;
import jp.co.atware.trial_app.balloon.Balloon.BalloonType;
import jp.co.atware.trial_app.balloon.BalloonButton;
import jp.co.atware.trial_app.balloon.BalloonButton.ButtonType;
import jp.co.atware.trial_app.balloon.Payload;
import jp.co.atware.trial_app.metadata.MetaData.MetaDataType;
import jp.co.atware.trial_app.metadata.SwitchAgent.AgentType;

import static org.junit.Assert.assertEquals;


/**
 * MetaDataParser単体試験
 */
public class MetaDataParserTest {

    /**
     * テスト実行
     *
     * @param metaData テストデータ
     * @param expected 期待値
     * @throws IOException 読み込み失敗
     */
    private void execute(String metaData, MetaData expected) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream(metaData), "UTF-8"))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        assertEquals(expected, new MetaDataParser().parse(sb.toString()));
    }

    /**
     * balloonが空のnlu_resultを解析
     *
     * @throws Exception テスト失敗
     */
    @Test
    public void test_balloon_empty() throws Exception {
        MetaData meta = new MetaData(MetaDataType.NLU_RESULT);
        meta.utterance = true;
        meta.balloons.add(new Balloon(BalloonType.AI_VOICE, "解析成功"));
        meta.switchAgent = new SwitchAgent();
        meta.switchAgent.agentId = "spftalk";
        meta.switchAgent.agentType = AgentType.MAIN;
        meta.switchAgent.afterUtt = true;
        meta.postback = new Postback();
        meta.postback.payload = "#DDB";

        execute("balloon_empty.json", meta);
    }

    /**
     * balloon.type:textのnlu_resultを解析
     *
     * @throws Exception テスト失敗
     */
    @Test
    public void test_text() throws Exception {
        Balloon balloon = new Balloon(BalloonType.AI_VOICE, "解析成功");

        MetaData meta = new MetaData(MetaDataType.NLU_RESULT);
        meta.utterance = true;
        meta.balloons.add(balloon);
        meta.switchAgent = new SwitchAgent();
        meta.switchAgent.agentId = "spftalk";
        meta.switchAgent.agentType = AgentType.MAIN;
        meta.switchAgent.afterUtt = true;
        meta.postback = new Postback();
        meta.postback.payload = "#DDB";
        meta.postback.clientData = Collections.emptyMap();

        execute("text.json", meta);
    }

    /**
     * balloon.type:textのnlu_result(payload無し)を解析
     *
     * @throws Exception テスト失敗
     */
    @Test
    public void test_text_empty() throws Exception {
        MetaData meta = new MetaData(MetaDataType.NLU_RESULT);
        meta.utterance = true;
        meta.switchAgent = new SwitchAgent();
        meta.switchAgent.agentId = "spftalk";
        meta.switchAgent.agentType = AgentType.MAIN;
        meta.switchAgent.afterUtt = true;
        meta.postback = new Postback();
        meta.postback.payload = "#DDB";

        execute("text_empty.json", meta);
    }


    /**
     * balloon.type:mediaのnlu_resultを解析
     *
     * @throws Exception テスト失敗
     */
    @Test
    public void test_media() throws Exception {
        Balloon balloon = new Balloon(BalloonType.IMAGE, "解析成功");

        MetaData meta = new MetaData(MetaDataType.NLU_RESULT);
        meta.utterance = true;
        meta.balloons.add(balloon);
        meta.switchAgent = new SwitchAgent();
        meta.switchAgent.agentId = "spftalk";
        meta.switchAgent.agentType = AgentType.MAIN;
        meta.switchAgent.afterUtt = true;

        execute("media.json", meta);
    }

    /**
     * balloon.type:imageのnlu_result(payload無し)を解析
     *
     * @throws Exception テスト失敗
     */
    @Test
    public void test_image_empty() throws Exception {
        MetaData meta = new MetaData(MetaDataType.NLU_RESULT);
        meta.utterance = true;
        meta.switchAgent = new SwitchAgent();
        meta.switchAgent.agentId = "spftalk";
        meta.switchAgent.agentType = AgentType.MAIN;
        meta.switchAgent.afterUtt = true;

        execute("media_empty.json", meta);
    }

    /**
     * payload.type:buttonのnlu_resultを解析
     *
     * @throws Exception テスト失敗
     */
    @Test
    public void test_button() throws Exception {
        BalloonButton btn1 = new BalloonButton(ButtonType.WEB_URL);
        btn1.title = "リンクを開く";
        btn1.value = "リンク";

        BalloonButton btn2 = new BalloonButton(ButtonType.POSTBACK);
        btn2.title = "キャンセル";
        btn2.value = "#CANCEL";

        Payload payload = new Payload();
        payload.text = "解析成功";
        payload.buttons = Arrays.asList(btn1, btn2);

        Balloon balloon = new Balloon(BalloonType.BUTTON, Collections.singletonList(payload));

        MetaData meta = new MetaData(MetaDataType.NLU_RESULT);
        meta.utterance = true;
        meta.balloons.add(balloon);

        execute("button.json", meta);
    }

    /**
     * payload.type:imageのnlu_result(buttons無し)を解析
     *
     * @throws Exception テスト失敗
     */
    @Test
    public void test_button_empty() throws Exception {
        Payload payload = new Payload();
        payload.text = "解析成功";
        Balloon balloon = new Balloon(BalloonType.BUTTON, Collections.singletonList(payload));

        MetaData meta = new MetaData(MetaDataType.NLU_RESULT);
        meta.utterance = true;
        meta.balloons.add(balloon);

        execute("button_empty.json", meta);
    }

    /**
     * payload.type:compoundのnlu_resultを解析
     *
     * @throws Exception テスト失敗
     */
    @Test
    public void test_compound() throws Exception {
        BalloonButton btn1_1 = new BalloonButton(ButtonType.WEB_URL);
        btn1_1.title = "リンク1を開く";
        btn1_1.value = "リンク1";

        BalloonButton btn1_2 = new BalloonButton(ButtonType.POSTBACK);
        btn1_2.title = "キャンセル1";
        btn1_2.value = "#CANCEL_1";

        Payload payload1 = new Payload();
        payload1.title = "タイトル1";
        payload1.text = "テキスト1";
        payload1.url = "画像1";
        payload1.buttons = Arrays.asList(btn1_1, btn1_2);

        BalloonButton btn2_1 = new BalloonButton(ButtonType.WEB_URL);
        btn2_1.title = "リンク2を開く";
        btn2_1.value = "リンク2";

        BalloonButton btn2_2 = new BalloonButton(ButtonType.POSTBACK);
        btn2_2.title = "キャンセル2";
        btn2_2.value = "#CANCEL_2";

        Payload payload2 = new Payload();
        payload2.title = "タイトル2";
        payload2.text = "テキスト2";
        payload2.url = "画像2";
        payload2.buttons = Arrays.asList(btn2_1, btn2_2);

        Balloon balloon = new Balloon(BalloonType.COMPOUND, Arrays.asList(payload1, payload2));

        MetaData meta = new MetaData(MetaDataType.NLU_RESULT);
        meta.balloons.add(balloon);
        meta.utterance = true;

        execute("compound.json", meta);
    }

    /**
     * payload.type:compoundのnlu_result(trays無し)を解析
     *
     * @throws Exception テスト失敗
     */
    @Test
    public void test_compound_empty() throws Exception {
        MetaData meta = new MetaData(MetaDataType.NLU_RESULT);
        meta.utterance = true;
        meta.balloons.add(new Balloon(BalloonType.COMPOUND));

        execute("compound_empty.json", meta);
    }

    /**
     * balloon.type:templateのnlu_result(payload無し)を解析
     *
     * @throws Exception テスト失敗
     */
    public void test_template_empty() throws Exception {
        MetaData meta = new MetaData(MetaDataType.NLU_RESULT);
        meta.utterance = true;
        meta.balloons.add(new Balloon(BalloonType.AI_VOICE, "解析成功"));

        execute("template_empty.json", meta);
    }

    /**
     * 複数の吹き出しがあるnlu_resultを解析
     *
     * @throws Exception テスト失敗
     */
    @Test
    public void test_multi_balloons() throws Exception {
        // text
        Balloon text = new Balloon(BalloonType.AI_VOICE, "テキスト");
        // image
        Balloon image = new Balloon(BalloonType.IMAGE, "画像URL");
        // audio
        Balloon audio1 = new Balloon(BalloonType.AUDIO, "音声URL1");
        // html
        Balloon html = new Balloon(BalloonType.HTML, "WebURL");
        // video
        Balloon video = new Balloon(BalloonType.AI_VOICE, "video/mpeg\n動画URL");
        // button
        BalloonButton btn1 = new BalloonButton(ButtonType.WEB_URL);
        btn1.title = "リンクを開く";
        btn1.value = "リンク";

        BalloonButton btn2 = new BalloonButton(ButtonType.POSTBACK);
        btn2.title = "キャンセル";
        btn2.value = "#CANCEL";

        Payload button = new Payload();
        button.text = "タイトル";
        button.buttons = Arrays.asList(btn1, btn2);

        Balloon buttonBal = new Balloon(BalloonType.BUTTON, Collections.singletonList(button));
        // compound
        BalloonButton btn1_1 = new BalloonButton(ButtonType.WEB_URL);
        btn1_1.title = "リンク1を開く";
        btn1_1.value = "リンク1";

        BalloonButton btn1_2 = new BalloonButton(ButtonType.POSTBACK);
        btn1_2.title = "キャンセル1";
        btn1_2.value = "#CANCEL_1";

        Payload compound1 = new Payload();
        compound1.title = "タイトル1";
        compound1.text = "テキスト1";
        compound1.url = "画像1";
        compound1.buttons = Arrays.asList(btn1_1, btn1_2);

        BalloonButton btn2_1 = new BalloonButton(ButtonType.WEB_URL);
        btn2_1.title = "リンク2を開く";
        btn2_1.value = "リンク2";

        BalloonButton btn2_2 = new BalloonButton(ButtonType.POSTBACK);
        btn2_2.title = "キャンセル2";
        btn2_2.value = "#CANCEL_2";

        Payload compound2 = new Payload();
        compound2.title = "タイトル2";
        compound2.text = "テキスト2";
        compound2.url = "画像2";
        compound2.buttons = Arrays.asList(btn2_1, btn2_2);

        Balloon compoundBal = new Balloon(BalloonType.COMPOUND, Arrays.asList(compound1, compound2));

        MetaData meta = new MetaData(MetaDataType.NLU_RESULT);
        meta.utterance = true;
        meta.balloons.add(text);
        meta.balloons.add(image);
        meta.balloons.add(audio1);
        meta.balloons.add(html);
        meta.balloons.add(video);
        meta.balloons.add(buttonBal);
        meta.balloons.add(compoundBal);
        meta.postback = new Postback();
        meta.postback.payload = "#DDB";
        meta.postback.clientData = new LinkedHashMap();
        meta.postback.clientData.put("name", "coke");
        meta.postback.clientData.put("level", 99);

        execute("multi_balloons.json", meta);
    }

}