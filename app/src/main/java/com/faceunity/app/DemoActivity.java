package com.faceunity.app;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.faceunity.app.base.BaseActivity;
import com.faceunity.app.data.PropDataFactory;
import com.faceunity.app.entity.FunctionEnum;
import com.faceunity.core.entity.FUCameraConfig;
import com.faceunity.core.entity.FURenderFrameData;
import com.faceunity.core.entity.FURenderInputData;
import com.faceunity.core.entity.FURenderOutputData;
import com.faceunity.core.enumeration.FUAIProcessorEnum;
import com.faceunity.core.faceunity.FUAIKit;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.listener.OnGlRendererListener;
import com.faceunity.core.renderer.CameraRenderer;
import com.faceunity.ui.control.PropControlView;
import com.faceunity.ui.dialog.ToastHelper;
import com.faceunity.ui.entity.PropBean;

/**
 * Created by Android Studio.
 * User: liheng
 * Date: 2021/9/9
 * Time: 21:50
 */
class DemoActivity extends BaseActivity {
    protected GLSurfaceView mSurfaceView;
    ImageView mImage;
    protected ViewStub mStubBottom;
    protected View mStubView;

    //stick config
    private int mFunctionType;
    private PropControlView mPropControlView;
    private PropDataFactory mPropDataFactory;
    protected Handler mMainHandler;

    /*检测 开关*/
    protected boolean isAIProcessTrack = true;
    /*检测标识*/
    protected int aIProcessTrackStatus = 1;


    //region CameraRenderer
    protected FURenderKit mFURenderKit = FURenderKit.getInstance();
    protected FUAIKit mFUAIKit = FUAIKit.getInstance();
    protected CameraRenderer mCameraRenderer;
    private int cameraRenderType = 0;

    //其他ui
    protected TextView mTrackingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public int getLayoutResID() {
        return R.layout.activity_main_demo_test;
    }

    @Override
    public void initData() {
        mMainHandler = new Handler();
        mFunctionType = FunctionEnum.STICKER;
        mPropDataFactory = new PropDataFactory(mPropListener, mFunctionType, 1);
    }

    protected int getStubBottomLayoutResID() {
        return R.layout.layout_control_prop;
    }

    @Override
    public void initView() {
        mSurfaceView = findViewById(R.id.gl_surface1);
        mImage = (ImageView) findViewById(R.id.outputImg_demo);
        mStubBottom = findViewById(R.id.stub_bottom);
        mStubBottom.setInflatedId(R.id.stub_bottom);
        if (getStubBottomLayoutResID() != 0) {
            mStubBottom.setLayoutResource(getStubBottomLayoutResID());
            mStubView = mStubBottom.inflate();
        }
        mTrackingView = findViewById(R.id.tv_tracking_demo);
    }

    @Override
    public void bindListener() {
        mCameraRenderer = new CameraRenderer(mSurfaceView, getCameraConfig(), mOnGlRendererListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCameraRenderer.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCameraRenderer.onPause();
    }


    @Override
    public void onDestroy() {
        mCameraRenderer.onDestroy();
        super.onDestroy();
    }

    /**
     * 检测结果变更回调
     *
     * @param fuaiProcessorEnum
     * @param status
     */
    protected void onTrackStatusChanged(FUAIProcessorEnum fuaiProcessorEnum, int status) {
        mTrackingView.setVisibility((status > 0) ? View.INVISIBLE : View.VISIBLE);
        if (status <= 0) {
            if (fuaiProcessorEnum == FUAIProcessorEnum.FACE_PROCESSOR) {
                mTrackingView.setText(R.string.fu_base_is_tracking_text);
            } else if (fuaiProcessorEnum == FUAIProcessorEnum.HUMAN_PROCESSOR) {
                mTrackingView.setText(R.string.toast_not_detect_body);
            }
            if (fuaiProcessorEnum == FUAIProcessorEnum.HAND_GESTURE_PROCESSOR) {
                mTrackingView.setText(R.string.toast_not_detect_gesture);
            }
        }
    }

    /**
     * 配置相机参数
     *
     * @return CameraBuilder
     */
    protected FUCameraConfig getCameraConfig() {
        FUCameraConfig cameraConfig = new FUCameraConfig();
        return cameraConfig;
    }

    /**
     * 检测类型
     *
     * @return
     */
    protected FUAIProcessorEnum getFURenderKitTrackingType() {
        return FUAIProcessorEnum.FACE_PROCESSOR;
    }


    private final OnGlRendererListener mOnGlRendererListener = new OnGlRendererListener() {
        @Override
        public void onDrawFrameAfter() {

        }

        @Override
        public void onRenderAfter(@NonNull FURenderOutputData fuRenderOutputData, @NonNull FURenderFrameData fuRenderFrameData) {

        }

        @Override
        public void onRenderBefore(@Nullable FURenderInputData fuRenderInputData) {

        }

        @Override
        public void onSurfaceChanged(int i, int i1) {

        }

        @Override
        public void onSurfaceCreated() {

        }

        @Override
        public void onSurfaceDestroy() {

        }

        /*AI识别数目检测*/
        private void trackStatus() {
            if (!isAIProcessTrack) {
                return;
            }
            FUAIProcessorEnum fuaiProcessorEnum = getFURenderKitTrackingType();
            int trackCount;
            if (fuaiProcessorEnum == FUAIProcessorEnum.HAND_GESTURE_PROCESSOR) {
                trackCount = mFUAIKit.handProcessorGetNumResults();
            } else if (fuaiProcessorEnum == FUAIProcessorEnum.HUMAN_PROCESSOR) {
                trackCount = mFUAIKit.humanProcessorGetNumResults();
            } else {
                trackCount = mFUAIKit.isTracking();
            }
            if (aIProcessTrackStatus != trackCount) {
                aIProcessTrackStatus = trackCount;
                runOnUiThread(() -> onTrackStatusChanged(fuaiProcessorEnum, trackCount));
            }
        }
    };

    private PropDataFactory.PropListener mPropListener = new PropDataFactory.PropListener() {

        @Override
        public void onItemSelected(PropBean bean) {
            if (mFunctionType == FunctionEnum.GESTURE_RECOGNITION) {
                if (bean.getPath() == null) {
                    mMainHandler.post(() -> {
                        isAIProcessTrack = false;
                        mTrackingView.setVisibility(View.INVISIBLE);
                        aIProcessTrackStatus = 1;
                    });
                } else {
                    isAIProcessTrack = true;
                }
            }
            if (bean.getDescId() > 0) {
                mMainHandler.post(() -> showDescription(bean.getDescId(), 1500));
            }
        }
    };

    /**
     * 显示提示描述
     *
     * @param strRes Int
     * @param time   Int
     */
    public void showDescription(int strRes, long time) {
        if (strRes == 0) {
            return;
        }
        runOnUiThread(() -> showToast(strRes));
    }
    /**
     * 显示提示描述
     */
    public void showToast(int res) {
        ToastHelper.showWhiteTextToast(this, res);
    }
}
