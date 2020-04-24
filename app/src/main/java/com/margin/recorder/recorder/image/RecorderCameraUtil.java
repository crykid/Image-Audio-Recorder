package com.margin.recorder.recorder.image;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Size;

import com.margin.recorder.recorder.IRecorderManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by : mr.lu
 * Created at : 2020-04-22 at 16:12
 * Description:
 */
public class RecorderCameraUtil implements IRecorderManager {

    private static final String TAG = "RecorderCameraUtil";

    private Context mContext;
    private CameraManager cameraManager;

    private final static class Holder {
        private final static RecorderCameraUtil INSTANCE = new RecorderCameraUtil();
    }

    public static RecorderCameraUtil getInstance() {
        return Holder.INSTANCE;
    }

    private RecorderCameraUtil() {
    }

    public void init(Context context) {
        this.mContext = context;
        cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public String getFrontCameraId() {
        return getCameraId(true);
    }

    public String getBackCameraId() {
        return getCameraId(false);
    }

    /**
     * 获取cameraId
     *
     * @param usefront true-前置，false-后置
     * @return
     */
    private String getCameraId(boolean usefront) {

        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (usefront) {
                    if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        return cameraId;
                    }
                } else {
                    if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        return cameraId;
                    }
                }


            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "getCameraId: ", e);
        }

        return null;
    }

    /**
     * 根据输出类获取指定相机的输出尺寸列表
     *
     * @param cameraId
     * @param clz      输出类
     * @return
     */
    public List<Size> getCameraOutputSizes(String cameraId, Class clz) {
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            List<Size> sizes = Arrays.asList(configs.getOutputSizes(clz));
            Collections.sort(sizes, new Comparator<Size>() {
                @Override
                public int compare(Size o1, Size o2) {
                    return o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight();
                }
            });
            Collections.reverse(sizes);
            return sizes;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * 释放相机资源
     *
     * @param cameraDevice
     */
    public void releaseCamera(CameraDevice cameraDevice) {
        assert cameraDevice != null;
        cameraDevice.close();
        cameraDevice = null;
    }


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

    @Override
    public void release() {
        mContext = null;
    }

}
