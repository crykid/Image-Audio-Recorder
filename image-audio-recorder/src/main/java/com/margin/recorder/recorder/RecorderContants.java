package com.margin.recorder.recorder;

/**
 * Created by : mr.lu
 * Created at : 2020-04-22 at 10:40
 * Description:
 */
public class RecorderContants {

    /**
     * 默认记录时间段，单位：s
     */
    public final static int DEFAULT_SECOND = 30;
    /**
     * 最大记录时间，单位：s
     */
    public final static int MAX_SECOND = 60;

    /**
     * 默认拍照次数，后续可能根据项目调整
     */
    public final static int DEFAULT_CAPTURE_TIME = 5;

    /**
     * 默认录音存储路径
     * com.xxx.xxx/Environment.DIRECTORY_MUSIC/audio/name
     */
    public static final String DIRECTORY_AUDIO = "audio";

    /**
     * 默认拍照存储路径
     * com.xxx.xxx/Environment.DIRECTORY_MUSIC/capture/name
     */
    public static final String DIRECTORY_CAPTURE = "capture";
}
