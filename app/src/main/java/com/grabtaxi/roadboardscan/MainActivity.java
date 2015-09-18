package com.grabtaxi.roadboardscan;

import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.grabtaxi.roadboardscan.common.GlobalVariables;
import com.grabtaxi.roadboardscan.fragment.CaptureFragment;
import com.grabtaxi.roadboardscan.fragment.CaptureFragmentForShot;
import com.grabtaxi.roadboardscan.fragment.FragmentTag;

public class MainActivity extends AppCompatActivity
{
    private final static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 获取屏幕参数
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        GlobalVariables.SCREEN_HEIGHT = dm.heightPixels;
        GlobalVariables.SCREEN_WIDTH = dm.widthPixels;
        GlobalVariables.SCREEN_DESITY = dm.density;
        Log.i(TAG, "screen_height:" + GlobalVariables.SCREEN_HEIGHT);
        Log.i(TAG, "screen_width:" + GlobalVariables.SCREEN_WIDTH);
        Log.i(TAG, "screen_density:" + GlobalVariables.SCREEN_DESITY);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        showCaptureFragment();
//        showCaptureFragmentForShot();
    }

    private void showCaptureFragment() {
        CaptureFragment captureFrag = new CaptureFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.main_content, captureFrag,
                FragmentTag.CaptureFragment);
        transaction.commit();
    }

    private void showCaptureFragmentForShot() {
        CaptureFragmentForShot captureFrag = new CaptureFragmentForShot();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.main_content, captureFrag,
                FragmentTag.CaptureFragmentForShot);
        transaction.commit();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onPostResume() {
        Log.d(TAG, "onPostResume");
        super.onPostResume();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
