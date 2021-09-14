package com.faceunity.app.service;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.faceunity.app.DemoConfig;
import com.faceunity.app.R;
import com.faceunity.app.base.BaseFaceUnityDemoActivity;
import com.faceunity.app.data.PropDataFactory;
import com.faceunity.app.data.source.PortraitSegmentSource;
import com.faceunity.app.entity.FunctionEnum;
import com.faceunity.core.entity.FUCameraConfig;
import com.faceunity.core.entity.FURenderFrameData;
import com.faceunity.core.entity.FURenderInputData;
import com.faceunity.core.entity.FURenderOutputData;
import com.faceunity.core.enumeration.CameraFacingEnum;
import com.faceunity.core.enumeration.FUAIProcessorEnum;
import com.faceunity.core.enumeration.FUAITypeEnum;
import com.faceunity.core.enumeration.FUTransformMatrixEnum;
import com.faceunity.core.faceunity.FUAIKit;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.listener.OnGlRendererListener;
import com.faceunity.core.renderer.CameraRenderer;
import com.faceunity.core.utils.GlUtil;
import com.faceunity.ui.entity.PropBean;

import org.jetbrains.annotations.NotNull;

import static com.faceunity.app.base.BaseFaceUnityDemoActivity.logWrap;

/**
 * @author : Administrator
 * @date : 2021/9/14
 * @description :
 */
public class FloatingService extends Service {

    static {
        System.loadLibrary("hello-jnicallback");
    }
        public native boolean writeByteToCamera(byte[] data, int length);
//    public  boolean writeByteToCamera(byte[] data, int length) {
//        return true;
//    }
    public native boolean writeFileToCamera(String filePath);

    private View floatView;
    private static WindowManager wm;
    private static WindowManager.LayoutParams params;
    private boolean isAdded = false; // 是否已增加悬浮窗
    protected GLSurfaceView mSurfaceView;

    //region CameraRenderer
    protected FURenderKit mFURenderKit = FURenderKit.getInstance();
    protected FUAIKit mFUAIKit = FUAIKit.getInstance();
    protected CameraRenderer mCameraRenderer;
    private int cameraRenderType = 0;
    /*检测 开关*/
    protected boolean isAIProcessTrack = true;
    /*检测标识*/
    protected int aIProcessTrackStatus = 1;
    /*Benchmark 开关*/
    private boolean isShowBenchmark = false;

    //两个维度 4个方向，一共16种可能做遍历
    public int lastOrient[] = {0,0};
    public int sucOrient[] = {0,0};
    public final int angle[] = {0,90,180,270};
    public boolean detectedFace = false;
    boolean fakeInput = false;

    //位置相关
    private float x;
    private float y;
    private float posx;
    private float startX = 0;
    private float startY = 0;

    //特效:
    private int mFunctionType;
    private PropDataFactory mPropDataFactory;

    public static void OpenOrFloatWindows(Context context) {
        Intent show = new Intent(context, FloatingService.class);
        context.startService(show);
    }

    public static void closeFloatWindow(Activity activity) {
        activity.stopService(new Intent(activity, FloatingService.class));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mFunctionType = FunctionEnum.STICKER;
        mPropDataFactory = new PropDataFactory(mPropListener, mFunctionType, 1);
        createFloatView();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (intent != null) {
            setupCellView(floatView);
        }
    }

    @Override
    public void onDestroy() {
        mPropDataFactory.releaseAIProcessor();
        mCameraRenderer.onDestroy();
        super.onDestroy();
    }

    /**
     * 创建悬浮窗
     */
    private void createFloatView() {
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        floatView = layoutInflater.inflate(R.layout.popup_window, null);

        wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();
        params.alpha = 1.0f;
        // 设置window type
//		params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        if (Build.VERSION.SDK_INT >= 19) {
            params.type = WindowManager.LayoutParams.TYPE_TOAST;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        /*
         * 如果设置为params.type = WindowManager.LayoutParams.TYPE_PHONE; 那么优先级会降低一些,
         * 即拉下通知栏不可见
         */
        params.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明

        // 设置Window flag
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        /*
         * 下面的flags属性的效果形同“锁定”。 悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应。
         * wmParams.flags=LayoutParams.FLAG_NOT_TOUCH_MODAL |
         * LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE;
         */

        // 设置悬浮窗的长得宽
        params.width = (int)(wm.getDefaultDisplay().getWidth()*0.6);
        params.height = params.width*9/16;
        params.gravity = Gravity.TOP | Gravity.TOP;
        params.x = 150;
        params.y = 300;

        wm.addView(floatView, params);
        isAdded = true;
    }

    /**
     * 设置浮窗view内部子控件
     * @param rootview
     */
    private void setupCellView(View rootview) {
        ImageView closedImg = (ImageView) rootview.findViewById(R.id.float_window_closed);

        closedImg.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isAdded) {
                    wm.removeView(floatView);
                    isAdded = false;
                    stopSelf();
                }
            }
        });

        mSurfaceView = rootview.findViewById(R.id.small_gl_surface);
        mCameraRenderer = new CameraRenderer(mSurfaceView, getCameraConfig(), mOnGlRendererListener);
        View touch_receiver = (View) rootview.findViewById(R.id.touch_receiver);
        posx = params.x;
        touch_receiver.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                x = event.getRawX();
                y = event.getRawY();

                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        Log.e("xiale","params.x= "+ params.x +" params.y= "+params.y);
                        params.x = (int)( x - startX-(0.2*wm.getDefaultDisplay().getWidth()));
                        params.y = (int) (y - startY-0.028*wm.getDefaultDisplay().getHeight());
                        wm.updateViewLayout(floatView, params);
                        Log.e("xiale","x= "+ x +" startX= "+startX);
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.e("xiale","params.x= "+ params.x +" params.y= "+params.y);
                        if(posx!=params.x) {
                            startX = startY = 0;
                            posx =params.x;
                            return true;
                        }
                        break;
                    default:
                }
                return false;
            }
        });
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
     * 特效配置
     */
    protected void configureFURenderKit() {
        mFUAIKit.loadAIProcessor(DemoConfig.BUNDLE_AI_FACE, FUAITypeEnum.FUAITYPE_FACEPROCESSOR);
        mPropDataFactory.bindCurrentRenderer();
    }

    /**
     * 检测类型
     *
     * @return
     */
    protected FUAIProcessorEnum getFURenderKitTrackingType() {
        return FUAIProcessorEnum.FACE_PROCESSOR;
    }

    private PropDataFactory.PropListener mPropListener = new PropDataFactory.PropListener() {

        @Override
        public void onItemSelected(PropBean bean) {

        }
    };


    /* CameraRenderer 回调*/
    private final OnGlRendererListener mOnGlRendererListener = new OnGlRendererListener() {


        private int width;//数据宽
        private int height;//数据高
        private long mFuCallStartTime = 0; //渲染前时间锚点（用于计算渲染市场）


        private int mCurrentFrameCnt = 0;
        private int mMaxFrameCnt = 10;
        private long mLastOneHundredFrameTimeStamp = 0;
        private long mOneHundredFrameFUTime = 0;

        private int firstLevel = 0;
        private int sceondLevel = 0;

        @Override
        public void onSurfaceCreated() {
            logWrap("back-debug", "------1 onSurfaceCreated-------");
            configureFURenderKit();

        }

        @Override
        public void onSurfaceChanged(int width, int height) {
            logWrap("back-debug", "------2 onSurfaceChanged-------");
        }

        @Override
        public void onRenderBefore(FURenderInputData inputData) {
            if(!detectedFace) {
                logWrap("face-out", " before detectedFace: firstLevel = "+ firstLevel + "sceondLevel = "+sceondLevel);
                inputData.getRenderConfig().setDeviceOrientation(angle[firstLevel]);
                inputData.getRenderConfig().setDeviceOrientation(angle[sceondLevel]);
                lastOrient[0] = angle[firstLevel];
                lastOrient[1] = angle[sceondLevel];
                if (firstLevel < angle.length - 1 ) {
                    firstLevel ++;
                } else if (sceondLevel < angle.length - 1) {
                    firstLevel = 0;
                    sceondLevel ++;
                } else {
                    firstLevel = 0;
                    sceondLevel =0;
                }
            } else {
                inputData.getRenderConfig().setDeviceOrientation(sucOrient[0]);
                inputData.getRenderConfig().setDeviceOrientation(sucOrient[1]);
                logWrap("face-out", " suc detectedFace: sucOrient[0] = "+ sucOrient[0] + "sucOrient[1]= "+sucOrient[1]);
            }
            int device = inputData.getRenderConfig().getDeviceOrientation();
            int input = inputData.getRenderConfig().getInputOrientation();
//            inputData.getRenderConfig().setOutputMatrix(FUTransformMatrixEnum.CCROT90);
            logWrap("back-debug", "------3 onRenderBefore-------device="+device +" input="+input);
            width = inputData.getWidth();
            height = inputData.getHeight();
            mFuCallStartTime = System.nanoTime();
//            if (cameraRenderType == 1) {
//                inputData.setImageBuffer(null);
//            }
            inputData.getRenderConfig().setNeedBufferReturn(true);
            if (fakeInput) {
                byte[] bytes = inputData.getImageBuffer().getBuffer();
                logWrap("xefod", "inputData.printMsg()=" + inputData.printMsg());
                writeByteToCamera(bytes,bytes.length);
//                mMainHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (bytes != null && bytes.length > 0) {
//                            YuvImage yuvimage = new YuvImage(bytes, ImageFormat.NV21, width, height, null);//20、20分别是图的宽度与高度
//                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                            yuvimage.compressToJpeg(new Rect(0, 0, width, height), 80, baos);//80--JPG图片的质量[0-100],100最高
//                            byte[] jdata = baos.toByteArray();
//                            Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
//                            mImage.setImageBitmap(bmp);
//                        } else {
//                            logWrap("xefod", "jdata is empty");
//                        }
//                    }
//                });
            }
        }


        @Override
        public void onRenderAfter(@NonNull FURenderOutputData outputData, @NotNull FURenderFrameData frameData) {
            logWrap("back-debug", "------4 onRenderAfter-------");
            Log.d("My-test","onRenderAfter call");
            recordingData(outputData, frameData.getTexMatrix());
            if (outputData == null || outputData.getTexture() == null || outputData.getTexture().getTexId() <= 0) {
                return;
            }
            if (outputData.getImage() == null) {
                return;
            }
            outputData.printMsg();
            if (!fakeInput) {
                logWrap("xefod", "outputData.printMsg()=" + outputData.printMsg());
                byte[] bytes = outputData.getImage().getBuffer();
                writeByteToCamera(bytes,bytes.length);
//                mMainHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (bytes != null && bytes.length > 0) {
//                            YuvImage yuvimage = new YuvImage(bytes, ImageFormat.NV21, outputData.getImage().getWidth(), outputData.getImage().getHeight(), null);//20、20分别是图的宽度与高度
//                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                            yuvimage.compressToJpeg(new Rect(0, 0, outputData.getImage().getWidth(), outputData.getImage().getHeight()), 80, baos);//80--JPG图片的质量[0-100],100最高
//                            byte[] jdata = baos.toByteArray();
//                            Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
//                            mImage.setImageBitmap(bmp);
//                        } else {
//                            logWrap("xefod", "jdata is empty");
//                        }
//                    }
//                });
            }

        }

        @Override
        public void onDrawFrameAfter() {
            logWrap("back-debug", "------5 onDrawFrameAfter-------");
            trackStatus();
            benchmarkFPS();

        }


        @Override
        public void onSurfaceDestroy() {
            logWrap("back-debug", "------6 onSurfaceDestroy-------");
            mFURenderKit.release();
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
            logWrap("face-out", "trackCount="+trackCount);
            if(trackCount == 1 && !detectedFace) {
                sucOrient = lastOrient;
                detectedFace = true;
                String orient = lastOrient[0]+":"+lastOrient[1];
                PortraitSegmentSource.putCacheOrient(orient);
                logWrap("face-out", "==========find the detecteFace suc[0]="+sucOrient[0]+"  suc[1]"+sucOrient[1]);
                logWrap("face-out", "==========find the detecteFace lastOrient[0]="+lastOrient[0]+"  lastOrient[1]"+lastOrient[1]);
            }
            if (aIProcessTrackStatus != trackCount) {
                aIProcessTrackStatus = trackCount;
            }
        }

        /*渲染FPS日志*/
        private void benchmarkFPS() {
            logWrap("back-debug", "------7 benchmarkFPS-------isShowBenchmark="+isShowBenchmark);
            if (!isShowBenchmark) {
                return;
            }
            mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime;
            if (++mCurrentFrameCnt == mMaxFrameCnt) {
                mCurrentFrameCnt = 0;
                double fps = ((double) mMaxFrameCnt) * 1000000000L / (System.nanoTime() - mLastOneHundredFrameTimeStamp);
                double renderTime = ((double) mOneHundredFrameFUTime) / mMaxFrameCnt / 1000000L;
                mLastOneHundredFrameTimeStamp = System.nanoTime();
                mOneHundredFrameFUTime = 0;
            }
        }

        /*录制保存*/
        private void recordingData(FURenderOutputData outputData, float[] texMatrix) {
            if (outputData == null || outputData.getTexture() == null || outputData.getTexture().getTexId() <= 0) {
                return;
            }
//            if (isRecordingPrepared) {
//                mVideoRecordHelper.frameAvailableSoon(outputData.getTexture().getTexId(), texMatrix, GlUtil.IDENTITY_MATRIX);
//            }
//            if (isTakePhoto) {
//                isTakePhoto = false;
//                mPhotoRecordHelper.sendRecordingData(outputData.getTexture().getTexId(), texMatrix, GlUtil.IDENTITY_MATRIX, outputData.getTexture().getWidth(), outputData.getTexture().getHeight());
//            }
        }
    };

}
