package com.margin.recorder.recorder.image;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.margin.recorder.BuildConfig;
import com.margin.recorder.recorder.FileUtil;
import com.margin.recorder.recorder.RecorderContants;
import com.margin.recorder.recorder.audio.AudioRecorderStatus;
import com.margin.recorder.recorder.audio.IAudioRecorder;
import com.margin.recorder.recorder.audio.IOnAudioRecorderStatusChangeListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by : mr.lu
 * Created at : 2020-05-14 at 16:43
 * Description:
 */
public class AudioRecorder2Iml implements IAudioRecorder {

    private static final String TAG = "AudioRecorder2Iml";
    private IOnAudioRecorderStatusChangeListener mListener;
    //音频输入
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    //采用频率
    private final static int AUDIO_SAMPLE_RATE = 16000;
    //声道，单声道
    private final static int AUDIO_CHANEL = AudioFormat.CHANNEL_IN_MONO;
    //编码
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private int bufferSize = 0;
    private AudioRecord mAudioRecord;
    private String fileName;

    private AudioRecorderStatus status = AudioRecorderStatus.STATUS_NO_READY;


    //----------------------Singleton Holder----------------------------------
    private AudioRecorder2Iml() {
    }

    private final static class Holder {
        private final static AudioRecorder2Iml INSTANCE = new AudioRecorder2Iml();
    }

    public static AudioRecorder2Iml getInstance() {
        return Holder.INSTANCE;
    }
    //----------------------Singleton Holder----------------------------------


    @Override
    public IAudioRecorder statusChangeListener(@NonNull IOnAudioRecorderStatusChangeListener listener) {
        this.mListener = listener;

        return this;
    }

    @Override
    public IAudioRecorder directory(@NonNull String directory) {
        return this;
    }

    @Override
    public IAudioRecorder period(int period) {
        return this;
    }

    @Override
    public void prepare(Context context) {
        bufferSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_CHANEL, AUDIO_ENCODING);
        mAudioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANEL, AUDIO_ENCODING, bufferSize);
        status = AudioRecorderStatus.STATUS_READY;
        if (mListener != null) {
            mListener.onChange(status);
        }
        fileName = generateFileName(context);
    }

    @Override
    public void start() {
        if (status == AudioRecorderStatus.STATUS_NO_READY) {
            throw new IllegalStateException("录音尚未初始化");
        }
        if (status == AudioRecorderStatus.STATUS_START) {
            Log.e(TAG, "start: 正在录音");
            throw new IllegalStateException("正在录音");
        }
        Log.d(TAG, "=== startRecord === " + mAudioRecord.getState());
        mAudioRecord.startRecording();
        status = AudioRecorderStatus.STATUS_START;
        if (mListener != null) {
            mListener.onChange(status);
        }
        //把录音保存到本地
        new Thread(this::writeData2File).start();
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause: 不支持此操作");
    }

    @Override
    public void resume() {
        Log.d(TAG, "resume: 不支持此操作");
    }

    @Override
    public void stop() {
        Log.d(TAG, " === stopRecord ===");

        if (status == AudioRecorderStatus.STATUS_NO_READY || status == AudioRecorderStatus.STATUS_READY) {
            Log.e(TAG, "stop: 录音尚未开始");
        } else {
            mAudioRecord.stop();
            status = AudioRecorderStatus.STATUS_STOP;
            if (mListener != null) {
                mListener.onChange(status);
            }
            release();
        }
    }


    @Override
    public void cancel() {
        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }
        status = AudioRecorderStatus.STATUS_NO_READY;
        if (mListener != null) {
            mListener.onChange(status);
        }
    }

    private void release() {

    }

    @Override
    public String getAudio() {
        return fileName;
    }

    @Override
    public boolean isStart() {
        return status == AudioRecorderStatus.STATUS_START;
    }

    @Override
    public boolean isStop() {
        return status == AudioRecorderStatus.STATUS_STOP;
    }

    @Override
    public boolean isPause() {
        Log.e(TAG, "isPause: 不支持 pause 操作 ！！！");
        return status == AudioRecorderStatus.STATUS_PAUSE;
    }

    @Override
    public String getStatus() {
        return status.name();
    }


    private void writeData2File() {
        try {
            byte[] audioDate = new byte[bufferSize];

            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fos = new FileOutputStream(file);
            int readSize;
            while (status == AudioRecorderStatus.STATUS_START) {
                readSize = mAudioRecord.read(audioDate, 0, bufferSize);
                if (AudioRecord.ERROR_INVALID_OPERATION != readSize) {
                    fos.write(audioDate);
                }
            }

            fos.close();
            if (BuildConfig.DEBUG) {
                String wavFileName = fileName.replace("pcm", "wav");
                FileUtil.makePCMFileToWAVFile(fileName, wavFileName, false);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String generateFileName(Context context) {
        String fileName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        fileName = FileUtil.getFilePath(context, Environment.DIRECTORY_MUSIC, RecorderContants.DIRECTORY_AUDIO)
                + File.separator
                + fileName
                + ".pcm";
        return fileName;
    }
}
