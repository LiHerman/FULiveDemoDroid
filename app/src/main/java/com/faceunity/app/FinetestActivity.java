/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.faceunity.app;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import com.faceunity.app.utils.FileHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

public class FinetestActivity extends AppCompatActivity {
    public static final String TAG = "--FinetestActivity--";
    int hour = 0;
    int minute = 0;
    int second = 0;
    TextView tickView;
    SurfaceView surfaceview1, surfaceview2;
    SurfaceHolder surfaceholder1, surfaceholder2;
    private Camera camera1 = null, camera2;
    Camera.Parameters parameters;
    ImageView mImage;

//    public native String stringFromJNI();
//
//    public native void startTicks();
//
//    public native void StopTicks();

    public native boolean writeByteToCamera(byte[] data, int length);
    public native boolean writeFileToCamera(String filePath);
    String path;
    SurfaceTexture surfacetexture;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_fine_test);
        tickView = (TextView) findViewById(R.id.tickView);
        surfaceview1 = (SurfaceView) findViewById(R.id.surfaceview1);
        surfaceview2 = (SurfaceView) findViewById(R.id.surfaceview2);
        surfaceholder1 = surfaceview1.getHolder();
        surfaceholder1.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceholder1.addCallback(new surfaceholderCallbackBack());


//        surfaceholder2 = surfaceview2.getHolder();
//        surfaceholder2.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        surfaceholder2.addCallback(new surfaceholderCallbackFont());
        surfacetexture=new SurfaceTexture(10);

        mImage = (ImageView) findViewById(R.id.outputImg);

//        justOpen();
    }


    private void justOpen() {
        int cameraCount = Camera.getNumberOfCameras();
        Log.e(TAG, "cameraCount=" + cameraCount);
        if (cameraCount > 0) {
            camera1 = Camera.open(0);
            try {
                // 设置预览监听
//                camera1.setPreviewDisplay(view.getHolder());
                camera1.setPreviewTexture(surfacetexture);
                Camera.Parameters parameters = camera1.getParameters();

                if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    parameters.set("orientation", "portrait");
                    camera1.setDisplayOrientation(90);
                    parameters.setRotation(90);
                } else {
                    parameters.set("orientation", "landscape");
                    camera1.setDisplayOrientation(0);
                    parameters.setRotation(0);
                }
                camera1.setParameters(parameters);
                // 启动摄像头预览
                camera1.startPreview();
                System.out.println("camera.startpreview");


                    camera1.setPreviewCallback(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] bytes, Camera camera) {

                            if (bytes != null && bytes.length > 0) {
                                Camera.Size size = camera.getParameters().getPreviewSize(); //获取预览大小
                                final int w = size.width;  //宽度
                                final int h = size.height;
                                Log.e(TAG, "bytes.lenth=" + bytes.length);
                                YuvImage yuvimage=new YuvImage(bytes, ImageFormat.NV21, w,h, null);//20、20分别是图的宽度与高度
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                yuvimage.compressToJpeg(new Rect(0, 0,w, h), 80, baos);//80--JPG图片的质量[0-100],100最高
                                byte[] jdata = baos.toByteArray();

                                Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
//                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                mImage.setImageBitmap(bmp);
                            } else {
                                Log.e(TAG, "bytes.lenthg is null");
                            }
                        }
                    });

            } catch (Exception e) {
                e.printStackTrace();
                camera1.release();
                System.out.println("camera.release");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        hour = minute = second = 0;
//        ((TextView) findViewById(R.id.hellojniMsg)).setText(stringFromJNI());
//        startTicks();
    }

    @Override
    public void onPause() {
        super.onPause();
//        StopTicks();
    }

    public void onFileInput(View view) {
//        byte[] data = FileHelper.readFileFromAssets(this, null, "test1.jpg");
        boolean suc = writeFileToCamera(path);
        Log.d(TAG, "onFileInput path= " + path +" result="+suc);
    }

    public void onByteInput(View view) {
        byte[] data = FileHelper.readFileFromAssets(this, null, "test2.yuv");
        boolean suc = writeByteToCamera(data, data.length);
        Log.d(TAG, "writeByteToCamera result= " + suc);
    }

    /*
     * A function calling from JNI to update current timer
     */
    @Keep
    private void updateTimer() {
        ++second;
        if (second >= 60) {
            ++minute;
            second -= 60;
            if (minute >= 60) {
                ++hour;
                minute -= 60;
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String ticks = "" + FinetestActivity.this.hour + ":" +
                        FinetestActivity.this.minute + ":" +
                        FinetestActivity.this.second;
                FinetestActivity.this.tickView.setText(ticks);
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (camera1 != null) {
            camera1.stopPreview();
            camera1.release();
        }
        if (camera2 != null) {
            camera2.stopPreview();
            camera2.release();
        }
    }


    /**
     * 后置摄像头回调
     */
    class surfaceholderCallbackBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // 获取camera对象
            int cameraCount = Camera.getNumberOfCameras();
            Log.e(TAG, "cameraCount=" + cameraCount);
            if (cameraCount > 0) {
                camera1 = Camera.open(0);
                try {
                    // 设置预览监听
                    camera1.setPreviewDisplay(holder);
//                    camera1.setPreviewTexture(surfacetexture);
                    Camera.Parameters parameters = camera1.getParameters();

                    if (FinetestActivity.this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                        parameters.set("orientation", "portrait");
                        camera1.setDisplayOrientation(90);
                        parameters.setRotation(90);
                    } else {
                        parameters.set("orientation", "landscape");
                        camera1.setDisplayOrientation(0);
                        parameters.setRotation(0);
                    }
                    camera1.setParameters(parameters);
                    // 启动摄像头预览
                    camera1.startPreview();
                    System.out.println("camera.startpreview");

                    camera1.setPreviewCallback(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] bytes, Camera camera) {

                            if (bytes != null && bytes.length > 0) {
                                Camera.Size size = camera.getParameters().getPreviewSize(); //获取预览大小
                                final int w = size.width;  //宽度
                                final int h = size.height;
                                Log.e(TAG, "camera1 bytes.lenth w=" + w +" h="+h);
                                YuvImage yuvimage=new YuvImage(bytes, ImageFormat.NV21, w,h, null);//20、20分别是图的宽度与高度
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                yuvimage.compressToJpeg(new Rect(0, 0,w, h), 80, baos);//80--JPG图片的质量[0-100],100最高
                                byte[] jdata = baos.toByteArray();

                                Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
//                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                mImage.setImageBitmap(bmp);
                            } else {
                                Log.e(TAG, "bytes.lenthg is null");
                            }
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    camera1.release();
                    System.out.println("camera.release");
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            camera1.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        initCamera();// 实现相机的参数初始化
                        camera.cancelAutoFocus();// 只有加上了这一句，才会自动对焦。
                    }
                }
            });

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

        // 相机参数的初始化设置
        private void initCamera() {
            parameters = camera1.getParameters();
            parameters.setPictureFormat(PixelFormat.JPEG);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 1连续对焦
            setDispaly(parameters, camera1);
            camera1.setParameters(parameters);
            camera1.startPreview();
            camera1.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
        }

        // 控制图像的正确显示方向
        private void setDispaly(Camera.Parameters parameters, Camera camera) {
            if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
                setDisplayOrientation(camera, 90);
            } else {
                parameters.setRotation(90);
            }

        }

        // 实现的图像的正确显示
        private void setDisplayOrientation(Camera camera, int i) {
            Method downPolymorphic;
            try {
                downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[]{int.class});
                if (downPolymorphic != null) {
                    downPolymorphic.invoke(camera, new Object[]{i});
                }
            } catch (Exception e) {
                Log.e("Came_e", "图像出错");
            }
        }
    }

    class surfaceholderCallbackFont implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // 获取camera对象
            int cameraCount = Camera.getNumberOfCameras();
            if (cameraCount == 2) {
                camera2 = Camera.open(1);
            }
            try {
                // 设置预览监听
//                camera2.setPreviewDisplay(holder);
                camera2.setPreviewTexture(surfacetexture);
                Camera.Parameters parameters = camera2.getParameters();

                if (FinetestActivity.this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    parameters.set("orientation", "portrait");
                    camera2.setDisplayOrientation(90);
                    parameters.setRotation(90);
                } else {
                    parameters.set("orientation", "landscape");
                    camera2.setDisplayOrientation(0);
                    parameters.setRotation(0);
                }
                camera2.setParameters(parameters);
                // 启动摄像头预览
                camera2.startPreview();
                System.out.println("camera.startpreview");

            } catch (IOException e) {
                e.printStackTrace();
                camera2.release();
                System.out.println("camera.release");
            }

            camera2.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] bytes, Camera camera) {
                    if (bytes != null && bytes.length > 0) {
                        Log.e(TAG, "camera2 bytes.lenthg ="+bytes.length);
                        writeByteToCamera(bytes,bytes.length);
                    } else {
                        Log.e(TAG, "bytes.lenthg is null");
                    }
                }
            });
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            camera2.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        parameters = camera2.getParameters();
                        parameters.setPictureFormat(PixelFormat.JPEG);
//                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 1连续对焦
                        //*****************
//                        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//                        Display display = wm.getDefaultDisplay();
//
//                        Camera.Parameters parameters = camera.getParameters();// 得到摄像头的参数
//
//                        parameters.setPreviewSize(display.getWidth(),display.getHeight());
//
//                        parameters.setPictureSize(display.getHeight(),display.getWidth());
                        //*****************
                        setDispaly(parameters, camera2);
                        camera2.setParameters(parameters);
                        camera2.startPreview();
                        camera2.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
                        camera.cancelAutoFocus();// 只有加上了这一句，才会自动对焦。
                    }
                }
            });

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

        // 控制图像的正确显示方向
        private void setDispaly(Camera.Parameters parameters, Camera camera) {
            if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
                setDisplayOrientation(camera, 90);
            } else {
                parameters.setRotation(90);
            }

        }

        // 实现的图像的正确显示
        private void setDisplayOrientation(Camera camera, int i) {
            Method downPolymorphic;
            try {
                downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[]{int.class});
                if (downPolymorphic != null) {
                    downPolymorphic.invoke(camera, new Object[]{i});
                }
            } catch (Exception e) {
                Log.e("Came_e", "图像出错");
            }
        }
    }
}
