package com.grabtaxi.roadboardscan.fragment;

import android.view.Gravity;
import android.widget.Toast;
import android.support.v4.app.Fragment;

public class BaseFragment extends Fragment {
	/**
	 * Toast提醒
	 * 
	 * @param msg
	 */
	public void showToast(String msg) {
		Toast toast = Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
}
