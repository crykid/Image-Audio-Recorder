package com.margin.recorder.recorder.image;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.TextureView;

import java.util.List;

/**
 * Created by : mr.lu
 * Created at : 2020-04-20 at 10:06
 * Description:
 */
public interface IImageRecorder {

    IImageRecorder target(@NonNull Activity activity, @NonNull TextureView previewView);

    IImageRecorder hand();

    IImageRecorder autoRandom(int time, int period);

    IImageRecorder autoAverage(int time, int period);

    IImageRecorder directory(@NonNull String directory);

    IImageRecorder recorderStatusChangeListener(@NonNull IOnImageRecorderStatusChangeListener listener);

    void startPreview();

    void takePhoto();

    void finish();

    List<String> getFiles();


}
