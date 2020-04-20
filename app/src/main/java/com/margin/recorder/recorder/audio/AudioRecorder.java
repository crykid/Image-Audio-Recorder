package com.margin.recorder.recorder.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by : mr.lu
 * Created at : 2020-04-20 at 10:06
 * Description: 音频录制
 */
public class AudioRecorder implements IAudioRecorder {

    private static final String TAG = "AudioRecorder";
    private static final int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_MP3;
    /**
     * 采样率
     * 44100是目前的标准，但是某些设备仍然支持22050，16000，11025
     * 采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
     */
    private final static int AUDIO_SAMPLE_RATE = 16000;
    /**
     * 音频源：音频输入-麦克风
     */
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;


    private int duration = -1;
    private Context mContext;
    private Handler mHandler;

    //缓冲区大小；缓冲区字节大小
    private int bufferSizeInBytes = 0;
    //录音对象
    private AudioRecord mAudioRecord;
    private String fileName;
    private RecorderStatus status = RecorderStatus.STATUS_NO_READY;
    //线程池
    private ExecutorService mExecutorService;
    private List<String> fragmentNames = new ArrayList<>();
    private IRecordingListener mRecordingListener;
    private IRecordFinishListener mRecordFinishListener;


    //------------------------------------------------------------------------
    private final static class Holder {
        private final static AudioRecorder INSTANCE = new AudioRecorder();
    }

    public static AudioRecorder getInstance() {
        return Holder.INSTANCE;
    }

    private AudioRecorder() {
        mExecutorService = Executors.newCachedThreadPool();

    }

    public enum RecorderStatus {
        /**
         * 未开始
         */
        STATUS_NO_READY,

        /**
         * 预备
         */
        STATUS_READY,
        /**
         * 录音
         */
        STATUS_START,
        /**
         * 暂停
         */
        STATUS_PAUSE,
        /**
         * 停止
         */
        STATUS_STOP
    }


    @Override
    public AudioRecorder init(Context context) {
        this.mContext = context;
        this.mHandler = new Handler(context.getMainLooper());
        return this;
    }

    @Override
    public AudioRecorder init(Handler mainHandler) {
        this.mHandler = mainHandler;
        return this;
    }

    @Override
    public AudioRecorder progressListener(IRecordingListener recordingListener) {
        this.mRecordingListener = recordingListener;
        return this;
    }

    @Override
    public AudioRecorder finishListener(IRecordFinishListener recordFinishListener) {
        this.mRecordFinishListener = recordFinishListener;
        return this;
    }


    @Override
    public AudioRecorder fileName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            throw new IllegalStateException("fileName can not be empty");
        }
        this.fileName = fileName;
        return this;
    }

    @Override
    public IAudioRecorder duration(int duration) {
        if (duration > 0) {
            this.duration = duration;
        } else {
            this.duration = -1;
        }
        return null;
    }

    @Override
    public void start() {
        // TODO: 2020-04-20  检查权限，麦克风，文件
//        if (mContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO))
        createDefaultAudio();
        startRecord();
    }


    private void createAudio(int audioSource, int sampleRateInHz, int chanelConfig, int audioFormat) {
        bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, chanelConfig, audioFormat);
        mAudioRecord = new AudioRecord(audioSource, sampleRateInHz, chanelConfig, audioFormat, bufferSizeInBytes);
    }

    private void createDefaultAudio() {

        createAudio(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING);
        status = RecorderStatus.STATUS_READY;
    }

    private void startRecord() {
        if (status == RecorderStatus.STATUS_NO_READY || mAudioRecord == null) {
            throw new IllegalStateException("AudioRecorder is not ready");
        }
        if (status == RecorderStatus.STATUS_START) {
            throw new IllegalStateException("AudioRecorder is recording ！");
        }
        Log.d(TAG, " === startRecord === " + mAudioRecord.getState());
        mAudioRecord.startRecording();
        status = RecorderStatus.STATUS_START;
        mExecutorService.execute(this::writeDataToFile);

    }

    private void writeDataToFile() {
        final byte[] audioData = new byte[bufferSizeInBytes];
        FileOutputStream fos = null;
        int readSize = 0;
        String localFileName = fileName;
        if (status == RecorderStatus.STATUS_PAUSE) {
            //假如是暂停录音 将文件名后面加个数字,防止重名文件内容被覆盖
            localFileName += fragmentNames.size();

        }
        fragmentNames.add(localFileName);

        try {
            File file = new File(localFileName);
            if (file.exists()) {
                file.delete();
            }
            fos = new FileOutputStream(file);
        } catch (Exception e) {
            Log.e(TAG, "writeDataToFile: ", e);
        }
        while (status == RecorderStatus.STATUS_START) {
            readSize = mAudioRecord.read(audioData, 0, bufferSizeInBytes);
            if (AudioRecord.ERROR_INVALID_OPERATION != readSize && fos != null) {
                try {
                    fos.write(audioData);

                    //加个回调方便扩展
                    if (mRecordingListener != null) {
                        mRecordingListener.onRecording(audioData, 0, audioData.length);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "writeDataToFile: ", e);
                }
            }
        }
        try {
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "writeDataToFile: ", e);
        }

        //结束监听
        if (mRecordFinishListener != null) {
            mRecordFinishListener.onFinish();
        }


    }

    @Override
    public void pause() {
        Log.d(TAG, "=== pause ===");
        if (status != RecorderStatus.STATUS_START) {
            Log.e(TAG, "pause: AudioRecorder is not working !");
            return;
        }
        mAudioRecord.stop();
        status = RecorderStatus.STATUS_PAUSE;
    }

    @Override
    public void stop() {

        Log.d(TAG, "=== stop === ");
        if (status == RecorderStatus.STATUS_NO_READY || status == RecorderStatus.STATUS_READY) {

            Log.e(TAG, "stop: The recording hasn't started");
            return;
        }
        mAudioRecord.stop();
        status = RecorderStatus.STATUS_STOP;
        save();
    }


    @Override
    public void cancel() {
        //此时不需要回调，因为取消是用户主动行为
        release();
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

    private void release() {

        //重置AudioRecorder
        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }

        //清除录音碎片
        AudioRecorderUtil.clearFragments(fragmentNames);
        //清除录音碎片地址集合
        fragmentNames.clear();
        fileName = null;
        //重置状态
        status = RecorderStatus.STATUS_NO_READY;
    }


    private void save() {
        Log.d(TAG, "=== release === ");

        try {
            if (fragmentNames.size() > 0) {
                List<String> filePaths = new ArrayList<>();
                for (String fragmentName : fragmentNames) {
                    filePaths.add(fragmentName);
                }
                mergePCMFileToWAVFile(filePaths);
            }
        } catch (Exception e) {
            Log.e(TAG, "release: ", e);
        }


        mContext = null;
        mHandler = null;
        release();
    }

    private void mergePCMFileToWAVFile(List<String> filePaths) {
        mExecutorService.execute(() -> {
            if (AudioRecorderUtil.mergePCMFilesToWAVFile(filePaths, fileName)) {
                Log.d(TAG, "mergePCMFileToWAVFile: success");
            } else {
                Log.e(TAG, "mergePCMFileToWAVFile: filed");
            }
        });
    }

    /**
     * 获取录制的内容
     *
     * @return
     */
    public File getAudio() {
        return null;
    }
}
