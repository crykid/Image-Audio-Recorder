package com.margin.recorder.recorder.image;

/**
 * Created by : mr.lu
 * Created at : 2020-04-22 at 17:11
 * Description: 摄像头-预览工作状态监听
 */
public interface IOnImageRecorderStatusChangeListener {
    void onStatusChange(ImageRecorderStatus status);
}
