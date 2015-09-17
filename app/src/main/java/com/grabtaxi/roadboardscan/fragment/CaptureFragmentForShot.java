package com.grabtaxi.roadboardscan.fragment;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.grabtaxi.roadboardscan.R;
import com.grabtaxi.roadboardscan.zxing.ViewfinderViewForShot;
import com.grabtaxi.roadboardscan.zxing.camera.CameraConfigurationManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import network.HttpResult;
import network.HttpResultListener;
import network.UploadImageRequestTask;

public class CaptureFragmentForShot extends BaseFragment implements
        SurfaceHolder.Callback
{
    private final String TAG = getClass().getSimpleName();
    private LayoutInflater inflater;
    private Camera camera;
    private CameraConfigurationManager cfm;
    private ViewfinderViewForShot viewfinderView;
    private SurfaceView surfaceView;
    // 提示
    private TextView captureDesc;
    private Button captureShot;
    private TextView captureResult;
    private boolean isProcessing = false;

    public ViewfinderViewForShot getViewfinderView()
    {
        return viewfinderView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        // 保持屏幕处于点亮状态
        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        Log.i(TAG, "onCreateView");
        this.inflater = inflater;
        View view = inflater.inflate(R.layout.capture_frag_for_shot, container, false);
        captureDesc = (TextView) view.findViewById(R.id.capture_desc);
        captureResult = (TextView) view.findViewById(R.id.capture_result);
        captureShot = (Button) view.findViewById(R.id.capture_shot);
        captureShot.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // 拍照
                if (camera != null) {
                    try
                    {
                        if (!isProcessing)
                        {
                            isProcessing = true;
                            captureShot.setEnabled(false);
                            camera.takePicture(shutter, null, new MyPictureCallback());
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                        isProcessing = false;
                        captureShot.setEnabled(true);
                    }
                }
            }
        });
        surfaceView = (SurfaceView) view.findViewById(R.id.preview_view);
        surfaceView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (camera != null)
                {
                    camera.autoFocus(null);
                }
                return false;
            }
        });
        viewfinderView = (ViewfinderViewForShot) view.findViewById(R.id.viewfinder_view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        Log.i(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        Log.i(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.i(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onAttach(Activity activity)
    {
        Log.i(TAG, "onAttach");
        super.onAttach(activity);
    }

    @Override
    public void onStart()
    {
        Log.i(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onResume()
    {
        Log.i(TAG, "onResume");
        super.onResume();
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        //surfaceview不维护自己的缓冲区，等待屏幕渲染引擎将内容推送到用户面前
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(this);

    }

    @Override
    public void onPause()
    {
        Log.i(TAG, "onPause");

        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.removeCallback(this);
        super.onPause();
    }


    @Override
    public void onStop()
    {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDetach()
    {
        Log.i(TAG, "onDetach");
        super.onDetach();
    }

    @Override
    public void onDestroy()
    {
        Log.i(TAG, "onDestroy");
        // 	退出全屏模式
        WindowManager.LayoutParams attrs = getActivity().getWindow().getAttributes();
        attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getActivity().getWindow().setAttributes(attrs);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        super.onDestroy();
    }

    @Override
    public void onDestroyView()
    {
        Log.i(TAG, "onDestroyView");
        super.onDestroyView();
    }

	@Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        try
        {
            int cameraCount= Camera.getNumberOfCameras();
            Log.i(TAG,"cameraCount:" + cameraCount);
            // 打开摄像头
            camera = Camera.open();
            try
            {
                // 设置用于显示拍照影像的SurfaceHolder对象
                camera.setPreviewDisplay(holder);
                cfm = new CameraConfigurationManager();
                cfm.initFromCameraParameters(camera);
                viewfinderView.setCameraConfigurationManager(cfm);
            }catch (IOException e){
                camera.release();
                camera = null;
            }
            camera.setDisplayOrientation(90);
            drawViewfinder();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        if (camera != null)
        {
            //当surfaceview关闭时，关闭预览并释放资源
            camera.stopPreview();
            // 释放照相机
            camera.release();
            camera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height)
    {
        //根本没有可处理的SurfaceView
        if (holder.getSurface() == null){
            return ;
        }

        if(camera != null)
        {
            try{
                Camera.Parameters params = camera.getParameters();
                Log.i(TAG, "SupportedPictureFormats:" + params.getSupportedPictureFormats());
                List rawSupportedPictureSizes = params.getSupportedPictureSizes();
                StringBuilder strPictureSize = new StringBuilder();
                Iterator pictureSizeIter = rawSupportedPictureSizes.iterator();

                while(pictureSizeIter.hasNext()) {
                    Camera.Size it = (Camera.Size)pictureSizeIter.next();
                    strPictureSize.append(it.width).append('x').append(it.height).append(' ');
                }
                Log.i(TAG, "SupportedPictureSizes:" + strPictureSize);
                Log.i(TAG, "SupportedPreviewFormats:" + params.getSupportedPreviewFormats());
                List rawSupportedPreviewSizes = params.getSupportedPreviewSizes();
                StringBuilder strPreviewSize = new StringBuilder();
                Iterator previewSizeIter = rawSupportedPreviewSizes.iterator();

                while(previewSizeIter.hasNext()) {
                    Camera.Size it = (Camera.Size)previewSizeIter.next();
                    strPreviewSize.append(it.width).append('x').append(it.height).append(' ');
                }
                Log.i(TAG, "SupportedPreviewSizes:" + strPreviewSize);
                params.setPictureFormat(PixelFormat.JPEG);
                params.setPreviewSize(cfm.getCameraResolutionForOneShot().x, cfm.getCameraResolutionForOneShot().y);
                params.setPictureSize(cfm.getCameraResolutionForOneShot().x, cfm.getCameraResolutionForOneShot().y);
                params.setJpegQuality(100);
                camera.setParameters(params);
                // 开始预览
                camera.startPreview();
                // 开始对焦
                camera.autoFocus(null);
            }catch(Exception e){
                e.printStackTrace();
            }

        }
    }

    public void drawViewfinder()
    {
        viewfinderView.drawViewfinder();
    }

    private Camera.ShutterCallback shutter = new Camera.ShutterCallback() {

        @Override
        public void onShutter() {
                // 发出提示用户的声音
//                new ToneGenerator(AudioManager.STREAM_SYSTEM,
//                        ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_PROP_BEEP);
                Vibrator vibrator = (Vibrator) getActivity()
                        .getSystemService(Service.VIBRATOR_SERVICE);
                vibrator.vibrate(100);
        }
    };

    /**
     * 重构照相类
     *
     * @author
     */
    private final class MyPictureCallback implements Camera.PictureCallback
    {

        @Override
        public void onPictureTaken(byte[] data, Camera camera)
        {
            try
            {
                captureResult.setText("Automatically Recognizing, Please wait...");
                // 保存图片到sd卡中
                new FileThread(data).start();
                camera.stopPreview();//关闭预览 处理数据
                camera.startPreview();//数据处理完后继续开始预览
                camera.autoFocus(null);
            } catch (Exception e)
            {
                e.printStackTrace();
                isProcessing = false;
                captureShot.setEnabled(true);
            }
        }
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            if (msg.what == 100)
            {
                String obj = (String)msg.obj;
                new UploadImageRequestTask(getActivity(), new HttpResultListener()
                {
                    @Override
                    public void onSuccess(HttpResult result)
                    {
                        isProcessing = false;
                        switch (result.getState())
                        {
                            case HttpResult.INTERNET_SUCCESS:
                                Toast.makeText(getActivity(), "result:" + result.getResult().substring(3),
                                        Toast.LENGTH_SHORT).show();
                                String text = result.getResult().substring(3);
                                captureResult.setText("result:" + text);

                                if (TextUtils.isEmpty(text))
                                {
                                    Toast.makeText(getActivity(), "Recognized nothing, please try again.", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case HttpResult.INTERNET_EXCEPTION:
                                // 网络异常
                                Toast.makeText(getActivity(), result.getErrorMsg(),
                                        Toast.LENGTH_SHORT).show();
                                captureResult.setText(result.getErrorMsg());
                                break;
                            default:
                                break;
                        }
                    }

                    @Override
                    public void onFailed(HttpResult result)
                    {
                        isProcessing = false;
                        captureShot.setEnabled(true);
                        Toast.makeText(getActivity(), "Recognized failed, please try again.",
                                Toast.LENGTH_SHORT).show();
                        captureResult.setText("Recognized failed, please try again.");
                    }

                    @Override
                    public void updateProgress(int progress)
                    {

                    }
                }).execute(obj);
            } else if (msg.what == -100){
                Toast.makeText(getActivity(),"Failed to save image file.",Toast.LENGTH_SHORT).show();
                isProcessing = false;
            }
        }
    };

    private class FileThread extends Thread
    {
        private byte[] data = null;
        private String topFilePath;

        public FileThread(byte[] dt)
        {
            this.data = dt;
        }

        @Override
        public void run()
        {
            super.run();
            try
            {
                if(isHaveSDCard())
                {
                    saveToSDCard(data);
                    Message msg = new Message();
                    msg.what = 100;
                    msg.obj = topFilePath;
                    handler.sendMessage(msg);
                } else {
                    handler.sendEmptyMessage(-100);
                }
            } catch (Exception e)
            {
                e.printStackTrace();
                handler.sendEmptyMessage(-100);
            }

        }

        /**
         * 将拍下来的照片存放在SD卡中
         *
         * @param data
         * @throws IOException
         */
        private void saveToSDCard(byte[] data) throws IOException
        {
            Log.i(TAG, "" + data.length);
            Matrix matrix = new Matrix();
            matrix.reset();
            matrix.postRotate(90);
            Bitmap origin = BitmapFactory.decodeByteArray(data, 0, data.length);
            Log.i(TAG, "before rotate origin:" + origin.getWidth() + "x" + origin.getHeight());
            Bitmap b = Bitmap.createBitmap(origin, 0, 0, origin.getWidth(),
                    origin.getHeight(), matrix, true);
            origin.recycle();
            Log.i(TAG, "after rotate b:" + b.getWidth() + "x" + b.getHeight());
            File fileFolder = new File(Environment.getExternalStorageDirectory() + File.separator + "roadboard" + File.separator);
            if (!fileFolder.exists())
            {
                fileFolder.mkdir();
            }

            Date date = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

            String fileName = format.format(date) + ".jpg";
            File file = new File(fileFolder, fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            b.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            Rect topRectInPreview = cfm.getFramingRectInPreviewForOneShot(cfm.getTopFramingRectForOneShot());
            Bitmap topBmp = Bitmap.createBitmap(b, topRectInPreview.left, topRectInPreview.top, topRectInPreview.width(), topRectInPreview.height());
            isValidIndicator(topBmp);
            String topFileName = format.format(date) + "top.jpg";
            File topFile = new File(fileFolder, topFileName);
            topFilePath = topFile.getAbsolutePath();
            FileOutputStream topOutputStream = new FileOutputStream(topFile);
            topBmp.compress(Bitmap.CompressFormat.JPEG, 100, topOutputStream);
            topOutputStream.flush();
            topOutputStream.close();
            topBmp.recycle();

            b.recycle();
        }

        /**
         * 检验是否有SD卡
         *
         * @true or false
         */
        private boolean isHaveSDCard()
        {
            return Environment.MEDIA_MOUNTED.equals(Environment
                    .getExternalStorageState());
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
        Log.i(TAG, "w:" + w + " h:" + h + " valid:" + valid + " percent:" + percent);
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
