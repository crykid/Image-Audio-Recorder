package com.margin.recorder.recorder.audio;

import android.media.MediaRecorder;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.margin.recorder.recorder.FileUtil;
import com.margin.recorder.recorder.RecorderContants;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by : mr.lu
 * Created at : 2020-04-20 at 10:06
 * Description: 音频录制
 * 一次性录制，每一次开始录制都要重新录入所有的参数。
 * 每一次录制结束，状态都会重置。
 */
public class AudioRecorderIml implements IAudioRecorder {

    private static final String TAG = "AudioRecorderIml";

    private IOnAudioRecorderStatusChangeListener mListener;
    //默认30s
    private int period = RecorderContants.DEFAULT_SECOND;
    private MediaRecorder mMediaRecorder;
    private String fileName;
    private AudioRecorderStatus status = AudioRecorderStatus.STATUS_NO_READY;

    //------------------------------------------------------------------------
    private final static class Holder {
        private final static AudioRecorderIml INSTANCE = new AudioRecorderIml();
    }

    public static AudioRecorderIml getInstance() {
        return Holder.INSTANCE;
    }

    private AudioRecorderIml() {

    }


    @Override
    public AudioRecorderIml init() {
        if (mMediaRecorder == null)
            mMediaRecorder = new MediaRecorder();
        this.status = AudioRecorderStatus.STATUS_READY;
        return this;
    }

    @Override
    public IAudioRecorder statusChangeListener(IOnAudioRecorderStatusChangeListener listener) {
        this.mListener = listener;
        return this;
    }


    @Override
    public AudioRecorderIml fileName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("fileName can not be empty");
        }
        this.fileName = fileName;
        return this;
    }

    @Override
    public IAudioRecorder period(int period) {
        if (period > 0) {
            this.period = period;
        } else {
            this.period = 30;
        }
        return this;
    }

    @Override
    public void start() {
        // TODO: 2020-04-20  检查权限，麦克风，文件
//        if (mContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO))
//        createDefaultAudio();

        if (status == AudioRecorderStatus.STATUS_NO_READY || mMediaRecorder == null) {
            throw new IllegalArgumentException("The recorder has not been initialized ！");
        }
        if (TextUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("The audio name can not be null !");
        }
        status = AudioRecorderStatus.STATUS_START;
        if (mListener != null) {
            mListener.onChange(status);
        }
        Log.d(TAG, "=== start ===");
        startRecord();
    }

    private void startRecord() {

        // 开始录音
        /* ①Initial：实例化MediaRecorder对象 */

//        mMediaRecorder.setMaxDuration(1000 * period);
        try {
            /* ②setAudioSource/setVedioSource */
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * ，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            /* ③准备 */
            mMediaRecorder.setOutputFile(fileName);
            mMediaRecorder.prepare();
            /* ④开始 */
            mMediaRecorder.start();


        } catch (IllegalStateException e) {
            Log.e(TAG, "startMeidaRecord: ", e);
        } catch (IOException e) {
            Log.e(TAG, "startMeidaRecord: ", e);
        }

    }


    @Override
    public void pause() {
        Log.d(TAG, "=== pause ===");
        if (status != AudioRecorderStatus.STATUS_START) {
            Log.e(TAG, "pause: AudioRecorderIml is not working !");
            return;
        }
        //7.0以上才有暂停功能
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMediaRecorder.pause();
        }
        status = AudioRecorderStatus.STATUS_PAUSE;
        if (mListener != null) {
            mListener.onChange(status);
        }
    }

    @Override
    public void resume() {
        if (status == AudioRecorderStatus.STATUS_PAUSE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mMediaRecorder.resume();
            }
            status = AudioRecorderStatus.STATUS_START;
            if (mListener != null) {
                mListener.onChange(status);
            }
        }
    }

    @Override
    public void stop() {

        Log.d(TAG, "=== stop === ");
        if (status == AudioRecorderStatus.STATUS_NO_READY || status == AudioRecorderStatus.STATUS_READY) {

            Log.e(TAG, "stop: The recording hasn't started");
            return;
        }
        status = AudioRecorderStatus.STATUS_STOP;
        if (mListener != null) {
            mListener.onChange(status);
        }
        try {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
            fileName = "";
        } catch (RuntimeException e) {
            Log.e(TAG, "stop: ", e);
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

            FileUtil.clearFragments(fileName);
            fileName = "";
        }
    }


    @Override
    public void cancel() {
        //此时不需要回调，因为取消是用户主动行为

        //1.退出录音
        final String p = fileName;
        stop();
        //2.清除录音文件
        FileUtil.clearFragments(p);

    }

    @Override
    public void play(String filePath) {
        // TODO: 2020-04-20
    }

    @Override
    public void delete(String desFilePath) {
        // TODO: 2020-04-20
    }

    @Override
    public List<String> getAudios(String dir) {
        // TODO: 2020-04-20
        return null;
    }


    /**
     * 获取录制的内容
     *
     * @return
     */
    public File getAudio() {
        return null;
    }


    public boolean isStart() {
        return status == AudioRecorderStatus.STATUS_START;
    }

    public boolean isPause() {
        return status == AudioRecorderStatus.STATUS_PAUSE;
    }

    public boolean isStop() {
        return status == AudioRecorderStatus.STATUS_STOP || status == AudioRecorderStatus.STATUS_NO_READY;
    }

    public String getStatus() {
        return status.name();
    }

}
