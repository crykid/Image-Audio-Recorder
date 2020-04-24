package com.margin.recorder;


import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import com.margin.recorder.recorder.FileUtil;
import com.margin.recorder.recorder.RecorderContants;
import com.margin.recorder.recorder.audio.AudioRecorderIml;
import com.margin.recorder.recorder.audio.IAudioRecorder;
import com.margin.recorder.recorder.image.AutoFitTextureView;
import com.margin.recorder.recorder.image.IOnImageRecorderStatusChangeListener;
import com.margin.recorder.recorder.image.ImageRecorderIml;
import com.margin.recorder.recorder.image.ImageRecorderStatus;

import java.util.Objects;

/**
 * Created by : mr.lu
 * Created at : 2020-04-22 at 14:09
 * Description:
 * * --------------------- *
 * | 注：该模块不负责权限的检查！|
 * | 注：该模块不负责权限的检查！|
 * | 注：该模块不负责权限的检查！|
 * * --------------------- *
 */
public class ImageAudioRecordActivity extends AppCompatActivity implements IOnImageRecorderStatusChangeListener {
    private static final String TAG = "RecordActivity";
    Button btnStart, btnStop, btnCancel, btnLock;
    IAudioRecorder recorder;
    //    RecorderTextureView previewView;
    AutoFitTextureView previewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        previewView = findViewById(R.id.texturev_record_content);
        btnStart = findViewById(R.id.btn_record_start);
        btnLock = findViewById(R.id.btn_record_lock);
        btnStop = findViewById(R.id.btn_record_stop);
        btnCancel = findViewById(R.id.btn_record_cancel);


        recorder = AudioRecorderIml.getInstance();

        btnCancel.setOnClickListener(v -> {
            recorder.cancel();


        });

        btnStart.setOnClickListener(v -> {


            // TODO: 2020-04-22 权限检查
//
            //开始视频预览
            ImageRecorderIml
                    .getInstance()
                    .target(this, previewView)
                    .directory(Objects.requireNonNull(FileUtil.getFilePath(this, Environment.DIRECTORY_PICTURES, "capture")))
                    .autoAverage(5, RecorderContants.DEFAULT_SECOND)
                    .recorderStatusChangeListener(this)
                    .prepare();
            ImageRecorderIml.getInstance().startPreview();


        });

        btnLock.setOnClickListener(v -> {
            if (status == ImageRecorderStatus.READY) {
                ImageRecorderIml.getInstance().takePhoto();

            }

        });
        btnStop.setOnClickListener(v -> {
            recorder.stop();
            changeBtn();
        });
    }

    ImageRecorderStatus status;

    @Override
    public void onChange(ImageRecorderStatus status) {
        this.status = status;
        if (status == ImageRecorderStatus.READY) {

//            //开始拍照
//            ImageRecorderIml.getInstance().takePhoto();
//            //开始录音
//            AudioRecorderIml
//                    .getInstance()
//                    .init()
//                    .fileName(getFileName())
//                    .period(RecorderContants.DEFAULT_SECOND)
//                    .statusChangeListener(s -> Log.d(TAG, "onCreate: " + s))
//                    .start();
        }
    }

    private void changeBtn() {
        if (recorder.isStart()) {
            btnStart.setText("pause");
        }
        if (recorder.isPause()) {
            btnStart.setText("go on");
        }
        if (recorder.isStop()) {
            btnStart.setText("start");
        }
    }




}
