package com.margin.recorder.recorder;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Created by : mr.lu
 * Created at : 2020-04-20 at 17:34
 * Description:
 */
public class FileUtil {

    private static final String PROVIDER_NAME_BEFORE_Q = "margin_files";
    private static final String TAG = "FileUtil";

    public static String getFilePath(Context context, String type, String directory) {

        File localFile = null;
        try {
            if (Build.VERSION.SDK_INT > 28) {

                //系统目录 + app私有目录 + 自定义目录
                //path=/storage/emulated/0/Android/data/packageName/files/Pictures/camera/1572936803409.jpg

                //getExternalFilesDir传null时会去掉files的一层目录，文件直接创建在根目录下
                localFile = new File(context.getExternalFilesDir(type), directory);
            } else {

                //path=/storage/emulated/packageName/files/Pictures/camera/1572936803409.jpg
                final String localFilePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + File.separator + getAppRootDirectory()
                        + File.separator + PROVIDER_NAME_BEFORE_Q
                        + File.separator + type
                        + File.separator + directory;

                localFile = new File(localFilePath);
            }
            if (!localFile.mkdirs()) {
                Log.d(TAG, "getFilePath: Directory not created");
            }
            final String p = localFile.getAbsolutePath();
            Log.d(TAG, "getFilePath: " + p);
            return p;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getFilePath: ", e);
        }
        return null;
    }

    private static String getAppRootDirectory() {
        return "com.margin.recorder";
    }


    /**
     * 将录音片段删除
     *
     * @param filePaths
     */
    public static void clearFragments(String filePaths) {

        File file = new File(filePaths);
        if (file.exists()) {
            file.delete();
        }
    }

}
