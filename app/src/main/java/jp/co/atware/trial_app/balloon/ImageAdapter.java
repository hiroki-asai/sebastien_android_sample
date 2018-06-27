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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;

import jp.co.atware.trial_app.R;
import jp.co.atware.trial_app.chat.ChatApplication;

/**
 * ImageViewにネット画像を設定
 */
public class ImageAdapter extends AsyncTask<String, Void, RecycleBitmapDrawable> {

    private static volatile Integer BALLOON_WIDTH;

    private final ImageView imageView;
    private boolean scroll;

    /**
     * コンストラクタ
     *
     * @param imageView ImageView
     */
    public ImageAdapter(ImageView imageView) {
        this.imageView = imageView;
        imageView.setTag(this);
    }

    /**
     * スクロールフラグをセット
     *
     * @param scroll 画像表示後にスクロールする場合にtrue
     */
    public void setScroll(boolean scroll) {
        this.scroll = scroll;
    }

    @Override
    protected RecycleBitmapDrawable doInBackground(String... params) {
        String imageUrl = params[0];
        ImageCache cache = ImageCache.getInstance();
        // 画像をディスクキャッシュから取得
        RecycleBitmapDrawable drawable = cache.load(imageUrl);
        if (drawable != null) {
            // ディスクキャッシュから取得した画像をメモリキャッシュに格納
            cache.put(imageUrl, drawable);
        } else {
            try {
                byte[] data = getData(imageUrl);
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, opts);
                // 画像のリサイズ
                int scale = (int) Math.floor((float) opts.outWidth / getBalloonWidth());
                if (2 < scale) {
                    for (int i = 2; i <= scale; i *= 2) {
                        opts.inSampleSize = i;
                    }
                }
                opts.inJustDecodeBounds = false;
                opts.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
                drawable = new RecycleBitmapDrawable(ChatApplication.getInstance().getResources(), bitmap);
                // リサイズした画像をキャッシュに格納
                cache.put(imageUrl, drawable);
                cache.save(imageUrl, drawable);
            } catch (InterruptedIOException ignore) {
            } catch (Exception e) {
                Log.w("ImageAdapter", "unexpected error occurred.", e);
            }
        }
        return drawable;
    }

    /**
     * 吹き出しの横幅を取得
     *
     * @return 横幅のPixel数
     */
    private int getBalloonWidth() {
        if (BALLOON_WIDTH == null) {
            synchronized (ImageAdapter.class) {
                if (BALLOON_WIDTH == null) {
                    BALLOON_WIDTH = imageView.getContext().getResources().getDimensionPixelSize(R.dimen.balloon_width);
                }
            }
        }
        return BALLOON_WIDTH;
    }

    /**
     * HTTPで画像データを取得
     *
     * @param imageUrl 画像URL
     * @return 画像データ
     * @throws Exception 画像データ取得失敗
     */
    private byte[] getData(String imageUrl) throws Exception {
        HttpURLConnection http = null;
        try {
            URL url = new URL(imageUrl);
            http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("GET");
            http.connect();

            try (InputStream input = http.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                int b = 0;
                while ((b = input.read()) != -1) {
                    output.write(b);
                }
                return output.toByteArray();
            }
        } finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }

    @Override
    protected void onPostExecute(RecycleBitmapDrawable drawable) {
        if (drawable != null) {
            imageView.setImageDrawable(drawable);
            if (scroll) {
                ChatApplication.getInstance().scrollDown();
            }
        }
    }

}
