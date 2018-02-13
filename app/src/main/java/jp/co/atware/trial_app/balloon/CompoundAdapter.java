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

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

import jp.co.atware.trial_app.R;

import static jp.co.atware.trial_app.balloon.BalloonAdapter.addButton;
import static jp.co.atware.trial_app.balloon.BalloonAdapter.setImage;

/**
 * 複合テンプレートを表示
 */
public class CompoundAdapter extends PagerAdapter {

    private final ViewPager pager;
    private final List<Payload> pages;

    private final OnClickListener prev = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int current = pager.getCurrentItem();
            pager.setCurrentItem(current - 1);
        }
    };

    private final OnClickListener next = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int current = pager.getCurrentItem();
            pager.setCurrentItem(current + 1);
        }
    };

    /**
     * コンストラクタ
     *
     * @param pager ViewPager
     * @param pages ページ毎の表示情報
     */
    public CompoundAdapter(ViewPager pager, List<Payload> pages) {
        this.pager = pager;
        this.pages = pages;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = LayoutInflater.from(container.getContext()).inflate(R.layout.compound, null);
        // PayloadのデータをViewにセット
        Payload payload = pages.get(position);
        setImage((ImageView) view.findViewById(R.id.compound_image), payload.url, false);
        ((TextView) view.findViewById(R.id.compound_title)).setText(payload.title);
        ((TextView) view.findViewById(R.id.compound_text)).setText(payload.text);
        addButton((ViewGroup) view.findViewById(R.id.compound_button), payload.buttons);
        // ナビゲートボタンをセット
        if (position == 0) {
            view.findViewById(R.id.chat_icon).setVisibility(View.VISIBLE);
        }
        if (0 < position) {
            ImageButton button = (ImageButton) view.findViewById(R.id.compound_prev);
            button.setOnClickListener(prev);
            button.setVisibility(View.VISIBLE);
        }
        if (position < pages.size() - 1) {
            ImageButton button = (ImageButton) view.findViewById(R.id.compound_next);
            button.setOnClickListener(next);
            button.setVisibility(View.VISIBLE);
        }
        // ListView内のScrollViewを操作可能にする
        view.findViewById(R.id.inner_scroll).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    ScrollView sv = (ScrollView) v;
                    if (sv.getChildAt(0).getHeight() > sv.getHeight()) {
                        sv.requestDisallowInterceptTouchEvent(true);
                    }
                }
                return v.onTouchEvent(event);
            }
        });
        container.addView(view);
        return view;
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

}
