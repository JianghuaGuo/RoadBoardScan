package com.grabtaxi.roadboardscan.zxing;

import java.util.concurrent.CountDownLatch;

import android.os.Handler;
import android.os.Looper;

import com.grabtaxi.roadboardscan.fragment.CaptureFragment;

/**
 * This thread does all the heavy lifting of decoding the images.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class DecodeThread extends Thread {

    public static final String CONTENT_BITMAP = "content_bitmap";

    private final CaptureFragment mFragment;

    private Handler handler;

    private final CountDownLatch handlerInitLatch;

    DecodeThread(CaptureFragment fragment) {

        this.mFragment = fragment;
        handlerInitLatch = new CountDownLatch(1);
    }

    Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new DecodeHandler(mFragment);
        handlerInitLatch.countDown();
        Looper.loop();
    }

}
