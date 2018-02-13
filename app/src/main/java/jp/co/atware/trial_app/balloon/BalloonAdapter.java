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
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import jp.co.atware.trial_app.MainActivity;
import jp.co.atware.trial_app.R;
import jp.co.atware.trial_app.balloon.Balloon.Action;
import jp.co.atware.trial_app.balloon.Balloon.BalloonType;
import jp.co.atware.trial_app.balloon.BalloonButton.ButtonType;
import jp.co.atware.trial_app.chat.ChatApplication;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;


/**
 * 吹き出しを表示
 */
public class BalloonAdapter extends BaseAdapter implements OnScrollListener {

    /**
     * 表示するViewを保持するクラス
     */
    public static class ViewHolder {
        public final BalloonType type;
        public TextView textView;
        public AudioView audioView;
        public ImageView imageView;
        public HtmlView htmlView;
        public ViewGroup buttonGroup;
        public ViewPager compoundPager;

        /**
         * コンストラクタ
         *
         * @param type 吹き出し種別
         */
        public ViewHolder(BalloonType type) {
            this.type = type;
        }
    }

    private final List<Balloon> balloonList;
    private boolean scrollNow;

    /**
     * コンストラクタ
     *
     * @param balloonList 吹き出しリスト
     */
    public BalloonAdapter(List<Balloon> balloonList) {
        this.balloonList = balloonList;
    }

    @Override
    public int getCount() {
        return balloonList.size();
    }

    @Override
    public Object getItem(int position) {
        return balloonList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View row, @NonNull ViewGroup parent) {
        ViewHolder holder = null;
        Balloon balloon = balloonList.get(position);
        if (row == null || ((ViewHolder) row.getTag()).type != balloon.type) {
            row = LayoutInflater.from(parent.getContext()).inflate(balloon.type.layout, null);
            holder = createHolder(balloon.type, row);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }
        Payload payload = balloon.payloads.get(0);
        switch (holder.type) {
            case USER_VOICE:
            case AI_VOICE:
                holder.textView.setText(payload.text);
                break;
            case AUDIO:
                holder.audioView.setUrl(payload.url);
                holder.audioView.setTag(position);
                AudioAdapter audio = AudioAdapter.getInstance();
                audio.adapt(holder.audioView);
                if (balloon.action == Action.PLAY_AFTER_UTT) {
                    audio.setAfterUtt(holder.audioView);
                }
                break;
            case IMAGE:
                if (balloon.action == Action.SCROLL) {
                    balloon.action = Action.DO_NOTHING;
                    setImage(holder.imageView, payload.url, true);
                } else {
                    setImage(holder.imageView, payload.url, false);
                }
                break;
            case HTML:
                if (scrollNow) {
                    holder.htmlView.showProgress();
                } else {
                    holder.htmlView.load(payload);
                }
                break;
            case BUTTON:
                holder.textView.setText(payload.text);
                holder.buttonGroup.removeAllViews();
                addButton(holder.buttonGroup, payload.buttons);
                break;
            case COMPOUND:
                PagerAdapter adapter = new CompoundAdapter(holder.compoundPager, balloon.payloads);
                holder.compoundPager.setAdapter(adapter);
                break;
        }
        return row;
    }

    /**
     * 表示するViewを保持するオブジェクトを生成
     *
     * @param type 表示種別
     * @param row  行レイアウトのView
     * @return 表示するViewを保持するオブジェクト
     */
    private ViewHolder createHolder(BalloonType type, View row) {
        ViewHolder holder = new ViewHolder(type);
        switch (type) {
            case USER_VOICE:
            case AI_VOICE:
                holder.textView = (TextView) row.findViewById(R.id.balloon_text);
                break;
            case AUDIO:
                holder.audioView = (AudioView) row.findViewById(R.id.balloon_audio);
                break;
            case IMAGE:
                holder.imageView = (ImageView) row.findViewById(R.id.balloon_image);
                break;
            case HTML:
                holder.htmlView = (HtmlView) row.findViewById(R.id.balloon_html);
                holder.htmlView.setProgress((ProgressBar) row.findViewById(R.id.loading_html));
                break;
            case BUTTON:
                holder.textView = (TextView) row.findViewById(R.id.button_text);
                holder.buttonGroup = (ViewGroup) row.findViewById(R.id.balloon_button);
                break;
            case COMPOUND:
                holder.compoundPager = (ViewPager) row.findViewById(R.id.balloon_compound);
                break;
        }
        return holder;
    }

    /**
     * ネット画像をImageViewに設定
     *
     * @param imageView ImageView
     * @param url       画像URL
     * @param scroll    画像設定後にスクロールする場合にtrue
     */
    static void setImage(final ImageView imageView, String url, boolean scroll) {
        if (url != null) {
            ImageAdapter adapter = new ImageAdapter(imageView, url);
            adapter.setScroll(scroll);
            adapter.execute();
        }
    }

    /**
     * ボタンをViewGroupに追加
     *
     * @param viewGroup ViewGroup
     * @param buttons   吹き出し表示情報
     */
    static void addButton(ViewGroup viewGroup, List<BalloonButton> buttons) {
        if (buttons != null) {
            final MainActivity activity = (MainActivity) viewGroup.getContext();
            int i = 0;
            int primary = ContextCompat.getColor(activity, R.color.colorPrimary);
            int primaryDark = ContextCompat.getColor(activity, R.color.colorPrimaryDark);

            for (BalloonButton balloonButton : buttons) {
                Button button = new Button(activity);
                if (++i % 2 == 1) {
                    button.setBackgroundColor(primary);
                } else {
                    button.setBackgroundColor(primaryDark);
                }
                button.setText(balloonButton.title);
                button.setTextColor(Color.WHITE);

                final ButtonType type = balloonButton.type;
                final String value = balloonButton.value;
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ChatApplication app = ChatApplication.getInstance();
                        if (type == ButtonType.WEB_URL) {
                            activity.startBrowser(value);
                        } else {
                            app.putPostback(value, null);
                        }
                    }
                });
                button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(MATCH_PARENT, calcPx(activity, 36));
                viewGroup.addView(button, params);
            }
        }
    }

    /**
     * dp値からピクセル数を算出
     *
     * @param context Context
     * @param dp      dp値
     * @return ピクセル数
     */
    private static int calcPx(Context context, float dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {
            case SCROLL_STATE_IDLE:
                scrollNow = false;
                int first = view.getFirstVisiblePosition();
                int count = view.getChildCount();
                for (int i = 0; i < count; i++) {
                    ViewHolder holder = (ViewHolder) view.getChildAt(i).getTag();
                    if (holder.type == Balloon.BalloonType.HTML) {
                        Payload payload = balloonList.get(first + i).payloads.get(0);
                        holder.htmlView.load(payload);
                    }
                }
                break;
            default:
                scrollNow = true;
                break;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // do nothing
    }

}
