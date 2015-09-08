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

package com.grabtaxi.roadboardscan.zxing.camera;

import java.lang.reflect.Method;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.zxing.client.android.camera.CameraConfigurationUtils;

/**
 * A class which deals with reading, parsing, and setting the camera parameters
 * which are used to configure the camera hardware.
 * 
 * <br/>
 * 
 * 摄像头参数的设置类
 */
final class CameraConfigurationManager {

	private static final String TAG = "CameraConfiguration";

	private final Context context;
	/**
	 * 屏幕分辨率
	 */
	private Point screenResolution;

	/**
	 * 相机分辨率
	 */
	private Point cameraResolution;

	CameraConfigurationManager(Context context) {
		this.context = context;
	}

	/**
	 * 计算屏幕分辨率和当前最适合的相机像素 Reads, one time, values from the camera that are
	 * needed by the app.
	 */
	void initFromCameraParameters(Camera camera) {
		Camera.Parameters parameters = camera.getParameters();
		Log.d(TAG, "Default preview format: " + parameters.getPreviewFormat()
				+ '/' + parameters.get("preview-format"));
		WindowManager manager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		screenResolution = new Point(display.getWidth(), display.getHeight());
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
		// 兼容2.1
		// camera.setDisplayOrientation(90);
		setDisplayOrientation(camera, 90);
	}

	Point getCameraResolution() {
		return cameraResolution;
	}

	Point getScreenResolution() {
		return screenResolution;
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

	/**
	 * compatible 1.6
	 * 
	 * @param camera
	 * @param angle
	 */
	protected void setDisplayOrientation(Camera camera, int angle) {
		Method downPolymorphic;
		try {
			downPolymorphic = camera.getClass().getMethod(
					"setDisplayOrientation", new Class[] { int.class });
			if (downPolymorphic != null)
				downPolymorphic.invoke(camera, new Object[] { angle });
		} catch (Exception e1) {
		}
	}

}
