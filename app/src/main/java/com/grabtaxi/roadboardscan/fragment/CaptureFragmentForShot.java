package com.grabtaxi.roadboardscan.fragment;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.grabtaxi.roadboardscan.R;
import com.grabtaxi.roadboardscan.zxing.ViewfinderViewForShot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CaptureFragmentForShot extends BaseFragment implements
        SurfaceHolder.Callback
{
    private final String TAG = getClass().getSimpleName();
    private LayoutInflater inflater;
    private Camera camera;
    private ViewfinderViewForShot viewfinderView;
    private SurfaceView surfaceView;
    // 提示
    private TextView captureDesc;
    private Button captureShot;

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
        View view = inflater.inflate(R.layout.main_route_capture_frag_for_shot, container, false);
        captureDesc = (TextView) view.findViewById(R.id.capture_desc);
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
                        camera.takePicture(shutter, null, new MyPictureCallback());
                    }catch(Exception e){
                        e.printStackTrace();
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
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);//surfaceview不维护自己的缓冲区，等待屏幕渲染引擎将内容推送到用户面前
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
    public void surfaceCreated(SurfaceHolder holder)
    {
        try
        {
            camera = Camera.open(); // 打开摄像头
            try
            {
                camera.setPreviewDisplay(holder); // 设置用于显示拍照影像的SurfaceHolder对象
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
            camera.release(); // 释放照相机
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
            //这里可以做一些我们要做的变换。
            //重新开启Camera的预览功能
            try{
                Camera.Parameters params = camera.getParameters();
                params.setPictureFormat(PixelFormat.JPEG);//图片格式
                params.setPreviewSize(1280, 720);//图片大小
                params.setPictureSize(1280, 720);
                params.setJpegQuality(100);
                camera.setParameters(params);//将参数设置到我的camera
                camera.startPreview(); // 开始预览
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

    Camera.ShutterCallback shutter = new Camera.ShutterCallback() {

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
                saveToSDCard(data); // 保存图片到sd卡中
                camera.stopPreview();//关闭预览 处理数据
                camera.startPreview();//数据处理完后继续开始预览
                camera.autoFocus(null);
                Toast.makeText(getActivity(), "success",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检验是否有SD卡
     *
     * @true or false
     */
    public static boolean isHaveSDCard()
    {
        return Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState());
    }

    /**
     * 将拍下来的照片存放在SD卡中
     *
     * @param data
     * @throws IOException
     */
    public void saveToSDCard(byte[] data) throws IOException
    {
        Log.e("gjh", "" + data.length);
        //剪切为正方形
//        Bitmap b = byteToBitmap(data);
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.postRotate(90);
        Bitmap origin = BitmapFactory.decodeByteArray(data, 0, data.length);
        Bitmap b =  Bitmap.createBitmap(origin, 0, 0, origin.getWidth(),
                origin.getHeight(), matrix, true);
        //生成文件
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间
        String filename = format.format(date) + ".jpg";
        File fileFolder = new File(Environment.getExternalStorageDirectory() + File.separator + "roadboard" + File.separator);
        if (!fileFolder.exists())
        { // 如果目录不存在，则创建一个名为"finger"的目录
            fileFolder.mkdir();
        }
        File jpgFile = new File(fileFolder, filename);
        FileOutputStream outputStream = new FileOutputStream(jpgFile); // 文件输出流
        b.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        outputStream.flush();
        outputStream.close(); // 关闭输出流
        b.recycle();//回收bitmap空间
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
        super.onDestroy();
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
}
