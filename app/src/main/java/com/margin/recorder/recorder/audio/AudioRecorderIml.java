package com.margin.recorder.recorder.audio;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.margin.recorder.recorder.FileUtil;
import com.margin.recorder.recorder.RecorderContants;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    private Context mContext;
    private IOnAudioRecorderStatusChangeListener mListener;
    //默认30s
    private int period = RecorderContants.DEFAULT_SECOND;
    private MediaRecorder mMediaRecorder;
    private String fileName, directory = RecorderContants.DIRECTORY_AUDIO;
    private AudioRecorderStatus status = AudioRecorderStatus.STATUS_NO_READY;
    private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

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
    public IAudioRecorder statusChangeListener(IOnAudioRecorderStatusChangeListener listener) {
        this.mListener = listener;
        return this;
    }


    /**
     * 可选的方法，默认"audio"
     *
     * @param directory 存储录音未见的最终文件夹名(不是全路径）
     * @return
     */
    @Override
    public AudioRecorderIml directory(String directory) {
        if (TextUtils.isEmpty(directory)) {
            throw new IllegalArgumentException("The directory can not be empty");
        }
        this.directory = directory;
        return this;
    }

    /**
     * 设置录音长度，可选的方法。录音的开始、停止跟随ImageRecorder变化
     *
     * @param period
     * @return
     */
    @Override
    public IAudioRecorder period(int period) {
        if (period > 0) {
            this.period = period;
        } else {
            this.period = RecorderContants.DEFAULT_SECOND;
        }
        return this;
    }


    /**
     * 准备就绪
     *
     * @param context
     */
    @Override
    public void prepare(Context context) {
        if (mMediaRecorder == null)
            mMediaRecorder = new MediaRecorder();
        this.status = AudioRecorderStatus.STATUS_READY;
        if (context != null) {
            this.mContext = context;
        }
    }

    @Override
    public void start() {
        // TODO: 2020-04-20  检查权限，麦克风，文件
//        if (mContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO))
//        createDefaultAudio();

        if (status == AudioRecorderStatus.STATUS_NO_READY || mMediaRecorder == null) {
            throw new IllegalArgumentException("The recorder has not been initialized ！");
        }
        fileName = generateFileName();

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


    @Deprecated
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

    @Deprecated
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

    /**
     * 停止录音
     */
    @Override
    public void stop() {

        Log.d(TAG, "=== stop === ");
        if (status == AudioRecorderStatus.STATUS_NO_READY || status == AudioRecorderStatus.STATUS_READY) {

            Log.d(TAG, "stop: The recording hasn't started");
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
        } catch (RuntimeException e) {
            Log.e(TAG, "stop: ", e);

        }
    }


    /**
     * 当主动退出（如按返回键，或app进入后台）时候调用。
     * 此时会清除未完成的文件
     */
    @Override
    public void cancel() {
        //此时不需要回调，因为取消是用户主动行为

        final String p = fileName;
        //1.退出录音
        stop();
        //2.清除录音文件
        FileUtil.clearFragments(p);

    }

    @Override
    public String getAudio() {
        return fileName;
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


    private String generateFileName() {
        final String name = dateFormat.format(new Date());
        final String realDirectory = FileUtil.getFilePath(mContext, Environment.DIRECTORY_MUSIC, directory);
        fileName = realDirectory + File.separator + name + ".wav";
        return fileName;
    }

}
