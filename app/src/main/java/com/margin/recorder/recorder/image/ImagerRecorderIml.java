package com.margin.recorder.recorder.image;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.margin.recorder.recorder.RecorderContants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by : mr.lu
 * Created at : 2020-04-22 at 10:32
 * Description: 摄像头预览-拍照
 * <p>
 * * --------------------- *
 * | 注：该模块不负责权限的检查！|
 * | 注：该模块不负责权限的检查！|
 * | 注：该模块不负责权限的检查！|
 * * --------------------- *
 */
public class ImagerRecorderIml implements IImageRecorder {

    private static final String TAG = "ImagerRecorderIml";
    //拍照策略，默认是--自动随机--的
    private Strategy mStrategy = Strategy.AUTO_RANDOM;

    //记录总时间
    private int mPeriod = RecorderContants.DEFAULT_SECOND;

    //当拍照策略为自动（AUTO_AVERATE,AUTO_RANDOM）时有效
    private int time = -1;

    //时间格式
    private DateFormat mFormat;

    //拍照后文件存储路径
    private String mImagesDirectory;

    private IOnImageRecorderStatusChangeListener mRecorderStatusChangeListener;


    /**
     * 注意！不使用的时候一定要release
     */
    private Context mContext;

    //一次生命周期内(从start到finish）拍到的所有照片路径
    private List<String> mFilePaths = new ArrayList<>();


    private Handler mCameraHandler;
    private TextureView mPreviewView;

    private CameraManager mCameraManager;

    private CameraDevice mCameraDevice;
    private Size previewSize;
    private String mCameraId;

    private CaptureRequest.Builder mPreviewBuilder;

    private ImageRecorderStatus mStatus = ImageRecorderStatus.STOP;


    //------------ -------------- --------------- ---------------


    private final static class Holder {
        private final static ImagerRecorderIml INSTANCE = new ImagerRecorderIml();
    }

    public static ImagerRecorderIml getInstance() {
        return Holder.INSTANCE;
    }

    private ImagerRecorderIml() {

        /*创建一个Thread供Camera运行使用，shiyong HandelrThread而不是Thread是因为HandlerThread给我们创建了Looper
         *不用我们自己创建
         */
        HandlerThread handlerThread = new HandlerThread("ImageRecorder");
        handlerThread.start();

        mCameraHandler = new Handler(handlerThread.getLooper());


    }


    //------------ -------------- --------------- ---------------

    @Override
    public IImageRecorder target(@NonNull Activity context, @NonNull TextureView previewView) {

        this.mPreviewView = previewView;
        this.mContext = context;
        Log.d(TAG, "init: *** 请在使用完成后清理 Context ！！！***");
        Log.d(TAG, "init: *** 请在使用完成后清理 Context ！！！***");
        Log.d(TAG, "init: *** please release the Context after the mission is completed  ！！！***");

        return this;
    }


    /**
     * 手动拍照，没有次数和时间限制(时间设置到允许的最大）
     *
     * @return
     */
    @Override
    public IImageRecorder hand() {
        mStrategy = Strategy.HAND;
        this.mPeriod = RecorderContants.MAX_SECOND;
        return this;
    }

    @Override
    public IImageRecorder autoRandom(int time, int period) {

        if (mPeriod <= 0) {
            throw new IllegalArgumentException("The Recording period or Recording time can only be greater than 0 !");
        } else {
            this.mPeriod = period;
        }
        this.mStrategy = Strategy.AUTO_RANDOM;
        return this;
    }

    @Override
    public IImageRecorder autoAverage(int time, int period) {
        if (mPeriod <= 0 || time <= 0) {
            throw new IllegalArgumentException("The Recording period or Recording time can only be greater than 0 !");
        } else {
            this.mPeriod = period;
        }
        this.mStrategy = Strategy.AUTO_AVERAGE;
        return this;
    }


    @Override
    public ImagerRecorderIml directory(@NonNull String directory) {

        this.mImagesDirectory = directory;

        return this;
    }

    @Override
    public IImageRecorder recorderStatusChangeListener(@NonNull IOnImageRecorderStatusChangeListener listener) {
        this.mRecorderStatusChangeListener = listener;
        return this;
    }

    @Override
    public void startPreview() {


        //初始化TextureView，设置监听
        final TextureView.SurfaceTextureListener listener = new SurfaceTextureListenerIml() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

                openCamera(width, height);
            }
        };
        mPreviewView.setSurfaceTextureListener(listener);

        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

    }

    @Override
    public void takePhoto() {

        this.mStatus = ImageRecorderStatus.START;
        if (mRecorderStatusChangeListener != null) {
            mRecorderStatusChangeListener.onChange(mStatus);
        }
        if (mFormat == null) {
            mFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        }
        switch (mStrategy) {
            case HAND:
                takeSinglePhotoFlow();
                break;
            case AUTO_AVERAGE:
                averageTakePhoto();
                break;
            case AUTO_RANDOM:
                randomTakePhoto();
                break;
        }
    }

    @Override
    public void finish() {
        mStatus = ImageRecorderStatus.STOP;
        if (mRecorderStatusChangeListener != null) {
            mRecorderStatusChangeListener.onChange(mStatus);
        }
        closeCamera();
        mContext = null;
    }

    private void averageTakePhoto() {


        //1.生成时间规则
        //2.按照规则派发
        takeSinglePhotoFlow();
    }

    private void randomTakePhoto() {
        //1.生成时间规则
        //2.按照规则派发
        takeSinglePhotoFlow();
    }

    private void takeSinglePhotoFlow() {
        //1.生成照片名
        final String localFileName = generateFileName();
        //2.拍照
        lockFocus();
        //3.保存文件
        //4.拍照完成
    }


    @Override
    public List<String> getFiles() {
        return null;
    }

    private String generateFileName() {

        return mFormat.format(new Date());
    }


    private void closeCamera() {
        mCameraDevice.close();
    }


    //更新来自摄像头的数据
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {

            Log.d(TAG, "onOpened: ");

            mCameraDevice = camera;
            startRealPreview();


        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    /**
     * surface ready的时候打开Camera
     *
     * @param width  surface的宽
     * @param height surface的高
     */
    @SuppressLint("MissingPermission")
    private void openCamera(int width, int height) {
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                //描述相机设备的属性类
                final CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
                //获取前置or后置的属性
                final Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                //使用前置摄像头
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    final StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map != null) {
                        previewSize = RecorderCameraUtil.getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);

                        mCameraId = cameraId;
                        return;
                    }
                }
            }
            //打开摄像头
            mCameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "openCamera: ", e);
        }
    }


    private void startRealPreview() {
        SurfaceTexture surfaceTexture = mPreviewView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //自动对焦
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            //预览对象
            mPreviewBuilder.addTarget(previewSurface);
            //添加摄像头状态回调
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), captureSessionStateCallback, mCameraHandler);

            mStatus = ImageRecorderStatus.READY;
            if (mRecorderStatusChangeListener != null) {
                mRecorderStatusChangeListener.onChange(mStatus);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "onOpened: ", e);
        }
    }

    CameraCaptureSession mCameraCaptureSession;
    //接收摄像头捕获的状态的更新
    private CameraCaptureSession.StateCallback captureSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            CaptureRequest request = mPreviewBuilder.build();
            try {
                mCameraCaptureSession = session;
                // 设置成预览
                session.setRepeatingRequest(request, null, mCameraHandler);

            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.e(TAG, "onConfigured: ", e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            stopPreview();
        }
    };

    private void lockFocus() {
        try {
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            mCameraCaptureSession.capture(mPreviewBuilder.build(), captureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 拍照回调
     */
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            try {
                session.setRepeatingRequest(request, null, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    };

    private void stopPreview() {
        // TODO: 2020/4/22 停止预览
//        if (mCapterSeesion != null) {
//            mCapterSeesion.close();
//            mCapterSeesion = null;
//        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

    }
}
