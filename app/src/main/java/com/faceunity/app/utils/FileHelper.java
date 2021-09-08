package com.faceunity.app.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Android Studio.
 * User: liheng
 * Date: 2021/8/28
 * Time: 9:56
 */
public class FileHelper {
    public static final String TAG = "--FileUtils--";

    public static byte[] readFileFromAssets(Context context, String groupPath, String filename) {
        byte[] buffer = null;
        AssetManager am = context.getAssets();
        try {
            InputStream inputStream = null;
            if (!TextUtils.isEmpty(groupPath)) {
                inputStream = am.open(groupPath + "/" + filename);
            } else {
                inputStream = am.open(filename);
            }

            int length = inputStream.available();
            Log.d(TAG, "readFileFromAssets length:" + length);
            buffer = new byte[length];
            inputStream.read(buffer);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return buffer;
    }

    //根据指定的二进制流字符串保存文件并返回保存路径
    public static boolean saveFileByBinary(byte[] byteFile, String savaFile) {
        //准备拼接新的文件名
        File file = new File(savaFile);
        if (file.exists()) {    //如果目标文件已经存在
            file.delete();    //则删除旧文件
        }

        try (InputStream is = new ByteArrayInputStream(byteFile);
             FileOutputStream os = new FileOutputStream(file)) {
            byte[] b = new byte[1024];
            int len = 0;
            //开始读取
            while ((len = is.read(b)) != -1) {
                os.write(b, 0, len);
            }
            //完毕关闭所有连接
            is.close();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

}
