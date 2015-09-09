package com.grabtaxi.roadboardscan.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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
import com.grabtaxi.roadboardscan.zxing.ViewfinderView;
import com.grabtaxi.roadboardscan.zxing.camera.CameraManager;
import com.grabtaxi.roadboardscan.zxing.result.ResultHandler;
import com.grabtaxi.roadboardscan.zxing.result.ResultHandlerFactory;

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
    private Camera.Parameters parameters = null;
    private ViewfinderView viewfinderView;
    private SurfaceView surfaceView;
    // 提示
    private TextView captureDesc;
    private TextView captureShot;

    private boolean hasSurface;

    /**
     * 活动监控器。如果手机没有连接电源线，那么当相机开启后如果一直处于不被使用状态则该服务会将当前fragment关闭。
     * 活动监控器全程监控扫描活跃状态，与CaptureFragment生命周期相同.每一次扫描过后都会重置该监控，即重新倒计时。
     */
    private InactivityTimer inactivityTimer;

    /**
     * 闪光灯调节器。自动检测环境光线强弱并决定是否开启闪光灯
     */
//	private AmbientLightManager ambientLightManager;
    public ViewfinderView getViewfinderView()
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
        captureShot = (TextView) view.findViewById(R.id.capture_shot);
        captureShot.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // 拍照
                if (camera != null) {
                    camera.takePicture(null, null, new MyPictureCallback());
                }
            }
        });
        surfaceView = (SurfaceView) view.findViewById(R.id.preview_view);

        viewfinderView = (ViewfinderView) view.findViewById(R.id.viewfinder_view);

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
            camera = Camera.open(); // 打开摄像头
            camera.setPreviewDisplay(holder); // 设置用于显示拍照影像的SurfaceHolder对象
            camera.setDisplayOrientation(getPreviewDegree(getActivity()));
            camera.startPreview(); // 开始预览
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
            camera.release(); // 释放照相机
            camera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height)
    {
        parameters = camera.getParameters(); // 获取各项参数
        parameters.setPictureFormat(PixelFormat.JPEG); // 设置图片格式
        parameters.setPreviewSize(width, height); // 设置预览大小
        parameters.setPreviewFrameRate(5);  //设置每秒显示4帧
        parameters.setPictureSize(width, height); // 设置保存的图片尺寸
        parameters.setJpegQuality(80); // 设置照片质量
        //  camera.setParameters(parameters);

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
        //剪切为正方形
        Bitmap b = byteToBitmap(data);
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
    }

    /**
     * 把图片byte流编程bitmap
     *
     * @param data
     * @return
     */
    private Bitmap byteToBitmap(byte[] data)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        int i = 0;
        while (true)
        {
            if ((options.outWidth >> i <= 1000)
                    && (options.outHeight >> i <= 1000))
            {
                options.inSampleSize = (int) Math.pow(2.0D, i);
                options.inJustDecodeBounds = false;
                b = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                break;
            }
            i += 1;
        }
        return b;

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
