package com.grabtaxi.roadboardscan.fragment;

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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
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

import com.grabtaxi.roadboardscan.R;
import com.grabtaxi.roadboardscan.zxing.CaptureActivityHandler;
import com.grabtaxi.roadboardscan.zxing.ViewfinderView;
import com.grabtaxi.roadboardscan.zxing.camera.CameraManager;

import network.HttpResult;
import network.HttpResultListener;
import network.UploadImageRequestTask;

public class CaptureFragment extends BaseFragment  implements
SurfaceHolder.Callback {
	private final String TAG = getClass().getSimpleName();
	private LayoutInflater inflater;
	private CameraManager cameraManager;

	private CaptureActivityHandler handler;

	private ViewfinderView viewfinderView;
	private SurfaceView surfaceView;
	private TextView captureFlash;
	// 扫描提示，例如"请将条码置于取景框内扫描"之类的提示
	private TextView captureDesc;
	private TextView captureResult;
	// 显示扫描截图
	private ImageView indicatorImage;

	private boolean hasSurface;

	/**
	 * 闪光灯调节器。自动检测环境光线强弱并决定是否开启闪光灯
	 */
//	private AmbientLightManager ambientLightManager;

	public ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	public CameraManager getCameraManager() {
		return cameraManager;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
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
			Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");
		this.inflater = inflater;
		View view = inflater.inflate(R.layout.main_route_capture_frag, container,false);
		captureFlash= (TextView) view.findViewById(R.id.capture_flash);
		captureDesc = (TextView) view.findViewById(R.id.capture_desc);
		captureResult = (TextView) view.findViewById(R.id.capture_result);
		indicatorImage = (ImageView) view.findViewById(R.id.capture_indicator_image);
		surfaceView = (SurfaceView) view.findViewById(R.id.preview_view);
		
		viewfinderView = (ViewfinderView) view.findViewById(R.id.viewfinder_view);
		captureFlash.setOnClickListener(new OnClickListener(){
			private boolean flashOn = false;
			@Override
			public void onClick(View v) {
				if (flashOn){
					cameraManager.setTorch(false);
					flashOn = false;
				}else{
					cameraManager.setTorch(true);
					flashOn = true;
				}
			}
			
		});
		
		// 这里仅仅是对各个组件进行简单的创建动作，真正的初始化动作放在onResume中
		hasSurface = false;
//		ambientLightManager = new AmbientLightManager(getActivity());
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Log.i(TAG, "onViewCreated");
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onAttach(Activity activity) {
		Log.i(TAG, "onAttach");
		super.onAttach(activity);
	}

	@Override
	public void onStart() {
		Log.i(TAG, "onStart");
		super.onStart();
	}

	@Override
	public void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();
		// CameraManager must be initialized here, not in onCreate(). This is
		// necessary because we don't
		// want to open the camera driver and measure the screen size if we're
		// going to show the help on
		// first launch. That led to bugs where the scanning rectangle was the
		// wrong size and partially
		// off screen.
		/**
		 * 上面这段话的意思是说，相机初始化的动作需要开启相机并测量屏幕大小，这些操作
		 * 不建议放到onCreate中，因为如果在onCreate中加上首次启动展示帮助信息的代码的 话，会导致扫描窗口的尺寸计算有误的bug
		 */
		cameraManager = new CameraManager(getActivity());

		viewfinderView.setCameraManager(cameraManager);

		handler = null;

		// 重置状态窗口，扫描窗口和结果窗口的状态
		resetStatusView();

		// 摄像头预览功能必须借助SurfaceView，因此也需要在一开始对其进行初始化
		// 如果需要了解SurfaceView的原理，参考:http://blog.csdn.net/luoshengyang/article/details/8661317
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			// 如果SurfaceView已经渲染完毕，会回调surfaceCreated，在surfaceCreated中调用initCamera()
			surfaceHolder.addCallback(this);
		}

		// 启动闪光灯调节器
//		ambientLightManager.start(cameraManager);
	}

	@Override
	public void onPause() {
		Log.i(TAG, "onPause");
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}

		// 停止闪光灯控制器
//		ambientLightManager.stop();
		// 关闭摄像头
		cameraManager.closeDriver();
		if (!hasSurface) {
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			surfaceHolder.removeCallback(this);
		}
		super.onPause();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (holder == null) {
			Log.e(TAG,
					"*** WARNING *** surfaceCreated() gave us a null surface!");
		}
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}


	// Put up our own UI for how to handle the decoded contents.
	/**
	 * 该方法会将最终处理的结果展示到result_view上。并且如果选择了"检索更多信息"则会内部对结果进行进一步的查询
	 * 
	 * @param rawResult
	 * @param resultHandler
	 * @param barcode
	 */
	public void handleDecodeSuccess(Bitmap content) {
		if (content != null) {
			indicatorImage.setVisibility(View.VISIBLE);
			indicatorImage.setImageBitmap(content);
			captureResult.setText("Found valid indicator, recognizing, Please wait......");
			new FileThread(content).start();
		}
	}

	private Handler fileHandler = new Handler(){
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
						switch (result.getState())
						{
							case HttpResult.INTERNET_SUCCESS:
								Toast.makeText(getActivity(), "result:" + result.getResult().substring(3),
										Toast.LENGTH_SHORT).show();
								String text = result.getResult().substring(3);
								captureResult.setText("result:" + text);

								if (TextUtils.isEmpty(text))
								{
									Toast.makeText(getActivity(), "Recognized nothing, waiting for next.", Toast.LENGTH_SHORT).show();
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
						Toast.makeText(getActivity(), "Recognized failed, waiting for next.",
								Toast.LENGTH_SHORT).show();
						captureResult.setText("Recognized failed, waiting for next.");
						indicatorImage.setVisibility(View.GONE);
					}

					@Override
					public void updateProgress(int progress)
					{

					}
				}).execute(obj);
			} else if (msg.what == -100){
				Toast.makeText(getActivity(),"Failed to save image file.",Toast.LENGTH_SHORT).show();
				indicatorImage.setVisibility(View.GONE);
			}
		}
	};

	private class FileThread extends Thread
	{
		private Bitmap bmp = null;
		private String filePath;

		public FileThread(Bitmap bmp)
		{
			this.bmp = bmp;
		}

		@Override
		public void run()
		{
			super.run();
			try
			{
				if(isHaveSDCard() && bmp != null)
				{
					saveToSDCard(bmp);
					bmp.recycle();
					Message msg = new Message();
					msg.what = 100;
					msg.obj = filePath;
					fileHandler.sendMessage(msg);
				} else {
					fileHandler.sendEmptyMessage(-100);
				}
			} catch (Exception e)
			{
				e.printStackTrace();
				fileHandler.sendEmptyMessage(-100);
			}

		}


		private void saveToSDCard(Bitmap bmp)
		{
			//生成文件
			Date date = new Date();
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间
			String fileName = format.format(date) + ".jpg";
			File fileFolder = new File(Environment.getExternalStorageDirectory() + File.separator + "roadboard" + File.separator);
			if (!fileFolder.exists())
			{
				fileFolder.mkdir();
			}
			File file = new File(fileFolder, fileName);
			FileOutputStream topOutputStream = null;
			try
			{
				topOutputStream = new FileOutputStream(file); // 文件输出流
				bmp.compress(Bitmap.CompressFormat.JPEG, 100, topOutputStream);
				topOutputStream.flush();
				filePath = file.getAbsolutePath();
			}catch (Exception e){
				e.printStackTrace();
			}finally
			{
				if (topOutputStream != null){
					try
					{
						topOutputStream.close();
					}catch (Exception e)
					{
					}
				}
			}
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


	
	private void initCamera(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == null) {
			throw new IllegalStateException("No SurfaceHolder provided");
		}
		if (cameraManager.isOpen()) {
			Log.w(TAG,
					"initCamera() while already open -- late SurfaceView callback?");
			return;
		}
		try {
			cameraManager.openDriver(surfaceHolder);
			// Creating the handler starts the preview, which can also throw a
			// RuntimeException.
			if (handler == null) {
				handler = new CaptureActivityHandler(this, cameraManager);
			}
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
			displayFrameworkBugMessageAndExit();
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			Log.w(TAG, "Unexpected error initializing camera", e);
			displayFrameworkBugMessageAndExit();
		}
	}

	private void displayFrameworkBugMessageAndExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getString(R.string.app_name));
		builder.setMessage(getString(R.string.msg_camera_framework_bug));
		builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				
			}
		});
		builder.show();
	}

	/**
	 * 在经过一段延迟后重置相机以进行下一次扫描。 成功扫描过后可调用此方法立刻准备进行下次扫描
	 * 
	 * @param delayMS
	 */
	public void restartPreviewAfterDelay(long delayMS) {
		new Handler().postDelayed(new Runnable(){
			@Override
			public void run() {
				if (handler != null) {
					handler.sendEmptyMessage(R.id.restart_preview);
				}
				resetStatusView();
			}
		}, delayMS);
	}

	/**
	 * 展示状态视图和扫描窗口，隐藏结果视图
	 */
	private void resetStatusView() {
		indicatorImage.setVisibility(View.INVISIBLE);
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

	@Override
	public void onStop() {
		Log.i(TAG, "onStop");
		super.onStop();
	}

	@Override
	public void onDetach() {
		Log.i(TAG, "onDetach");
		super.onDetach();
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();
		// 	退出全屏模式
		WindowManager.LayoutParams attrs = getActivity().getWindow().getAttributes();
		attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getActivity().getWindow().setAttributes(attrs);
		getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		Log.i(TAG, "onDestroyView");
		super.onDestroyView();
	}
}
