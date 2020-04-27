package com.margin.recorder.recorder.image;

/**
 * Created by : mr.lu
 * Created at : 2020-04-22 at 10:24
 * Description: 拍照策略
 */
public enum ScheduleStrategy {

    /**
     * 随机
     */
    AUTO_RANDOM,
    /**
     * 间隔相同时间
     */
    AUTO_AVERAGE,

    /**
     * 手动
     */
    HAND

}
