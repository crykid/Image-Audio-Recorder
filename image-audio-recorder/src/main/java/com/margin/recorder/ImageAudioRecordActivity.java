package com.margin.recorder;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
public class ImageAudioRecordActivity extends AppCompatActivity implements IOnImageRecorderStatusChangeListener,
        View.OnClickListener {
    private static final String TAG = "RecordActivity";
    public final static String INTENT_IMAGE_PATH = "intent_image_path";
    public final static String INTENT_AUDIO_PATH = "intent_audio_path";
    public final static int RECORDER_REQUESTCODE = 111;


    private Button btnStop, btnRestart, btnCancel, btnLock;
    private IAudioRecorder mAudioRecorder;
    private AutoFitTextureView previewView;
    private TextView tvReadContent;


    private int recordTime;
    private int captureTime;

    ImageRecorderStatus status;


    /**
     * -----------------------------
     * <p>
     * 启动该Activity的方法
     * <p>
     * -----------------------------
     *
     * @param activity
     * @param recordTime  记录的时长
     * @param captureTime 拍照次数
     */
    public static void start(Activity activity, int recordTime, int captureTime, String readContent) {

        activity.startActivityForResult(
                new Intent()
                        .setClass(activity, ImageAudioRecordActivity.class)
                        .putExtra("intent_record_time", recordTime)
                        .putExtra("intent_capture_time", captureTime)
                        .putExtra("intent_read_content", readContent),
                RECORDER_REQUESTCODE
        );
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        //--------------init view-----------------------

        previewView = findViewById(R.id.texturev_record_content);
        btnLock = findViewById(R.id.btn_record_lock);
        btnStop = findViewById(R.id.btn_record_stop);
        btnCancel = findViewById(R.id.btn_record_cancel);
        btnRestart = findViewById(R.id.btn_record_restart);
        tvReadContent = findViewById(R.id.tv_recorder_content);

        btnRestart.setOnClickListener(this);
        btnLock.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        //取消
        btnCancel.setOnClickListener(this);

        //--------------init view-----------------------


        Intent data = getIntent();

        recordTime = data.getIntExtra("intent_record_time", RecorderContants.DEFAULT_SECOND);
        captureTime = data.getIntExtra("intent_capture_time", RecorderContants.DEFAULT_CAPTURE_TIME);
        final String readContent = data.getStringExtra("intent_read_content");

        //阅读的文本
        if (!TextUtils.isEmpty(readContent)) {
            tvReadContent.setText(readContent);
        }


        //--初始化相机
        initCamera();

        //-- 初始化录音
        initAudioRecorder();


    }

    private void initAudioRecorder() {
        //        mAudioRecorder = AudioRecorderIml.getInstance();
        mAudioRecorder = AudioRecorder2Iml.getInstance();

        mAudioRecorder
                .statusChangeListener(status -> {
                    Log.d(TAG, "onCreate: AudioRecorder status : " + status);
                })
//                .period(RecorderContants.DEFAULT_SECOND)
                .period(recordTime)
                .directory(RecorderContants.DIRECTORY_AUDIO)
                .prepare(this);
    }

    private void initCamera() {
        ImageRecorderIml
                .getInstance()
                .target(previewView)
                .directory(RecorderContants.DIRECTORY_CAPTURE)
//                .directory("capture")
                .autoAverage(captureTime, recordTime)
                .recorderStatusChangeListener(this)
                .prepare(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        //--2.开始预览
        ImageRecorderIml.getInstance().startPreview();
        // TODO: 2020/6/8 如果要实现一进来就直接开始记录，则使用这行代码
//        startRecording();
    }


    /**
     * 视频预览状态
     *
     * @param status
     */
    @Override
    public void onStatusChange(ImageRecorderStatus status) {
        this.status = status;
        Log.d(TAG, "onChange: RecorderStatus = " + status);

        //在拍照状态时候，开始录音
        if (status == ImageRecorderStatus.CAPTURE) {

            //--开始录音
            //mAudioRecorder.start();
        }

        if (status == ImageRecorderStatus.STOP) {

            //-- 结束录制，并返回数据
            mAudioRecorder.stop();
            finishAndReturnData();
        }
    }


    /**
     * 结束当前页面，并返回数据
     */
    private void finishAndReturnData() {
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
        cancelRecording();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //停止一切功能
        cancelRecording();
    }


    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.btn_record_restart) {
            //重新开始
            restartRecord();
        } else if (id == R.id.btn_record_lock) {
            startRecording();
        } else if (id == R.id.btn_record_cancel) {

            cancelRecording();

        } else if (id == R.id.btn_record_stop) {

            Toast.makeText(this, "功能暂时未实现", Toast.LENGTH_SHORT).show();
        }
    }

    //---------------------------------------主动功能------------------------------------------------


    /**
     * 开始记录
     */
    private void startRecording() {
        //--开始拍照，可以放在点击事件中，也可以在相机初始化完成后触发
        ImageRecorderIml.getInstance().takePhoto();
        //-- 开始录音
        mAudioRecorder.start();
    }

    /**
     * 重新开始
     */
    private void restartRecord() {
        //重新开始拍照
        ImageRecorderIml.getInstance().restartRecord();
        //重新开始录音
        mAudioRecorder.restartRecord();
    }

    /**
     * 取消记录
     */
    private void cancelRecording() {
        ImageRecorderIml.getInstance().cancel();
        mAudioRecorder.cancel();
    }


    //----------------------------------------主动功能-----------------------------------------------


}
