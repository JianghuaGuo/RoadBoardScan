package com.grabtaxi.roadboardscan.zxing.camera;

import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;

import com.google.zxing.client.android.camera.CameraConfigurationUtils;
import com.grabtaxi.roadboardscan.common.GlobalVariables;

import java.util.Iterator;
import java.util.List;

/**
 * A class which deals with reading, parsing, and setting the camera parameters
 * which are used to configure the camera hardware.
 * 
 * <br/>
 * 
 * 摄像头参数的设置类
 */
public final class CameraConfigurationManager {

	private static final String TAG = "CameraConfiguration";
	/**
	 * 屏幕分辨率
	 */
	private Point screenResolution;

	/**
	 * 相机分辨率
	 */
	private Point cameraResolution;

	public CameraConfigurationManager() {
	}

	/**
	 * 计算屏幕分辨率和当前最适合的相机像素 Reads, one time, values from the camera that are
	 * needed by the app.
	 */
	public void initFromCameraParameters(Camera camera) {
		Camera.Parameters parameters = camera.getParameters();
		Log.i(TAG, "Default preview format: " + parameters.getPreviewFormat()
				+ '/' + parameters.get("preview-format"));

		Log.i(TAG, "SupportedPictureFormats:" + parameters.getSupportedPictureFormats());
		List rawSupportedPictureSizes = parameters.getSupportedPictureSizes();
		StringBuilder strPictureSize = new StringBuilder();
		Iterator pictureSizeIter = rawSupportedPictureSizes.iterator();

		while(pictureSizeIter.hasNext()) {
			Camera.Size it = (Camera.Size)pictureSizeIter.next();
			strPictureSize.append(it.width).append('x').append(it.height).append(' ');
		}
		Log.i(TAG, "SupportedPictureSizes:" + strPictureSize);
		Log.i(TAG, "SupportedPreviewFormats:" + parameters.getSupportedPreviewFormats());
		List rawSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
		StringBuilder strPreviewSize = new StringBuilder();
		Iterator previewSizeIter = rawSupportedPreviewSizes.iterator();

		while(previewSizeIter.hasNext()) {
			Camera.Size it = (Camera.Size)previewSizeIter.next();
			strPreviewSize.append(it.width).append('x').append(it.height).append(' ');
		}
		Log.i(TAG, "SupportedPreviewSizes:" + strPreviewSize);
		screenResolution = new Point(GlobalVariables.SCREEN_WIDTH, GlobalVariables.SCREEN_HEIGHT);
		Log.i(TAG, "Screen resolution: " + screenResolution);
		// preview size is always like 480*320,other 320*480
		Point screenResolutionForCamera = new Point();
		screenResolutionForCamera.x = screenResolution.x;
		screenResolutionForCamera.y = screenResolution.y;
		if (screenResolution.x < screenResolution.y){
			screenResolutionForCamera.x = screenResolution.y;
			screenResolutionForCamera.y = screenResolution.x;
		}
		Log.i(TAG, "screenResolutionForCamera: " + screenResolutionForCamera);
		cameraResolution = CameraConfigurationUtils.findBestPreviewSizeValue(
				parameters, screenResolutionForCamera);
		Log.i(TAG, "Camera resolution: " + cameraResolution);
	}

	public Point getCameraResolution() {
		return cameraResolution;
	}

	public Point getScreenResolution() {
		return screenResolution;
	}
	public synchronized Rect getTopFramingRectForOneShot() {
		int rectWidth = (int) (300 * GlobalVariables.SCREEN_DESITY);;
		int rectHeight = (int) (60 * GlobalVariables.SCREEN_DESITY);;
		int rectMargin = (int) (10 * GlobalVariables.SCREEN_DESITY);;
		int leftOffset = (GlobalVariables.SCREEN_WIDTH - rectWidth) / 2;
		int topOffset = (int) ((80 + 60) * GlobalVariables.SCREEN_DESITY);
		Rect topFramingRect = new Rect(leftOffset, topOffset, leftOffset + rectWidth,
				topOffset + rectHeight);
		return topFramingRect;
	}

	public synchronized Rect getFramingRectInPreviewForOneShot(Rect rect) {
		Log.d(TAG, "getFramingRectInPreview input rect: " + rect);
		Rect framingRectInPreview = new Rect();
		Point cameraResolution = getCameraResolution();
		Point screenResolution = getScreenResolution();
		if (cameraResolution == null || screenResolution == null) {
			// Called early, before init even finished
			return null;
		}
		// 由于修改了屏幕的初始方向，手机分辨率由原来的width*height变为height*width形式，但是相机的分辨率则是固定的，因此这里需做些调整以计算出正确的缩放比率。
		rect.left = rect.left * cameraResolution.y / screenResolution.x;
		rect.right = rect.right * cameraResolution.y / screenResolution.x;
		rect.top = rect.top * cameraResolution.x / screenResolution.y;
		rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
		framingRectInPreview = rect;

		Log.d(TAG, "getFramingRectInPreview calculated framingRectInPreview: "
				+ framingRectInPreview);
		Log.d(TAG, "cameraResolution: " + cameraResolution);
		Log.d(TAG, "screenResolution: " + screenResolution);
		return framingRectInPreview;
	}

	/**
	 * 读取配置设置相机的对焦模式、闪光灯模式等等
	 * 
	 * @param camera
	 * @param safeMode
	 */
	void setDesiredCameraParameters(Camera camera, boolean safeMode) {
		Camera.Parameters parameters = camera.getParameters();

		if (parameters == null) {
			Log.w(TAG,
					"Device error: no camera parameters are available. Proceeding without configuration.");
			return;
		}

		Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

		if (safeMode) {
			Log.w(TAG,
					"In camera config safe mode -- most settings will not be honored");
		}

		// 初始化闪光灯,默认不开启闪光灯
		doSetTorch(parameters, false, safeMode);

		// 默认使用自动对焦
		CameraConfigurationUtils.setFocus(parameters, true, true, safeMode);

		if (!safeMode) {
//			CameraConfigurationUtils.setInvertColor(parameters);
			CameraConfigurationUtils.setVideoStabilization(parameters);
			CameraConfigurationUtils.setFocusArea(parameters);
			CameraConfigurationUtils.setMetering(parameters);
		}

//		parameters.setPictureFormat(PixelFormat.JPEG);
//		parameters.setPictureSize(cameraResolution.x, cameraResolution.y);
//		parameters.setJpegQuality(100);
		parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
		Log.i(TAG, "Final camera parameters: " + parameters.flatten());
		camera.setParameters(parameters);

		Camera.Parameters afterParameters = camera.getParameters();
		Camera.Size afterSize = afterParameters.getPreviewSize();
		if (afterSize != null
				&& (cameraResolution.x != afterSize.width || cameraResolution.y != afterSize.height)) {
			Log.w(TAG, "Camera said it supported preview size "
					+ cameraResolution.x + 'x' + cameraResolution.y
					+ ", but after setting it, preview size is "
					+ afterSize.width + 'x' + afterSize.height);
			cameraResolution.x = afterSize.width;
			cameraResolution.y = afterSize.height;
		}
		// 调整相机preview的时钟方向与手机竖屏的自然方向一致。该方法必须在startPreview之前被调用，在预览界面展示出来后设置是无效的。
		 camera.setDisplayOrientation(90);
	}

	boolean getTorchState(Camera camera) {
		if (camera != null) {
			Camera.Parameters parameters = camera.getParameters();
			if (parameters != null) {
				String flashMode = parameters.getFlashMode();
				return flashMode != null
						&& (Camera.Parameters.FLASH_MODE_ON.equals(flashMode) || Camera.Parameters.FLASH_MODE_TORCH
								.equals(flashMode));
			}
		}
		return false;
	}

	void setTorch(Camera camera, boolean newSetting) {
		Camera.Parameters parameters = camera.getParameters();
		doSetTorch(parameters, newSetting, false);
		camera.setParameters(parameters);
	}

	private void doSetTorch(Camera.Parameters parameters, boolean newSetting,
			boolean safeMode) {
		CameraConfigurationUtils.setTorch(parameters, newSetting);
		if (!safeMode) {
			CameraConfigurationUtils.setBestExposure(parameters, newSetting);
		}
	}
}
