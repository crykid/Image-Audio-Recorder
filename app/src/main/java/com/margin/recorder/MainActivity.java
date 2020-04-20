package com.margin.recorder;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.margin.recorder.recorder.audio.AudioRecorder;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        String file
        AudioRecorder
                .getInstance()
                .duration(30)
                .fileName("新录音")
                .start();
    }
}
