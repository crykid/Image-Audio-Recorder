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

    void restartRecord();

    @Deprecated
    void pause();

    @Deprecated
    void resume();

    /**
     * 停止录音
     * stop相对于cancel，使用时应挑选更合适的时机
     */
    void stop();

    /**
     * 停止录音并删除已录制部分，释放资源 然后退出录音
     */
    void cancel();


    String getAudio();


    boolean isStart();

    boolean isStop();

    @Deprecated
    boolean isPause();

    String getStatus();


}
