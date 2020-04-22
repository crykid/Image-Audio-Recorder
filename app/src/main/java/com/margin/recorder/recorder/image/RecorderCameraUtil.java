package com.margin.recorder.recorder.image;

import android.support.annotation.NonNull;
import android.util.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by : mr.lu
 * Created at : 2020-04-22 at 16:12
 * Description:
 */
public class RecorderCameraUtil {

    private static final String TAG = "RecorderCameraUtil";

    public static Size getOptimalSize(Size[] sizes, int width, int height) {
        //1080*1692
        ArrayList<Size> sizeList = new ArrayList<>();
        for (Size option : sizes) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                } else {
                    if (option.getWidth() > height && option.getHeight() > width) {
                        sizeList.add(option);
                    }
                }
            }
        }

        if (sizeList.size() > 0) {
            return (Size) Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size o1, Size o2) {

                    return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                }
            });
        }

        return sizes[0];

    }

}
