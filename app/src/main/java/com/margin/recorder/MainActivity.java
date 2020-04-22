package com.margin.recorder;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.margin.recorder.recorder.demo.CameraPreview;

public class MainActivity extends AppCompatActivity {

    CameraPreview cameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraView = findViewById(R.id.cameraView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.onResume(this);
    }

    @Override
    protected void onPause() {
        cameraView.onPause();
        super.onPause();
    }

    public void takePic(View view) {
        cameraView.takePicture();
    }

    public void jump(View view) {
        startActivity(new Intent(this, ImageAudioRecordActivity.class));
    }
}
