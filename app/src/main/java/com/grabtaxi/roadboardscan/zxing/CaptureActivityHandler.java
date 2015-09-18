package com.grabtaxi.roadboardscan.zxing;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.grabtaxi.roadboardscan.R;
import com.grabtaxi.roadboardscan.fragment.CaptureFragment;
import com.grabtaxi.roadboardscan.zxing.camera.CameraManager;

/**
 * This class handles all the messaging which comprises the state machine for
 * capture.
 * 
 * 除CaptureActivity之外的又一个核心类，继承自Handler。接收开始扫描、扫描成功、失败等消息并进行处理。
 * 一个针对扫描任务的Handler，可接收的message有启动扫描（restart_preview）、扫描成功（decode_succeeded）、扫描失败（decode_failed）等等
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CaptureActivityHandler extends Handler {

    private static final String TAG = CaptureActivityHandler.class.getSimpleName();

    private final CaptureFragment mFragment;

    /**
     * 真正负责扫描任务的核心线程
     */
    private final DecodeThread decodeThread;

    private State state;

    private final CameraManager cameraManager;

    /**
     * 当前扫描的状态
     */
    private enum State {
        /**
         * 预览
         */
        PREVIEW, 
        /**
         * 扫描成功
         */
        SUCCESS, 
        /**
         * 结束扫描
         */
        DONE
    }

    public CaptureActivityHandler(CaptureFragment fragment, CameraManager cameraManager) {
        this.mFragment = fragment;
        
        // 启动扫描线程
        decodeThread = new DecodeThread(mFragment);
        decodeThread.start();
        
        state = State.SUCCESS;

        // Start ourselves capturing previews and decoding.
        this.cameraManager = cameraManager;
        
        // 开启相机预览界面
        cameraManager.startPreview();
        
        // 将preview回调函数与decodeHandler绑定、调用viewfinderView
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case R.id.restart_preview: // 准备进行下一次扫描
                Log.i(TAG, "Got restart preview message");
                restartPreviewAndDecode();
                break;
            case R.id.decode_succeeded:
                Log.i(TAG, "Got decode succeeded message");
                state = State.SUCCESS;
                Bundle bundle = message.getData();
                Bitmap contentBmp = null;
                if (bundle != null) {
                    byte[] compressedBitmap = bundle.getByteArray(DecodeThread.CONTENT_BITMAP);
                    if (compressedBitmap != null) {
                        contentBmp = BitmapFactory.decodeByteArray(compressedBitmap, 0,
                                compressedBitmap.length, null);
                        mFragment.handleDecodeSuccess(contentBmp);
                    }
                }
                break;
            case R.id.decode_failed:
            	Log.i(TAG, "Got decode failed message");
                // We're decoding as fast as possible, so when one decode fails, start another.
                state = State.PREVIEW;
                cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
                break;
        }
    }

    public void quitSynchronously() {
        state = State.DONE;
        cameraManager.stopPreview();
        Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
        quit.sendToTarget();
        
        try {
            // Wait at most half a second; should be enough time, and onPause()
            // will timeout quickly
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded);
        removeMessages(R.id.decode_failed);
    }

    /**
     * 完成一次扫描后，只需要再调用此方法即可
     */
    private void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            
            // 向decodeThread绑定的handler（DecodeHandler)发送解码消息
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
            mFragment.drawViewfinder();
        }
    }
}
