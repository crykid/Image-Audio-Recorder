package com.margin.recorder.recorder.audio;

import android.support.annotation.NonNull;

import java.util.List;

/**
 * Created by : mr.lu
 * Created at : 2020-04-20 at 10:07
 * Description:
 */
public interface IAudioRecorder {

    IAudioRecorder init();

    IAudioRecorder statusChangeListener(@NonNull IOnAudioRecorderStatusChangeListener listener);

    IAudioRecorder fileName(@NonNull String name);

    IAudioRecorder period(int period);

    void start();


    void pause();

    void resume();

    void stop();

    void cancel();

    void play(String filePath);

    void delete(String desFilePath);

    List<String> getAudios(@NonNull String dir);

    boolean isStart();

    boolean isStop();

    boolean isPause();

    String getStatus();


}
