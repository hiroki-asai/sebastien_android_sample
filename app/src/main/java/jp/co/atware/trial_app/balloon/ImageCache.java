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

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;

import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.disklrucache.DiskLruCache.Editor;
import com.jakewharton.disklrucache.DiskLruCache.Snapshot;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jp.co.atware.trial_app.chat.ChatApplication;

/**
 * 画像キャッシュ
 */
public class ImageCache {

    private static final String CACHE_DIR = "images";
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;
    private static final long MAX_SIZE = 24 * 1024 * 1024;
    private static final int BUFFER_SIZE = 8 * 1024;
    private static final int QUALITY = 100;

    private static volatile ImageCache INSTANCE = null;

    /**
     * Singletonインスタンスを取得
     *
     * @return インスタンス
     */
    public static ImageCache getInstance() {
        if (INSTANCE == null) {
            synchronized (ImageCache.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ImageCache();
                }
            }
        }
        return INSTANCE;
    }

    private final LruCache<Integer, RecycleBitmapDrawable> memCache;
    private final DiskLruCache diskCache;

    /**
     * コンストラクタ
     */
    private ImageCache() {
        Context context = ChatApplication.getInstance().getApplicationContext();
        memCache = createMemCache(context);
        diskCache = createDiskCache(context);
    }

    /**
     * メモリキャッシュを生成
     *
     * @param context Context
     * @return メモリキャッシュ
     */
    private LruCache<Integer, RecycleBitmapDrawable> createMemCache(Context context) {
        int memClass = ((ActivityManager) context.
                getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
        int maxSize = memClass * 1024 * 1024 / 8;
        return new LruCache<Integer, RecycleBitmapDrawable>(maxSize) {
            @Override
            protected int sizeOf(Integer key, RecycleBitmapDrawable value) {
                return value.getBitmap().getByteCount();
            }

            @Override
            protected void entryRemoved(boolean evicted, Integer key,
                                        RecycleBitmapDrawable oldValue, RecycleBitmapDrawable newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                oldValue.setCached(false);
            }
        };
    }

    /**
     * ディスクキャッシュを生成
     *
     * @param context Context
     * @return ディスクキャッシュ
     */
    private DiskLruCache createDiskCache(Context context) {
        try {
            File dir = new File(context.getCacheDir(), CACHE_DIR);
            return DiskLruCache.open(dir, APP_VERSION, VALUE_COUNT, MAX_SIZE);
        } catch (IOException e) {
            Log.w("ImageCache", "unable to open disk cache.", e);
        }
        return null;
    }

    /**
     * 画像データをメモリキャッシュから取得
     *
     * @param imageUrl 画像URL
     * @return 画像データ
     */
    public RecycleBitmapDrawable get(String imageUrl) {
        return memCache.get(imageUrl.hashCode());
    }


    /**
     * 画像データをメモリキャッシュに格納
     *
     * @param imageUrl 画像URL
     * @param drawable 画像データ
     */
    public void put(String imageUrl, RecycleBitmapDrawable drawable) {
        if (get(imageUrl) == null) {
            drawable.setCached(true);
            memCache.put(imageUrl.hashCode(), drawable);
        }
    }


    /**
     * 画像データをディスクキャッシュから取得
     *
     * @param imageUrl 画像URL
     * @return 画像データ
     */
    public RecycleBitmapDrawable load(String imageUrl) {
        if (diskCache == null) {
            return null;
        }
        Snapshot snapshot = null;
        try {
            snapshot = diskCache.get(String.valueOf(imageUrl.hashCode()));
            if (snapshot == null) {
                return null;
            }
            final InputStream in = snapshot.getInputStream(0);
            if (in != null) {
                final BufferedInputStream buffIn =
                        new BufferedInputStream(in, BUFFER_SIZE);
                Bitmap bitmap = BitmapFactory.decodeStream(buffIn);
                return new RecycleBitmapDrawable(ChatApplication.getInstance().getResources(), bitmap);

            }
        } catch (IOException e) {
            Log.w("ImageCache", "unable to load cache.", e);
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }
        return null;
    }

    /**
     * 画像データをディスクキャッシュに保存
     *
     * @param imageUrl 画像URL
     * @param drawable 画像データ
     */
    public void save(String imageUrl, RecycleBitmapDrawable drawable) {
        if (diskCache == null) {
            return;
        }
        Editor editor = null;
        try {
            editor = diskCache.edit(String.valueOf(imageUrl.hashCode()));
            if (editor == null) {
                return;
            }
            try (OutputStream out = new BufferedOutputStream(editor.newOutputStream(0), BUFFER_SIZE)) {
                if (drawable.getBitmap().compress(Bitmap.CompressFormat.PNG, QUALITY, out)) {
                    diskCache.flush();
                    editor.commit();
                } else {
                    editor.abort();
                }
            }
        } catch (IOException e) {
            Log.w("ImageCache", "unable to save cache.", e);
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * キャッシュのクリア
     */
    public void clear() {
        memCache.evictAll();
        if (diskCache != null) {
            try {
                diskCache.delete();
            } catch (IOException e) {
                Log.w("ImageCache", "unable to clear cache.", e);
            }
        }
    }
}
