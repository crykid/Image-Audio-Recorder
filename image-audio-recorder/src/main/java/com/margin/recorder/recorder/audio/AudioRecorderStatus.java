package com.margin.recorder.recorder.audio;


import android.support.annotation.NonNull;

public enum AudioRecorderStatus  {
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
        STATUS_STOP;

        @NonNull
        @Override
        public String toString() {
                return super.toString();
        }
}