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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

/**
 * メモリ解放機能を実装したBitmapDrawable
 */
public class RecycleBitmapDrawable extends BitmapDrawable {

    private int cacheCount = 0;
    private int displayCount = 0;
    private boolean hasBeenDisplayed;

    /**
     * コンストラクタ
     *
     * @param res    Resource
     * @param bitmap Bitmap
     */
    public RecycleBitmapDrawable(Resources res, Bitmap bitmap) {
        super(res, bitmap);
    }

    /**
     * 表示フラグをセット
     *
     * @param displayed 表示されている場合にtrue
     */
    public void setDisplayed(boolean displayed) {
        synchronized (this) {
            if (displayed) {
                displayCount++;
                hasBeenDisplayed = true;
            } else {
                displayCount--;
            }
        }
        recycle();
    }


    /**
     * キャッシュフラグをセット
     *
     * @param cached キャッシュされている場合にtrue
     */
    public void setCached(boolean cached) {
        synchronized (this) {
            if (cached) {
                cacheCount++;
            } else {
                cacheCount--;
            }
        }
        recycle();
    }

    /**
     * メモリ解放
     */
    private synchronized void recycle() {
        Bitmap bitmap = null;
        if (cacheCount <= 0 && displayCount <= 0 && hasBeenDisplayed
                && (bitmap = getBitmap()) != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}
