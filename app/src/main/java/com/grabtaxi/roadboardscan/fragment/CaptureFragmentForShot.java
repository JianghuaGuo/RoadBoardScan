package com.grabtaxi.roadboardscan.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.grabtaxi.roadboardscan.R;
import com.grabtaxi.roadboardscan.zxing.BeepManager;
import com.grabtaxi.roadboardscan.zxing.CaptureActivityHandler;
import com.grabtaxi.roadboardscan.zxing.InactivityTimer;
import com.grabtaxi.roadboardscan.zxing.ViewfinderViewForShot;
import com.grabtaxi.roadboardscan.zxing.camera.CameraManager;
import com.grabtaxi.roadboardscan.zxing.result.ResultHandler;
import com.grabtaxi.roadboardscan.zxing.result.ResultHandlerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Cipher;

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

    private boolean hasSurface;
    private int cameraPosition = 1;//0代表前置摄像头，1代表后置摄像头

    /**
     * 活动监控器。如果手机没有连接电源线，那么当相机开启后如果一直处于不被使用状态则该服务会将当前fragment关闭。
     * 活动监控器全程监控扫描活跃状态，与CaptureFragment生命周期相同.每一次扫描过后都会重置该监控，即重新倒计时。
     */
    private InactivityTimer inactivityTimer;

    public ViewfinderViewForShot getViewfinderView()
    {
        return viewfinderView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        // 在扫描功能开启后，保持屏幕处于点亮状态
        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // 全屏扫描
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
                        camera.takePicture(null, null, new MyPictureCallback());//将拍摄到的照片给自定义的对象
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
                if (camera != null){
                    camera.autoFocus(null);
                }
                return false;
            }
        });
        viewfinderView = (ViewfinderViewForShot) view.findViewById(R.id.viewfinder_view);

        // 这里仅仅是对各个组件进行简单的创建动作，真正的初始化动作放在onResume中
        hasSurface = false;
        inactivityTimer = new InactivityTimer(getActivity());
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

        // 摄像头预览功能必须借助SurfaceView，因此也需要在一开始对其进行初始化
        // 如果需要了解SurfaceView的原理，参考:http://blog.csdn.net/luoshengyang/article/details/8661317
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);//surfaceview不维护自己的缓冲区，等待屏幕渲染引擎将内容推送到用户面前
        if (!hasSurface)
        {
            surfaceHolder.addCallback(this);
        }

        // 恢复活动监控器
        inactivityTimer.onResume();
    }

    @Override
    public void onPause()
    {
        Log.i(TAG, "onPause");

        // 暂停活动监控器
        inactivityTimer.onPause();

        if (!hasSurface)
        {
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        if (holder == null)
        {
            Log.e(TAG,
                    "*** WARNING *** surfaceCreated() gave us a null surface!");
        }

        if (!hasSurface) {
            hasSurface = true;
        }
        try
        {
            if (camera != null) {
                camera.stopPreview();
                camera.release();
            }
            camera = Camera.open(); // 打开摄像头
            camera.setPreviewDisplay(holder); // 设置用于显示拍照影像的SurfaceHolder对象
//            camera.setDisplayOrientation(getPreviewDegree(getActivity()));
            camera.startPreview(); // 开始预览
            camera.setDisplayOrientation(90);
            camera.autoFocus(null);
            drawViewfinder();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        hasSurface = false;
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

        //先停止Camera的预览
        try{
            camera.stopPreview();
        }catch(Exception e){
            e.printStackTrace();
        }

        //这里可以做一些我们要做的变换。

        //重新开启Camera的预览功能
        try{
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        }catch(Exception e){
            e.printStackTrace();
        }
        if (camera != null)
        {
            //设置参数，并拍照
            Camera.Parameters params = camera.getParameters();
            params.setPictureFormat(PixelFormat.JPEG);//图片格式
            params.setPreviewSize(1280, 720);//图片大小
            params.setPictureSize(1280, 720);
            params.setJpegQuality(100);
            camera.setParameters(params);//将参数设置到我的camera
        }
    }

    // 提供一个静态方法，用于根据手机方向获得相机预览画面旋转的角度
    public static int getPreviewDegree(Activity activity)
    {
        // 获得手机的方向
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degree = 0;
        // 根据手机的方向计算相机预览画面应该选择的角度
        switch (rotation)
        {
            case Surface.ROTATION_0:
                degree = 90;
                break;
            case Surface.ROTATION_90:
                degree = 0;
                break;
            case Surface.ROTATION_180:
                degree = 270;
                break;
            case Surface.ROTATION_270:
                degree = 180;
                break;
        }
        return degree;
    }

    public void drawViewfinder()
    {
        viewfinderView.drawViewfinder();
    }

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
                Toast.makeText(getActivity(), "success",
                        Toast.LENGTH_SHORT).show();
                camera.startPreview(); // 拍完照后，重新开始预览
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
        outputStream.write(data); // 写入sd卡中
        outputStream.close(); // 关闭输出流
        camera.stopPreview();//关闭预览 处理数据
        camera.startPreview();//数据处理完后继续开始预览
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
        // 停止活动监控器
        inactivityTimer.shutdown();
        // 	退出全屏模式
        WindowManager.LayoutParams attrs = getActivity().getWindow().getAttributes();
        attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getActivity().getWindow().setAttributes(attrs);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        super.onDestroy();
    }

    @Override
    public void onDestroyView()
    {
        Log.i(TAG, "onDestroyView");
        super.onDestroyView();
    }
}
