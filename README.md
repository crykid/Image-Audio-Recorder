# Image-Audio-Recorder
---
```text
在一定时间内，录音，拍摄一定数量的照片。
结束后返回录音文件的路径，以及照片列表。
```

---
## 开发/生产 设置

该library支持模块化运行，特别注意在library的build.gradle中，
第一行 **asApplication**参数：


- asApplication = true：模块作为Application单独运行，此时的manifest会使用main.debug下的版本，并设置applicationId；
- asApplication = false：模块作为library使用 ，此时manifest会使用main.release下的版本。

---


## 主要api：
- startRecording()： 开始记录。开始拍照并录音。
- restartRecord()： 重新开始记录。废弃之前录制的内容。
- cancelRecording()：取消。停止记录，删除已记录的音频和照片，释放资源并退出。

特别说明：都需要在可视状态下调用

---

## 启动方式：

### 1.打开主页面：
 使用 ImageAudioRecordActivity.start()方法。
以startActivityForResult方式启动。requestCode见**ImageAudioRecordActivity.RECORDER_REQUESTCODE**

    参数说明：
 
 - int recordTime ：记录时间。
 - int captureTime ： 拍照次数，拍摄张数。
 - String readContent ： 需要显示/阅读的文本内容。

### 2.开始记录


 当一切准备就绪，需要注意，相机预览会在Activity的onResume（）中准备就绪，此时才可以进行预览拍照。所以要想实现一进页面直接开始记录，在此时调用startRecording()开始录制最好。

---
## 结束方式：

### 1.结束记录
在ImageAudioRecordActivity中重写了**onStatusChange**方法，当状态变成ImageRecorderStatus.STOP时，照片拍摄已经停止，再停止录音，然后调用**finishAndReturnData()**方法。setResult，结束当前页面并返回数据。

### 2.获取数据
在Activity的OnActivityResult的 data中取出数据。

    参数说明：
  
  - 录音文件地址 String ：ImageAudioRecordActivity.INTENT_AUDIO_PATH;
  - 照片列表 ArrayList< String> ：ImageAudioRecordActivity.INTENT_IMAGE_PATH

    
---

## 其它说明

- 该模块不负责权限检查。因为权限弹窗有定制，所以该模块不负责权限检查，使用该模块之前需要做好权限检查.
- 录音并没有严格的计时功能。其计时的功能由ImageRecorder的TimeSchedule来控制。





