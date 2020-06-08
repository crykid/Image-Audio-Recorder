package com.margin.recorder.recorder.image;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.margin.recorder.recorder.RecorderContants;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by : mr.lu
 * Created at : 2020-04-26 at 10:56
 * Description: 时间规划
 * 在一段时间内，按照时间<strong>规划策略</strong>{@link ScheduleStrategy}和<strong>拍照次数</strong>，
 * 安排好拍照时间表，然后根据时间表执行事件。当然时间规划并不关注执行的事件是什么，只负责规划时间和通知
 */
public class TimeSchedule {

    private static final String TAG = "TimeSchedule";
    //总时间
    private int RECORD_PERIOD = RecorderContants.DEFAULT_SECOND;
    //拍照次数
    private int CAPTURE_TIME;

    private IScheduleExecutor mExecutor;

    private ScheduleStrategy scheduleStrategy;

    private final int EXECUTE_MSG = 1;
    private final int STOP_EXECUTE = 2;

    private int executedTime = 0;
    ThreadLocal<Integer> local = new ThreadLocal<>();
    private Timer timer;

    private static TimeSchedule schedule;


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == EXECUTE_MSG) {
                if (local.get() == null) {
                    local.set(0);
                }
                executedTime = local.get();
                if (executedTime > CAPTURE_TIME) return;
                randomSchedule();
            } else if (msg.what == STOP_EXECUTE) {
                local.set(0);
            }
        }
    };



    //----------------------Singleton Holder----------------------------------
    private TimeSchedule(){}

    private final static class Holder{
        private final static TimeSchedule INSTANCE = new TimeSchedule();
    }
    public static TimeSchedule getInstance(){
        return Holder.INSTANCE;
    }
    //----------------------Singleton Holder----------------------------------


    /**
     * @param scheduleStrategy
     * @param reocrdPeriod     单位：秒-S
     * @param captureTime
     * @return
     */
    public TimeSchedule prepare(@NonNull ScheduleStrategy scheduleStrategy, int reocrdPeriod, int captureTime) {
        if (reocrdPeriod <= 0)
            throw new IllegalArgumentException("Total reocrdPeriod can only greater than 0 !");
        if (captureTime <= 0)
            throw new IllegalArgumentException("captureTime can only greater than 0 !");
        if (reocrdPeriod < 2) {
            Log.e(TAG, "prepare: Strongly suggest that reocrdPeriod greater than 2 !");
        }
        this.scheduleStrategy = scheduleStrategy;
        RECORD_PERIOD = reocrdPeriod;
        CAPTURE_TIME = captureTime;
        local.set(executedTime);
        return this;
    }

    public void execute(IScheduleExecutor executor) {
        assert executor == null : "ScheduleExecutor can not be null !";
        this.mExecutor = executor;
        schedule();
    }

    /**
     * 安排时间表
     */
    private void schedule() {
        //拍照-存储 流程需要大约1s左右，所以时间间隔应该大于1s，并且，最后一次时间安排距离结束时间应大于这个时间段
//        final int scheduleTotalTime = RECORD_PERIOD - 1;

        //只需要生成4个时间就行了。
        if (scheduleStrategy == ScheduleStrategy.AUTO_AVERAGE) {
            average();
        } else {
            mHandler.sendEmptyMessage(EXECUTE_MSG);
//            randomSchedule();
        }

    }

    private void randomSchedule() {

        int max = RECORD_PERIOD / CAPTURE_TIME;
        float next = new Random().nextFloat() * max;
        long delay = (long) (next * 1000);
        mHandler.postDelayed(() -> {
            executedTime = local.get();
            executedTime++;
            local.set(executedTime);

            mExecutor.execute(executedTime);

            mHandler.sendEmptyMessage(EXECUTE_MSG);
        }, delay);

    }

    TimerTask takePhotoTask = null;

    private void average() {


        long period = RECORD_PERIOD * 1000 / CAPTURE_TIME;
        if (timer == null) {
            timer = new Timer();
        }
        takePhotoTask = new TimerTask() {

            ThreadLocal<Integer> local = new ThreadLocal();

            @Override
            public void run() {
                if (local.get() == null) {
                    local.set(1);
                }
              int  executedTime = local.get();

                if (executedTime > CAPTURE_TIME) {
                    timer.cancel();
                    local.remove();
                    Log.d(TAG, "run: cancel");
                }
                Log.d(TAG, "run: executedTime = " + executedTime);
                mExecutor.execute(executedTime);
                executedTime++;
                local.set(executedTime);


            }
        };

        //每隔period时间执行一次
        timer.schedule(takePhotoTask, 0, period);
    }

    public void stop() {
        if (scheduleStrategy == ScheduleStrategy.AUTO_AVERAGE) {

            if (timer != null) {
                timer.cancel();
                timer.purge();
                timer =null;
            }
            if (takePhotoTask != null) {
                takePhotoTask.cancel();
                takePhotoTask = null;
            }

        } else {
            mHandler.sendEmptyMessage(STOP_EXECUTE);
        }
    }

    interface IScheduleExecutor {
        void execute(int time);
    }
}
