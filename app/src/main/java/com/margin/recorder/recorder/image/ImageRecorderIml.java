package com.margin.recorder.recorder.image;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import com.margin.recorder.recorder.RecorderContants;

import java.io.File;
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
public class ImageRecorderIml implements IImageRecorder {

    private static final String TAG = "ImageRecorderIml";

    private static final SparseIntArray PHOTO_ORITATION = new SparseIntArray();

    static {
        PHOTO_ORITATION.append(Surface.ROTATION_0, 90);
        PHOTO_ORITATION.append(Surface.ROTATION_90, 0);
        PHOTO_ORITATION.append(Surface.ROTATION_180, 270);
        PHOTO_ORITATION.append(Surface.ROTATION_270, 180);
    }

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

    //记录器目前状态
    private ImageRecorderStatus mStatus = ImageRecorderStatus.STOP;
    //记录器状态回调
    private IOnImageRecorderStatusChangeListener mRecorderStatusChangeListener;

    /**
     * 注意！不使用的时候一定要release
     */
    private Context mContext;

    //一次生命周期内(从start到finish）拍到的所有照片路径
    private List<String> mFilePaths = new ArrayList<>();


    private Handler mCameraHandler;
    private AutoFitTextureView mPreviewView;

    private CameraManager mCameraManager;

    private CameraDevice mCameraDevice;
    //预览尺寸，用于调整textureView缓存和UI大小
    private Size previewSize, photoSize;
    //摄像头ID，前置or后置
    private String mCameraId;

    //录像or拍照请求
    private CaptureRequest.Builder mPreviewBuilder;

    //摄像头对话
    private CameraCaptureSession mCameraCaptureSession;


    private ImageReader mImageReader;
    //拍照的预览Surface
    private Surface readerSurface;
    private int displayRotation;


    //------------ -------------- --------------- ---------------


    private final static class Holder {
        private final static ImageRecorderIml INSTANCE = new ImageRecorderIml();
    }

    public static ImageRecorderIml getInstance() {
        return Holder.INSTANCE;
    }

    private ImageRecorderIml() {

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

        this.mPreviewView = (AutoFitTextureView) previewView;
        this.mContext = context;

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
    public ImageRecorderIml directory(@NonNull String directory) {

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


        Log.d(TAG, "=== startPreview === ");
        initCamera();

        //如果UI在初始化时候没有第一时间设置上面的回调，textureView就绪时候无法得知，就需要主动打开摄像头
        if (mPreviewView.isAvailable()) {
            openCamera();
        } else {
            mPreviewView.setSurfaceTextureListener(surfaceTextureListener);
        }

    }

    private void initCamera() {
        //初始化摄像头工具类
        RecorderCameraUtil.getInstance().init(mContext);
        mCameraManager = RecorderCameraUtil.getInstance().getCameraManager();
        //1.得到指定的相机；
        mCameraId = RecorderCameraUtil.getInstance().getFrontCameraId();
        //2.获得相机输出参数;
        List<Size> cameraOutputSizes = RecorderCameraUtil.getInstance().getCameraOutputSizes(mCameraId, SurfaceTexture.class);
        //3.得到与屏幕匹配的尺寸；
        previewSize = cameraOutputSizes.get(0);
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
        final String name = mFormat.format(new Date());
        return mImagesDirectory + File.separator + name + ".jpg";
    }


    private void closeCamera() {
        mCameraDevice.close();
    }


    /**
     * surface ready的时候打开Camera
     */
    @SuppressLint("MissingPermission")
    private void openCamera() {

        Log.d(TAG, " === openCamera === ");
        try {
            //将预览尺寸应用到TextureView
            new Handler(mContext.getMainLooper()).post(() -> {
                mPreviewView.setAspectRation(previewSize.getWidth(), previewSize.getHeight());
            });

            //打开摄像头
            mCameraManager.openCamera(mCameraId, mCameraStateCallback, mCameraHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "openCamera: ", e);
        }
    }


    private void startRealPreview() {
        SurfaceTexture surfaceTexture = mPreviewView.getSurfaceTexture();

        //应用预览尺寸
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

        Surface previewSurface = new Surface(surfaceTexture);

        try {
            //创建预览请求
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //自动对焦
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //预览对象
            mPreviewBuilder.addTarget(previewSurface);
            //创建预览请求对话
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), captureSessionStateCallback, mCameraHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "onOpened: ", e);
        }
    }


    private void lockFocus() {

    }


    private void stopPreview() {

        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

    }
    //------------------------------------surfaceTextureListener/callback-----------------------------------------

    //TextureView 的Surface状态监听，用于TextureView就绪后开始后续任务
    final private TextureView.SurfaceTextureListener surfaceTextureListener = new SurfaceTextureListenerIml() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, " === onSurfaceTextureAvailable ===  width = " + width + " height = " + height);
            openCamera();
        }
    };

    //摄像头状态回调，就绪后可以创建相机会话
    final private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {

            Log.d(TAG, " -- CameraState : onOpened ");

            mCameraDevice = camera;

            //初始化预览
            startRealPreview();

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, " -- CameraState : onDisconnected ");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.d(TAG, " -- CameraState : onError ");
        }
    };


    //相机会话状态回调
    final private CameraCaptureSession.StateCallback captureSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            CaptureRequest request = mPreviewBuilder.build();
            Log.d(TAG, " -- onConfigured: ");
            try {
                mCameraCaptureSession = session;
                // 设置成预览
                session.setRepeatingRequest(request, null, mCameraHandler);

                //更新预览状态
                mStatus = ImageRecorderStatus.READY;
                if (mRecorderStatusChangeListener != null) {
                    mRecorderStatusChangeListener.onChange(mStatus);
                }

            } catch (CameraAccessException e) {
                Log.e(TAG, " -- onConfigured: ", e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            stopPreview();
            Log.e(TAG, " -- onConfigureFailed: ");
        }
    };

    //------------------------------------surfaceTextureListener/callback-----------------------------------------
}
