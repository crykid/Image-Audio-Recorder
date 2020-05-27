package com.margin.recorder;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.margin.recorder.recorder.RecorderContants;
import com.margin.recorder.recorder.audio.IAudioRecorder;
import com.margin.recorder.recorder.image.AudioRecorder2Iml;
import com.margin.recorder.recorder.image.AutoFitTextureView;
import com.margin.recorder.recorder.image.IOnImageRecorderStatusChangeListener;
import com.margin.recorder.recorder.image.ImageRecorderIml;
import com.margin.recorder.recorder.image.ImageRecorderStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by : mr.lu
 * Created at : 2020-04-22 at 14:09
 * Description:  使用 start 方法
 * * --------------------- *
 * | 注：该模块不负责权限的检查！|
 * | 注：该模块不负责权限的检查！|
 * | 注：该模块不负责权限的检查！|
 * * --------------------- *
 * <p>
 * 由相机控制时间，录音不负责控制时间。录音的工作状态由相机状态决定
 */
public class ImageAudioRecordActivity extends AppCompatActivity implements IOnImageRecorderStatusChangeListener {
    private static final String TAG = "RecordActivity";
    public final static String INTENT_IMAGE_PATH = "intent_image_path";
    public final static String INTENT_AUDIO_PATH = "intent_audio_path";


    private Button btnStart, btnStop, btnCancel, btnLock;
    private IAudioRecorder mAudioRecorder;
    private AutoFitTextureView previewView;


    /**
     * -----------------------------
     * <p>
     * 启动该Activity的方法
     * <p>
     * -----------------------------
     *
     * @param context
     * @param recordTime  记录的时长
     * @param captureTime 拍照次数
     */
    public static void start(Context context, int recordTime, int captureTime) {

        context.startActivity(
                new Intent()
                        .setClass(context, ImageAudioRecordActivity.class)
                        .putExtra("intent_record_time", recordTime)
                        .putExtra("intent_capture_time", captureTime));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        previewView = findViewById(R.id.texturev_record_content);
        btnStart = findViewById(R.id.btn_record_start);
        btnLock = findViewById(R.id.btn_record_lock);
        btnStop = findViewById(R.id.btn_record_stop);
        btnCancel = findViewById(R.id.btn_record_cancel);


        Intent data = getIntent();

        final int recordTime = data.getIntExtra("intent_record_time", RecorderContants.DEFAULT_SECOND);
        final int captureTime = data.getIntExtra("intent_capture_time", RecorderContants.DEFAULT_CAPTURE_TIME);
//        final String directory = data.getStringExtra("intent_save_directory");

        // TODO: 2020-04-22 权限检查


//        mAudioRecorder = AudioRecorderIml.getInstance();
        mAudioRecorder = AudioRecorder2Iml.getInstance();

        //--1.初始化相机
        ImageRecorderIml
                .getInstance()
                .target(previewView)
                .directory(RecorderContants.DIRECTORY_CAPTURE)
//                .directory("capture")
                .autoAverage(captureTime, recordTime)
                .recorderStatusChangeListener(this)
                .prepare(this);

        //--初始化录音
        mAudioRecorder
                .statusChangeListener(status -> {
                    Log.d(TAG, "onCreate: AudioRecorder status : " + status);
                })
//                .period(RecorderContants.DEFAULT_SECOND)
                .period(recordTime)
                .directory(RecorderContants.DIRECTORY_AUDIO)
                .prepare(this);


        btnCancel.setOnClickListener(v -> {
            ImageRecorderIml.getInstance().cancel();
            mAudioRecorder.cancel();


        });

        btnStart.setOnClickListener(v -> {

            ImageRecorderIml.getInstance().takePhoto();

        });

        btnLock.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: RecorderStatus = " + status);
            //--3.开始拍照，可以放在点击事件中，也可以在相机初始化完成后触发
            ImageRecorderIml.getInstance().takePhoto();


        });
        btnStop.setOnClickListener(v -> {
            mAudioRecorder.stop();
        });
    }

    ImageRecorderStatus status;

    @Override
    protected void onResume() {
        super.onResume();
        //--2.开始预览
        ImageRecorderIml.getInstance().startPreview();
    }


    /**
     * 视频预览状态
     *
     * @param status
     */
    @Override
    public void onChange(ImageRecorderStatus status) {
        this.status = status;
        Log.d(TAG, "onChange: RecorderStatus = " + status);

        //在拍照状态时候，开始录音
        if (status == ImageRecorderStatus.CAPTURE) {
            //--开始录音
            mAudioRecorder
                    .start();
        }
        if (status == ImageRecorderStatus.STOP) {
            mAudioRecorder.stop();


            finishAndReturnData();

        }
    }

    /**
     * 结束当前页面，并返回数据
     */
    private void finishAndReturnData() {

        //返回结果！！
        //录音文件
        String audioFilePath = mAudioRecorder.getAudio();
        //拍摄的所有照片
        List<String> imagePaths = ImageRecorderIml.getInstance().getFiles();
        ArrayList<String> realPahts = new ArrayList<>(imagePaths);


        Intent data = new Intent();

        //记得拿到文件路径首先判断文件是否存在
        data.putExtra(INTENT_IMAGE_PATH, realPahts);
        data.putExtra(INTENT_AUDIO_PATH, audioFilePath);

        setResult(RESULT_OK, data);
        if (BuildConfig.DEBUG) {
            Toast.makeText(this, "完成自动返回客户app", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
        //停止一切功能
        ImageRecorderIml.getInstance().cancel();
        mAudioRecorder.cancel();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //停止一切功能
        ImageRecorderIml.getInstance().cancel();
        mAudioRecorder.cancel();
    }


}
