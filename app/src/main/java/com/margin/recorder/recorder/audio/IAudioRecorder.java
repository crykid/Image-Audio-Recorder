package com.margin.recorder.recorder.audio;

import android.content.Context;
import android.os.Handler;

import java.util.List;

/**
 * Created by : mr.lu
 * Created at : 2020-04-20 at 10:07
 * Description:
 */
public interface IAudioRecorder {

    IAudioRecorder init(Context context);

    IAudioRecorder init(Handler mainHandler);

    IAudioRecorder progressListener(IRecordingListener recordingListener);

    IAudioRecorder finishListener(IRecordFinishListener recordingListener);

    IAudioRecorder fileName(String name);

    IAudioRecorder duration(int duration);

    void start();


    void pause();

    void stop();

    void cancel();

    void play(String filePath);

    void delete(String desFilePath);

    List<String> getAudios(String dir);


}
