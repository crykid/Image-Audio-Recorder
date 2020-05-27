package com.margin.recorder.recorder;

import android.content.Context;
import android.util.Log;
import android.util.Size;

/**
 * Created by : mr.lu
 * Created at : 2020-04-23 at 10:30
 * Description:
 */
public class ScreenUtil {
    private static final String TAG = "ScreenUtil";

    public static Size getScreenSize(Context context) {
        int widthPixels = context.getResources().getDisplayMetrics().widthPixels;
        int heightPixels = context.getResources().getDisplayMetrics().heightPixels;
        Log.d(TAG, "getScreenSize: " + widthPixels + " * " + heightPixels);
        return new Size(widthPixels, heightPixels);
    }


}
