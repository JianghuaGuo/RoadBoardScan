/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.grabtaxi.roadboardscan.zxing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.grabtaxi.roadboardscan.R;
import com.grabtaxi.roadboardscan.common.GlobalVariables;
import com.grabtaxi.roadboardscan.fragment.CaptureFragment;
import com.grabtaxi.roadboardscan.zxing.camera.CameraManager;

public final class DecodeHandler extends Handler {

    private static final String TAG = DecodeHandler.class.getSimpleName();

    private final CaptureFragment mFragment;

    private boolean running = true;

    DecodeHandler(CaptureFragment fragment) {
        this.mFragment = fragment;
    }

    @Override
    public void handleMessage(Message message) {
        if (!running) {
            return;
        }
        switch (message.what) {
            case R.id.decode:
                decode((byte[]) message.obj, message.arg1, message.arg2);
                break;
            case R.id.quit:
                running = false;
                Looper.myLooper().quit();
                break;
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it
     * took. For efficiency, reuse the same reader objects from one decode to
     * the next.
     * 
     * @param data The YUV preview frame.
     * @param width The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private void decode(byte[] data, int width, int height) {
    	Log.i(TAG, "decode:" + data.length + " width:" + width + ", height:" + height);
        long start = System.currentTimeMillis();

        // 将YUV格式的数据转换成位图
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);
        byte[] rgb = baos.toByteArray();

        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.postRotate(90);
        Bitmap origin = BitmapFactory.decodeByteArray(rgb, 0, rgb.length);
        Bitmap b =  Bitmap.createBitmap(origin, 0, 0, origin.getWidth(),
                origin.getHeight(), matrix, true);

        // 去除扫描窗口部分的位图信息
        Rect rectInPreview = mFragment.getCameraManager().getFramingRectInPreview();
        Bitmap contentBmp = Bitmap.createBitmap(b, rectInPreview.left, rectInPreview.top, rectInPreview.width(), rectInPreview.height());
        // 逐个像素判断，看是否符合路牌的颜色范围
        boolean isValidRoadIndicator = isValidIndicator(contentBmp);
        Handler handler = mFragment.getHandler();
        if (isValidRoadIndicator) {
            long end = System.currentTimeMillis();
            Log.i(TAG, "Found valid road indicarot in " + (end - start) + " ms");
            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded);
                Bundle bundle = new Bundle();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                contentBmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
                bundle.putByteArray(DecodeThread.CONTENT_BITMAP, out.toByteArray());
                message.setData(bundle);
                message.sendToTarget();
            }
        } else {
            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_failed);
                message.sendToTarget();
            }
        }
        if (contentBmp != null){
            contentBmp.recycle();
        }
    }

    private boolean isValidIndicator(Bitmap bmp){
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int valid = 0;
        for (int i = 0 ; i < w; i ++){
            for (int j = 0 ; j < h ; j ++){
                int color = bmp.getPixel(i, j);
                int[] argb = intToByteArray(color);
                if ((10 < argb[1] && argb[1] < 50) && (150 < argb[2] && argb[2] < 250) && (100 < argb[3] && argb[3] <200))
                {
                    valid ++;
                } else if ((200 < argb[1] && argb[1] < 255) && (200 < argb[2] && argb[2] < 255) && (200 < argb[3] && argb[3] < 255)){
                    valid ++;
                }
            }
        }
        int percent = (valid * 100) / (w * h);
        Log.e(TAG, "w:" + w + " h:" + h + " valid:" + valid + " percent:" + percent);
        if (percent > 50){
            return true;
        }else{
            return false;
        }
    }

    public static int[] intToByteArray(int i) {
        int[] result = new int[4];
        //由高位到低位
        result[0] = ((i >> 24) & 0xFF);
        result[1] = ((i >> 16) & 0xFF);
        result[2] = ((i >> 8) & 0xFF);
        result[3] = (i & 0xFF);
        return result;
    }

}
