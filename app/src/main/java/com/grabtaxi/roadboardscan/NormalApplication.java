
package com.grabtaxi.roadboardscan;

import android.app.Application;

public class NormalApplication extends Application {

	private static NormalApplication sInstance;

	public static NormalApplication getInstance() {
		return sInstance;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		sInstance = this;
	}

	@Override
	public void onTerminate() {
		// TODO Auto-generated method stub
		super.onTerminate();
	}
}
