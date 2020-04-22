package com.margin.recorder.recorder.audio;

/**
 * Created by : mr.lu
 * Created at : 2020-04-20 at 15:38
 * Description:
 */
public interface IRecordingListenerAudioRecorder extends IOnAudioRecorderStatusChangeListener {

    void onRecording(byte[] data, int begin, int end);

    @Override
    void onChange(AudioRecorderStatus status);
}