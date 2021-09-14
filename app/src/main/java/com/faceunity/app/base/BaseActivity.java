package com.faceunity.app.base;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.faceunity.app.service.FloatingService;
import com.faceunity.ui.dialog.ToastHelper;

/**
 * DESC：
 * Created on 2021/4/12
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(0x80000000, 0x80000000);
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Toast.makeText(this, "返回键无效", Toast.LENGTH_SHORT).show();
            return false;//return true;拦截事件传递,从而屏蔽back键。
        }
        if (KeyEvent.KEYCODE_HOME == keyCode) {
            FloatingService.OpenOrFloatWindows(this);
            Toast.makeText(getApplicationContext(), "HOME 键已被禁用...", Toast.LENGTH_SHORT).show();
            return false;//同理
        }
        return super.onKeyDown(keyCode, event);
    }

}
