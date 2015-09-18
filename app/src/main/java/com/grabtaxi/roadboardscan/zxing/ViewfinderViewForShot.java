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

import com.grabtaxi.roadboardscan.R;
import com.grabtaxi.roadboardscan.common.GlobalVariables;
import com.grabtaxi.roadboardscan.zxing.camera.CameraManager;
import com.grabtaxi.roadboardscan.zxing.camera.CameraConfigurationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ViewfinderViewForShot extends View {
	/**
	 * 刷新界面的时间
	 */
	private static final long ANIMATION_DELAY = 10L;

	/**
	 * 四个绿色边角对应的长度
	 */
	private int CORNER_LENGTH;
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
	private CameraConfigurationManager configManager;

	// This constructor is used when the class is built from an XML resource.
	public ViewfinderViewForShot(Context context, AttributeSet attrs) {
		super(context, attrs);
		// 将像素转换成dp
		CORNER_LENGTH = (int) (20 * GlobalVariables.SCREEN_DESITY);
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.viewfinder_mask);
	}

	public void setCameraConfigurationManager(CameraConfigurationManager cfm){
		this.configManager = cfm;
	}

	@SuppressLint("DrawAllocation")
	@Override
	public void onDraw(Canvas canvas) {
		if (this.configManager == null){
			return;
		}
		Rect topRect = configManager.getTopFramingRectForOneShot();
		//获取屏幕的宽和高
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		// Draw the exterior (i.e. outside the framing rect) darkened
		/*
		 * 预览界面的绘制
		 */
		paint.setColor(maskColor);
		// 画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
		// 扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
		canvas.drawRect(0, 0, width, topRect.top - 1, paint);
		canvas.drawRect(0, topRect.top - 1, topRect.left - 1, topRect.bottom + 1,
				paint);
		canvas.drawRect(topRect.right + 1, topRect.top - 1, width,
				topRect.bottom + 1, paint);
		canvas.drawRect(0, topRect.bottom + 1, width, height, paint);

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

		// 画扫描框边上的角，总共8个部分
		paint.setColor(Color.GREEN);
		canvas.drawRect(topRect.left, topRect.top, topRect.left + CORNER_LENGTH,
				topRect.top + CORNER_WIDTH, paint);
		canvas.drawRect(topRect.left, topRect.top, topRect.left + CORNER_WIDTH,
				topRect.top + CORNER_LENGTH, paint);
		canvas.drawRect(topRect.right - CORNER_LENGTH, topRect.top, topRect.right,
				topRect.top + CORNER_WIDTH, paint);
		canvas.drawRect(topRect.right - CORNER_WIDTH, topRect.top, topRect.right,
				topRect.top + CORNER_LENGTH, paint);
		canvas.drawRect(topRect.left, topRect.bottom - CORNER_WIDTH, topRect.left
				+ CORNER_LENGTH, topRect.bottom, paint);
		canvas.drawRect(topRect.left, topRect.bottom - CORNER_LENGTH, topRect.left
				+ CORNER_WIDTH, topRect.bottom, paint);
		canvas.drawRect(topRect.right - CORNER_LENGTH, topRect.bottom
				- CORNER_WIDTH, topRect.right, topRect.bottom, paint);
		canvas.drawRect(topRect.right - CORNER_WIDTH, topRect.bottom
				- CORNER_LENGTH, topRect.right, topRect.bottom, paint);

		// 刷新界面区域
		postInvalidateDelayed(ANIMATION_DELAY, 0, 0, width, height);
	}

	public void drawViewfinder() {
		invalidate();
	}

}
