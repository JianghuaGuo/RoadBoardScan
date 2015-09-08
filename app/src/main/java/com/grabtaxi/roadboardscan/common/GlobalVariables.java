package com.grabtaxi.roadboardscan.common;

public class GlobalVariables {
	private static GlobalVariables globalVariables;
	public static int SCREEN_WIDTH;// 屏幕宽
	public static int SCREEN_HEIGHT;// 屏幕高
	public static float SCREEN_DESITY;// 屏幕密度

	// 禁止创建对象
	private GlobalVariables() {

	}

	public static GlobalVariables getInstance() {
		if (null == globalVariables) {
			globalVariables = new GlobalVariables();
		}
		return globalVariables;
	}

}
