package com.margin.recorder.recorder.image;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import com.margin.recorder.recorder.FileUtil;
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
        PHOTO_ORITATION.append(0, 0);
        PHOTO_ORITATION.append(90, 270);
        PHOTO_ORITATION.append(180, 180);
        PHOTO_ORITATION.append(270, 270);
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
    final private List<String> mFilePaths = new ArrayList<>();


    private Handler mCameraHandler;
    private AutoFitTextureView mPreviewView;

    private CameraManager mCameraManager;

    private CameraDevice mCameraDevice;
    //预览尺寸，用于调整textureView缓存和UI大小
    private Size previewSize, photoSize;
    //摄像头ID，前置or后置
    private String mCameraId;

    //摄像头对话
    private CameraCaptureSession mCameraCaptureSession;

    private CaptureRequest mReviewCaptureRequest;


    private ImageReader mImageReader;
    //拍照的预览Surface
    private Surface readerSurface;
    //屏幕旋转方向，根据屏幕旋转方向旋转surfaceView，不过此处禁止屏幕旋转了，无需此参数
    private int displayRotation;
    //摄像头旋转方向，保存照片的时候根据摄像头旋转方向对照片进行旋转
    private int cameraOritation;


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
    public void prepare() {
        mFilePaths.clear();
        Log.d(TAG, "=== startPreview === ");
        initCamera();
    }

    @Override
    public void startPreview() {


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
        try {
            //获取摄像头旋转方向
            cameraOritation = mCameraManager.getCameraCharacteristics(mCameraId).get(CameraCharacteristics.SENSOR_ORIENTATION);
            Log.d(TAG, " === 摄像头旋转角度 " + cameraOritation);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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

        //1.拍照
        lockFocus();
        //2.生成照片名
        //photoReaderListener 中保存前生成
        //3.保存文件
        //在 photoReaderListener 回调中保存
    }


    @Override
    public List<String> getFiles() {
        final List<String> l = new ArrayList<>(mFilePaths);
        mFilePaths.clear();
        return l;
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
            displayRotation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
            Log.d(TAG, "openCamera: displayRotation = " + displayRotation);
            //将预览尺寸应用到TextureView
            new Handler(mContext.getMainLooper()).post(() -> {
                if (displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_180) {
                    mPreviewView.setAspectRation(previewSize.getHeight(), previewSize.getWidth());
                } else {
                    mPreviewView.setAspectRation(previewSize.getWidth(), previewSize.getHeight());
                }
            });

            configureTransform(mPreviewView.getWidth(), mPreviewView.getHeight());
            //打开摄像头
            mCameraManager.openCamera(mCameraId, mCameraStateCallback, mCameraHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "openCamera: ", e);
        }
    }


    private void startRealPreview() {
        final SurfaceTexture surfaceTexture = mPreviewView.getSurfaceTexture();

        //设置缓冲大小
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

        final Surface previewSurface = new Surface(surfaceTexture);

        try {
            //创建预览请求
            CaptureRequest.Builder previewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //自动对焦
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //预览对象
            previewBuilder.addTarget(previewSurface);
            mReviewCaptureRequest = previewBuilder.build();

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, readerSurface), captureSessionStateCallback, mCameraHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "onOpened: ", e);
        }
    }

    /**
     * @param viewWidth
     * @param viewHeight
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (mPreviewView == null || previewSize == null || mContext == null) {
            return;
        }
        final int rotation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerX());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mPreviewView.setTransform(matrix);

    }


    private void initReaderAndSurface() {
        mImageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(photoReaderListener, null);
        readerSurface = mImageReader.getSurface();
    }


    private void lockFocus() {
        try {
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            /*
            注意：captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, photoRotation)不一定生效！！！
            注意：captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, photoRotation)不一定生效！！！
            注意：captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, photoRotation)不一定生效！！！
            ----
            因为不同的厂商、设备底层不一定做过相应的处理，如三星，魅族部分，并没有对该方法做过处理！这是天大的坑啊
            ----
            图片的旋转还是需要通过读取图片二进制的Exif信息获得旋转角度，然后使用Bitmap旋转！
             */
            //修正旋转角度
            int photoRotation = PHOTO_ORITATION.get(cameraOritation);
            //因为摄像头画面可能是旋转的，所以根据摄像头角度把画面旋转
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, photoRotation);


            captureBuilder.addTarget(readerSurface);
            CaptureRequest phtotoRequest = captureBuilder.build();

            //先停止实时视频预览
            mCameraCaptureSession.stopRepeating();
            //拍照
            mCameraCaptureSession.capture(phtotoRequest, captureSessionCaptureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "lockFocus: ", e);
        }
    }

    private void release() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }

    }


    //------------------------------------surfaceTextureListener/callback-----------------------------------------

    //预览-TextureView 的Surface状态监听，用于TextureView就绪后开始后续任务
    final private TextureView.SurfaceTextureListener surfaceTextureListener = new SurfaceTextureListenerIml() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, " -- onSurfaceTextureAvailable --");
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }
    };


    //预览-摄像头状态回调，就绪后可以创建相机会话
    final private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {

            Log.d(TAG, " -- CameraState : onOpened ");
            initReaderAndSurface();
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


    //预览-相机会话状态回调
    final private CameraCaptureSession.StateCallback captureSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d(TAG, " -- onConfigured: ");
            try {
                mCameraCaptureSession = session;
                //设置循环预览（视频预览的本质就是不停的将每一帧都送到Surface预览）
                mCameraCaptureSession.setRepeatingRequest(mReviewCaptureRequest, null, mCameraHandler);

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
            release();
            Log.e(TAG, " -- onConfigureFailed: ");
        }
    };

    //拍照-捕捉画面成功回调，然后存储成照片
    private final ImageReader.OnImageAvailableListener photoReaderListener = reader -> {
        Log.d(TAG, " -- 画面捕捉完成 开始存储照片 --");
        //保存照片
        Image image = reader.acquireNextImage();
        final String imageName = generateFileName();
        boolean saveSuccess = FileUtil.writeImageToFile(image, imageName);
        if (saveSuccess) {
            mFilePaths.add(imageName);
            Log.d(TAG, " -- 照片存储完成 --");

        }
    };

    //拍照完成监听
    private CameraCaptureSession.CaptureCallback captureSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            Log.d(TAG, " -- onCaptureCompleted: 拍照完成，继续预览  ");
            try {
                //继续预览
                mCameraCaptureSession.setRepeatingRequest(mReviewCaptureRequest, null, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.e(TAG, " -- onCaptureCompleted: 拍照完成，继续预览出错 ", e);
            }
        }
    };

    //------------------------------------surfaceTextureListener/callback-----------------------------------------
}
