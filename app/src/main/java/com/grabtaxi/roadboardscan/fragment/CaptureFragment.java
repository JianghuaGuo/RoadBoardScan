package com.grabtaxi.roadboardscan.fragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.grabtaxi.roadboardscan.R;
import com.grabtaxi.roadboardscan.zxing.BeepManager;
import com.grabtaxi.roadboardscan.zxing.CaptureActivityHandler;
import com.grabtaxi.roadboardscan.zxing.ViewfinderView;
import com.grabtaxi.roadboardscan.zxing.camera.CameraManager;
import com.grabtaxi.roadboardscan.zxing.result.ResultHandler;
import com.grabtaxi.roadboardscan.zxing.result.ResultHandlerFactory;

public class CaptureFragment extends BaseFragment  implements
SurfaceHolder.Callback {
	private final String TAG = getClass().getSimpleName();
	private LayoutInflater inflater;
	private CameraManager cameraManager;

	private CaptureActivityHandler handler;

	private Result savedResultToShow;

	private ViewfinderView viewfinderView;
	private SurfaceView surfaceView;
	private TextView captureFlash;
	// 扫描提示，例如"请将条码置于取景框内扫描"之类的提示
	private TextView captureDesc;
	// 扫描状态
	private TextView statusText;
	private ImageView statusImage;
	private TextView notFound;
	// 显示扫描截图
	private ImageView barCodeImage;
	// 扫描中
	private ProgressBar progress;
	// 解析结果
	private LinearLayout captureResultInfo;
	private TextView captureResultName;
	private TextView captureResultSchedule;
	private TextView captureResultDate;

	/**
	 * 扫描结果展示窗口
	 */
	private View resultView;

	private boolean hasSurface;

	/**
	 * 声音震动管理器。如果扫描成功后可以播放一段音频，也可以震动提醒，可以通过配置来决定扫描成功后的行为。
	 */
	private BeepManager beepManager;

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
		statusText = (TextView) view.findViewById(R.id.capture_status_text);
		statusImage = (ImageView) view.findViewById(R.id.capture_status_img);
		notFound = (TextView) view.findViewById(R.id.capture_not_found);
		barCodeImage = (ImageView) view.findViewById(R.id.capture_barcode_image);
		progress = (ProgressBar) view.findViewById(R.id.capture_progress);
		captureResultInfo = (LinearLayout) view.findViewById(R.id.capture_result_info);
		captureResultName = (TextView) view.findViewById(R.id.capture_result_passenger_name);
		captureResultSchedule = (TextView) view.findViewById(R.id.capture_result_schedule);
		captureResultDate = (TextView) view.findViewById(R.id.capture_result_date);
		surfaceView = (SurfaceView) view.findViewById(R.id.preview_view);
		
		viewfinderView = (ViewfinderView) view.findViewById(R.id.viewfinder_view);
		resultView = view.findViewById(R.id.result_view);
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
		beepManager = new BeepManager(getActivity());
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

		// 加载声音配置，其实在BeemManager的构造器中也会调用该方法，即在onCreate的时候会调用一次
		beepManager.updatePrefs();

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
		beepManager.close();
		// 关闭摄像头
		cameraManager.closeDriver();
		if (!hasSurface) {
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			surfaceHolder.removeCallback(this);
		}
		super.onPause();
	}

	/**
	 * 向CaptureActivityHandler中发送消息，并展示扫描到的图像
	 * 
	 * @param bitmap
	 * @param result
	 */
	private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
		// Bitmap isn't used yet -- will be used soon
		if (handler == null) {
			savedResultToShow = result;
		} else {
			if (result != null) {
				savedResultToShow = result;
			}
			if (savedResultToShow != null) {
				Message message = Message.obtain(handler,
						R.id.decode_succeeded, savedResultToShow);
				handler.sendMessage(message);
			}
			savedResultToShow = null;
		}
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

	/**
	 * A valid barcode has been found, so give an indication of success and show
	 * the results.
	 * 
	 * @param rawResult
	 *            The contents of the barcode.
	 * @param scaleFactor
	 *            amount by which thumbnail was scaled
	 * @param barcode
	 *            A greyscale bitmap of the camera data which was decoded.
	 */
	public void handleDecodeSuccess(Result rawResult, Bitmap barcode, float scaleFactor) {
		Log.i(TAG, "handleDecodeSuccess");
		ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(
				getActivity(), rawResult);

		drawResultPoints(barcode, scaleFactor, rawResult);
		handleDecodeInternally(rawResult, resultHandler, barcode);
	}

	/**
	 * Superimpose a line for 1D or dots for 2D to highlight the key features of
	 * the barcode.
	 * 
	 * @param barcode
	 *            A bitmap of the captured image.
	 * @param scaleFactor
	 *            amount by which thumbnail was scaled
	 * @param rawResult
	 *            The decoded results which contains the points to draw.
	 */
	private void drawResultPoints(Bitmap barcode, float scaleFactor,
			Result rawResult) {
		ResultPoint[] points = rawResult.getResultPoints();
		if (points != null && points.length > 0) {
			Canvas canvas = new Canvas(barcode);
			Paint paint = new Paint();
			paint.setColor(getResources().getColor(R.color.result_points));
			if (points.length == 2) {
				paint.setStrokeWidth(4.0f);
				drawLine(canvas, paint, points[0], points[1], scaleFactor);
			} else if (points.length == 4
					&& (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A || rawResult
							.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
				// Hacky special case -- draw two lines, for the barcode and
				// metadata
				drawLine(canvas, paint, points[0], points[1], scaleFactor);
				drawLine(canvas, paint, points[2], points[3], scaleFactor);
			} else {
				paint.setStrokeWidth(10.0f);
				for (ResultPoint point : points) {
					if (point != null) {
						canvas.drawPoint(scaleFactor * point.getX(),
								scaleFactor * point.getY(), paint);
					}
				}
			}
		}
	}

	private static void drawLine(Canvas canvas, Paint paint, ResultPoint a,
			ResultPoint b, float scaleFactor) {
		if (a != null && b != null) {
			canvas.drawLine(scaleFactor * a.getX(), scaleFactor * a.getY(),
					scaleFactor * b.getX(), scaleFactor * b.getY(), paint);
		}
	}

	// Put up our own UI for how to handle the decoded contents.
	/**
	 * 该方法会将最终处理的结果展示到result_view上。并且如果选择了"检索更多信息"则会内部对结果进行进一步的查询
	 * 
	 * @param rawResult
	 * @param resultHandler
	 * @param barcode
	 */
	private void handleDecodeInternally(Result rawResult,
			ResultHandler resultHandler, Bitmap barcode) {
		viewfinderView.setVisibility(View.GONE);
		captureDesc.setVisibility(View.GONE);
		resultView.setVisibility(View.VISIBLE);
		barCodeImage.setVisibility(View.VISIBLE);
		if (barcode == null) {
			barCodeImage.setImageBitmap(BitmapFactory.decodeResource(
					getResources(), R.mipmap.ic_launcher));
		} else {
			barCodeImage.setImageBitmap(barcode);
		}

		CharSequence displayContents = resultHandler.getDisplayContents();
		new ValidateTicket().execute(displayContents.toString());
	}

	private class ValidateTicket extends AsyncTask<String,String,String>{

		private String qrCode;
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			beforeValidate();
		}

		@Override
		protected String doInBackground(String... params) {
			String result = null;
			qrCode = params[0];
			Log.i(TAG, "qrCode:" + qrCode);
			String publicKey = getPublicKeyStringFromPemFormat();
			Log.i(TAG, "publicKey:" + publicKey);
			if (!TextUtils.isEmpty(publicKey)){
				result = encryptRSA(publicKey, qrCode);
				Log.i(TAG, "result:" + result);
			}
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (!TextUtils.isEmpty(result)) {
				onValidateSuccessed(result);
			} else {
				onValidateFailed("失败");
			}
			restartPreviewAfterDelay(1000);
		}
		
		private void beforeValidate(){
			captureResultInfo.setVisibility(View.GONE);
			statusImage.setVisibility(View.GONE);
			statusText.setText("已扫描，正在处理中");
			notFound.setVisibility(View.GONE);
			progress.setVisibility(View.VISIBLE);
		}
		
		private void onValidateSuccessed(String name){
			beepManager.playBeepSoundAndVibrate(true);
			// 成功
			progress.setVisibility(View.GONE);
			notFound.setVisibility(View.GONE);
			captureResultInfo.setVisibility(View.VISIBLE);
			captureResultName.setText(name);
			statusImage.setVisibility(View.VISIBLE);
			statusImage.setImageResource(R.mipmap.scan_img_pass);
			statusText.setText("成功");
		}

		private void onValidateFailed(String reason){
			beepManager.playBeepSoundAndVibrate(false);
			// 失败
			progress.setVisibility(View.GONE);
			notFound.setVisibility(View.VISIBLE);
			notFound.setText(reason);
			captureResultInfo.setVisibility(View.GONE);
			captureResultName.setText("");
			captureResultDate.setText("");
			captureResultSchedule.setText("");
			statusImage.setVisibility(View.VISIBLE);
			statusImage.setImageResource(R.mipmap.scan_img_fail);
			statusText.setText("失败");
		}
		
		public String encryptRSA(String keyString, String toDecryptStr) {
			String result = null;
			try {
				// converts the String to a PublicKey instance
				byte[] keyBytes = Base64.decode(keyString.getBytes("UTF-8"), Base64.NO_WRAP);
				Log.i(TAG, "key length=" + keyBytes.length);
				X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
				KeyFactory keyFactory = KeyFactory.getInstance("RSA");
				PublicKey key = keyFactory.generatePublic(spec);

				// decrypts the message
				Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
				cipher.init(Cipher.DECRYPT_MODE, key);
				byte[] toDecryptBytes = Base64.decode(toDecryptStr, Base64.NO_WRAP);
				Log.i(TAG, "toDecrypt length=" + toDecryptBytes.length);
				byte[] dectypted = cipher.doFinal(toDecryptBytes);
				result = new String(dectypted, "utf-8");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return result;
		}
		
		private String getPublicKeyStringFromPemFormat(){
			StringBuffer content = new StringBuffer();
			String filesDir = null;
			try {
				filesDir = getActivity().getFilesDir().getAbsolutePath();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (!TextUtils.isEmpty(filesDir)){
				String destFilePath = filesDir + File.separator + "KEY";
				
				File destFile = new File(destFilePath);
				if (destFile.exists()) {
					try{
						InputStream input = new FileInputStream(destFile);
						InputStreamReader read = new InputStreamReader(input, "utf-8");
						BufferedReader pemReader = new BufferedReader(read);
						String line = null;
						while ((line = pemReader.readLine()) != null) {
							if (line.indexOf("-----BEGIN PUBLIC KEY-----") != -1) {
								while ((line = pemReader.readLine()) != null) {
									if (line.indexOf("-----END PUBLIC KEY") != -1) {
										break;
									}
									content.append(line.trim());
								}
								break;
							}
						}
						pemReader.close();
						read.close();
						input.close();
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}else{
					Log.i(TAG, "PUBLIC KEY NOT FOUND");
				}
			}
			
			return content.toString();
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
				handler = new CaptureActivityHandler(this, null,
						null, null, cameraManager);
			}
			decodeOrStoreSavedBitmap(null, null);
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
		viewfinderView.setVisibility(View.VISIBLE);
		barCodeImage.setVisibility(View.INVISIBLE);
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
