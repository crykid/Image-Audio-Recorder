package com.margin.recorder.recorder.image;

/**
 * Created by : mr.lu
 * Created at : 2020-04-22 at 17:15
 * Description:
 */
public enum ImageRecorderStatus {

    /**
     * 初始化完成，可以开始预览
     */
    READY,
    /**
     * 已开始预览，可以进行拍照
     */
    PREVIEW,
    /**
     * 正在拍照，自动拍照期间不可以重复拍照
     */
    CAPTURE,
    /**
     * 拍照完成，相机关闭。
     * 想继续拍照则需要从初始化开始
     */
    STOP
}
