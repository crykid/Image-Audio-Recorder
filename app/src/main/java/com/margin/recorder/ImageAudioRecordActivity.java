package com.margin.recorder;


import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.margin.recorder.recorder.FileUtil;
import com.margin.recorder.recorder.RecorderContants;
import com.margin.recorder.recorder.audio.AudioRecorderIml;
import com.margin.recorder.recorder.audio.IAudioRecorder;
import com.margin.recorder.recorder.image.AutoFitTextureView;
import com.margin.recorder.recorder.image.IOnImageRecorderStatusChangeListener;
import com.margin.recorder.recorder.image.ImageRecorderStatus;
import com.margin.recorder.recorder.image.ImagerRecorderIml;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    Button btnStart, btnStop, btnCancel;
    IAudioRecorder recorder;
    //    RecorderTextureView previewView;
    AutoFitTextureView previewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        previewView = findViewById(R.id.texturev_record_content);
        btnStart = findViewById(R.id.btn_record_start);
        btnStop = findViewById(R.id.btn_record_stop);
        btnCancel = findViewById(R.id.btn_record_cancel);


        recorder = AudioRecorderIml.getInstance();

//        ImagerRecorderIml
//                    .getInstance()
//                    .target(this, previewView)
//                    .directory(Objects.requireNonNull(FileUtil.getFilePath(this, Environment.DIRECTORY_PICTURES, "capture")))
//                    .autoAverage(5, RecorderContants.DEFAULT_SECOND)
//                    .recorderStatusChangeListener(this)
//                    .startPreview();

        btnCancel.setOnClickListener(v -> {
            recorder.cancel();
            changeBtn();
        });

        btnStart.setOnClickListener(v -> {

//            if (recorder.isStart()) {
//                recorder.pause();
//            } else if (recorder.isPause()) {
//                recorder.resume();
//            } else {
//                recorder
//                        .init()
//                        .fileName(getFileName())
//                        .statusChangeListener(staus -> Log.d(TAG, "onCreate: " + staus))
//                        .start();
//            }
//
//            changeBtn();
//            progressBar.setVisibility(View.VISIBLE);

            // TODO: 2020-04-22 权限检查
//
            //开始视频预览
            ImagerRecorderIml
                    .getInstance()
                    .target(this, previewView)
                    .directory(Objects.requireNonNull(FileUtil.getFilePath(this, Environment.DIRECTORY_PICTURES, "capture")))
                    .autoAverage(5, RecorderContants.DEFAULT_SECOND)
                    .recorderStatusChangeListener(this)
                    .startPreview();


        });
        btnStop.setOnClickListener(v -> {
            recorder.stop();
            changeBtn();
        });
    }

    @Override
    public void onChange(ImageRecorderStatus status) {
        if (status == ImageRecorderStatus.READY) {
//            //开始拍照
//            ImagerRecorderIml.getInstance().takePhoto();
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
