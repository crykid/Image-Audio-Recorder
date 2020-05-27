package com.margin.recorder.recorder.audio;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by : mr.lu
 * Created at : 2020-04-20 at 10:07
 * Description:
 */
public interface IAudioRecorder {


    IAudioRecorder statusChangeListener(@NonNull IOnAudioRecorderStatusChangeListener listener);

    IAudioRecorder directory(@NonNull String directory);

    @Deprecated
    IAudioRecorder period(int period);

    void prepare(Context context);

    void start();

    @Deprecated
    void pause();

    @Deprecated
    void resume();

    void stop();

    void cancel();


    String getAudio();


    boolean isStart();

    boolean isStop();

    @Deprecated
    boolean isPause();

    String getStatus();


}
