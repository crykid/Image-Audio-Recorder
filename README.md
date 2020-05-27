  # Image-Audio-Recorder
  ```text
在一定时间内，录音，拍摄一定数量的照片。
结束后返回录音文件的路径，一节照片列表。
```
## 启动方式：
    使用 <strong>ImageAudioRecordActivity.start()</strong>方法。以startActivityForResult方式启动
    参数说明：
    - recordTime ：记录时间。
    - captureTime ： 拍照次数，拍摄几张，

## 结束方式：
    见ImageAudioRecordActivity.finishAndReturnData()方法。计时结束，自动结束之前的页面，并setResult。
    在Activity的OnActivityResult的 data中取出数据，说明：
    - 录音文件地址 ：ImageAudioRecordActivity.INTENT_AUDIO_PATH;
    - 照片列表List ：ImageAudioRecordActivity.INTENT_IMAGE_PATH
    