package com.faceunity.app.base;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * @author : Administrator
 * @date : 2021/9/13
 * @description :
 */
public class MyGlSurface extends GLSurfaceView {

    public MyGlSurface(Context context) {
        super(context);
    }

    public MyGlSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return
//        mGLThread.surfaceDestroyed();
        Log.e("xefod","----surfaceDestroyed call----");
//        super.surfaceDestroyed(holder);
    }
}
