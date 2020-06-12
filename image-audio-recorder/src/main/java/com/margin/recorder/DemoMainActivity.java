package com.margin.recorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by : mr.lu
 * Created at : 2020/6/8 at 11:08
 * Description: module 的builde.gradle中，第一行，asApplication = true 时，此activity才存在
 */
public class DemoMainActivity extends AppCompatActivity {
    private static final String TAG = "DemoMainActivity";
    private TextView tvMainText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_main);

        tvMainText = findViewById(R.id.tv_main_hello);

        String lantingjixu = "永和九年，岁在癸丑，暮春之初，会于会稽山阴之兰亭，修禊事也。群贤毕至，少长咸集。" +
                "此地有崇山峻岭，茂林修竹，又有清流激湍，映带左右。引以为流觞曲水，列坐其次。虽无丝竹管弦之盛，" +
                "一觞一咏，亦足以畅叙幽情。";

        findViewById(R.id.btn_main_start).setOnClickListener(v ->
                ImageAudioRecordActivity.start(this, 45, 5, lantingjixu)
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            if (requestCode == ImageAudioRecordActivity.RECORDER_REQUESTCODE && data != null) {
                Toast.makeText(this, "记录完成，返回数据", Toast.LENGTH_SHORT).show();
                String audioFilePath = data.getStringExtra(ImageAudioRecordActivity.INTENT_AUDIO_PATH);
                ArrayList<String> imageFilePahts = (ArrayList<String>) data.getSerializableExtra(ImageAudioRecordActivity.INTENT_IMAGE_PATH);

                //判断file是否存在
                File file = new File(audioFilePath);
                Log.d(TAG, "onActivityResult: " + file.exists());
                tvMainText.setText("--AudioPath : " + audioFilePath + "\n"
                        + "--ImagePaths : " + imageFilePahts);
            }

        }
    }

//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        navigationBarStatusBar(this, hasFocus);
//    }
//
//    /**
//     * 导航栏，状态栏隐藏
//     *
//     * @param activity
//     */
//    public static void navigationBarStatusBar(Activity activity, boolean hasFocus) {
//        if (hasFocus && Build.VERSION.SDK_INT >= 19) {
//            View decorView = activity.getWindow().getDecorView();
//            decorView.setSystemUiVisibility(
//                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                            | View.SYSTEM_UI_FLAG_FULLSCREEN
//                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
//        }
//    }
}
