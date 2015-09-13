/*
 * Copyright (C) 2008 ZXing authors
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

package com.grabtaxi.roadboardscan.zxing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.grabtaxi.roadboardscan.R;
import com.grabtaxi.roadboardscan.common.GlobalVariables;
import com.grabtaxi.roadboardscan.zxing.camera.CameraManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the laser scanner
 * animation and result points.
 * 
 * <br/>
 * <br/>
 * 该视图是覆盖在相机的预览视图之上的一层视图。扫描区构成原理，其实是在预览视图上画四块遮罩层，
 * 中间留下的部分保持透明，并画上一条激光线，实际上该线条就是展示而已，与扫描功能没有任何关系。
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderViewForShot extends View {
	/**
	 * 刷新界面的时间
	 */
	private static final long ANIMATION_DELAY = 10L;

	/**
	 * 四个绿色边角对应的长度
	 */
	private int ScreenRate;
	/**
	 * 四个绿色边角对应的宽度
	 */
	private static final int CORNER_WIDTH = 6;
	private static final int L_WIDTH = 1;

	/**
	 * 画笔对象的引用
	 */
	private Paint paint;
	private final int maskColor;

	// This constructor is used when the class is built from an XML resource.
	public ViewfinderViewForShot(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Initialize these once for performance rather than calling them every
		// time in onDraw().
		// 将像素转换成dp
		ScreenRate = (int) (20 * GlobalVariables.SCREEN_DESITY);
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.viewfinder_mask);
	}

	@SuppressLint("DrawAllocation")
	@Override
	public void onDraw(Canvas canvas) {
		// 定义上下视窗大小
		int rectWidth = (int) (300 * GlobalVariables.SCREEN_DESITY);;
		int rectHeight = (int) (60 * GlobalVariables.SCREEN_DESITY);;
		int rectMargin = (int) (10 * GlobalVariables.SCREEN_DESITY);;
		int leftOffset = (GlobalVariables.SCREEN_WIDTH - rectWidth) / 2;
		int topOffset = (int) ((80 + 60) * GlobalVariables.SCREEN_DESITY);
		Rect topRect = new Rect(leftOffset, topOffset, leftOffset + rectWidth,
				topOffset + rectHeight);

		Rect bottomRect = new Rect(leftOffset, topOffset + rectHeight + rectMargin, leftOffset + rectWidth, topOffset + rectHeight + rectMargin + rectHeight);
		//获取屏幕的宽和高
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		// Draw the exterior (i.e. outside the framing rect) darkened
		/*
		 * 预览界面的绘制
		 */
		paint.setColor(maskColor);
		//画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
		//扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
		canvas.drawRect(0, 0, width, topRect.top - 1, paint);
		canvas.drawRect(0, topRect.top - 1, topRect.left - 1, bottomRect.bottom + 1,
				paint);
		canvas.drawRect(topRect.right + 1, topRect.top - 1, width,
				bottomRect.bottom + 1, paint);
		canvas.drawRect(0, bottomRect.bottom + 1, width, height, paint);
		canvas.drawRect(topRect.left - 1, topRect.bottom + 1, topRect.right + 1, bottomRect.top - 1, paint);

		// 画预览灰线框，共4个部分
		paint.setColor(Color.GRAY);
		canvas.drawRect(topRect.left, topRect.top, topRect.right, topRect.top
				+ L_WIDTH, paint);
		canvas.drawRect(topRect.left, topRect.top, topRect.left + L_WIDTH,
				topRect.bottom, paint);
		canvas.drawRect(topRect.left, topRect.bottom - L_WIDTH, topRect.right,
				topRect.bottom, paint);
		canvas.drawRect(topRect.right - L_WIDTH, topRect.top, topRect.right,
				topRect.bottom, paint);

		canvas.drawRect(bottomRect.left, bottomRect.top, bottomRect.right, bottomRect.top
				+ L_WIDTH, paint);
		canvas.drawRect(bottomRect.left, bottomRect.top, bottomRect.left + L_WIDTH,
				bottomRect.bottom, paint);
		canvas.drawRect(bottomRect.left, bottomRect.bottom - L_WIDTH, bottomRect.right,
				bottomRect.bottom, paint);
		canvas.drawRect(bottomRect.right - L_WIDTH, bottomRect.top, bottomRect.right,
				bottomRect.bottom, paint);
		// 画扫描框边上的角，总共8个部分
		paint.setColor(Color.GREEN);
		canvas.drawRect(topRect.left, topRect.top, topRect.left + ScreenRate,
				topRect.top + CORNER_WIDTH, paint);
		canvas.drawRect(topRect.left, topRect.top, topRect.left + CORNER_WIDTH,
				topRect.top + ScreenRate, paint);
		canvas.drawRect(topRect.right - ScreenRate, topRect.top, topRect.right,
				topRect.top + CORNER_WIDTH, paint);
		canvas.drawRect(topRect.right - CORNER_WIDTH, topRect.top, topRect.right,
				topRect.top + ScreenRate, paint);
		canvas.drawRect(topRect.left, topRect.bottom - CORNER_WIDTH, topRect.left
				+ ScreenRate, topRect.bottom, paint);
		canvas.drawRect(topRect.left, topRect.bottom - ScreenRate, topRect.left
				+ CORNER_WIDTH, topRect.bottom, paint);
		canvas.drawRect(topRect.right - ScreenRate, topRect.bottom
				- CORNER_WIDTH, topRect.right, topRect.bottom, paint);
		canvas.drawRect(topRect.right - CORNER_WIDTH, topRect.bottom
				- ScreenRate, topRect.right, topRect.bottom, paint);

		canvas.drawRect(bottomRect.left, bottomRect.top, bottomRect.left + ScreenRate,
				bottomRect.top + CORNER_WIDTH, paint);
		canvas.drawRect(bottomRect.left, bottomRect.top, bottomRect.left + CORNER_WIDTH,
				bottomRect.top + ScreenRate, paint);
		canvas.drawRect(bottomRect.right - ScreenRate, bottomRect.top, bottomRect.right,
				bottomRect.top + CORNER_WIDTH, paint);
		canvas.drawRect(bottomRect.right - CORNER_WIDTH, bottomRect.top, bottomRect.right,
				bottomRect.top + ScreenRate, paint);
		canvas.drawRect(bottomRect.left, bottomRect.bottom - CORNER_WIDTH, bottomRect.left
				+ ScreenRate, bottomRect.bottom, paint);
		canvas.drawRect(bottomRect.left, bottomRect.bottom - ScreenRate, bottomRect.left
				+ CORNER_WIDTH, bottomRect.bottom, paint);
		canvas.drawRect(bottomRect.right - ScreenRate, bottomRect.bottom
				- CORNER_WIDTH, bottomRect.right, bottomRect.bottom, paint);
		canvas.drawRect(bottomRect.right - CORNER_WIDTH, bottomRect.bottom
				- ScreenRate, bottomRect.right, bottomRect.bottom, paint);

		// 刷新界面区域
		postInvalidateDelayed(ANIMATION_DELAY, 0, 0, width, height);
	}

	public void drawViewfinder() {
		invalidate();
	}

}
