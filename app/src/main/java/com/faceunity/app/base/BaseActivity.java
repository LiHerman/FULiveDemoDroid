package com.faceunity.app.base;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.faceunity.app.service.FloatingService;
import com.faceunity.app.service.HomeWatcherReceiver;
import com.faceunity.ui.dialog.ToastHelper;

/**
 * DESC：
 * Created on 2021/4/12
 */
public abstract class BaseActivity extends AppCompatActivity {
    private static final String LOG_TAG = "HomeReceiver";
    private static HomeWatcherReceiver mHomeKeyReceiver = null;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        this.getWindow().setFlags(0x80000000, 0x80000000);
        if (getLayoutResID() > 0) {
            setContentView(getLayoutResID());
        }
        initData();
        initView();
        bindListener();
    }

    public abstract int getLayoutResID();

    public abstract void initData();

    public abstract void initView();

    public abstract void bindListener();

    @Override
    protected void onPause() {
        super.onPause();
        ToastHelper.dismissToast();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerHomeKeyReceiver(this);
    }

    @Override
    protected void onDestroy() {
        unregisterHomeKeyReceiver(this);
        super.onDestroy();
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            Toast.makeText(this, "返回键无效", Toast.LENGTH_SHORT).show();
////            return false;//return true;拦截事件传递,从而屏蔽back键。
//        }
//        if (KeyEvent.KEYCODE_HOME == keyCode) {
//            BaseFaceUnityDemoActivity.logWrap("home","press home");
//            Toast.makeText(getApplicationContext(), "HOME 键已被禁用...", Toast.LENGTH_SHORT).show();
//            FloatingService.OpenOrFloatWindows(this);
////            return false;//同理
//        }
//        return super.onKeyDown(keyCode, event);
//    }

    private static void registerHomeKeyReceiver(Context context) {
        Log.i(LOG_TAG, "registerHomeKeyReceiver");
        mHomeKeyReceiver = new HomeWatcherReceiver();
        final IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        context.registerReceiver(mHomeKeyReceiver, homeFilter);
    }

    private static void unregisterHomeKeyReceiver(Context context) {
        Log.i(LOG_TAG, "unregisterHomeKeyReceiver");
        if (null != mHomeKeyReceiver) {
            context.unregisterReceiver(mHomeKeyReceiver);
        }
    }


}
