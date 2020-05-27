# Image-Audio-Recorder
---
```text
在一定时间内，录音，拍摄一定数量的照片。
结束后返回录音文件的路径，以及照片列表。
```
## 开发/生产 设置
```text
该library支持模块化运行，特别注意在library的build.gradle中，
第一行 **asApplication**参数
```
- asApplication = true：模块作为Application单独运行，此时的manifest会使用main.debug下的版本，并设置applicationId；
- asApplication = false：开发模式下使用，模块作为library使用 ，此时manifest会使用main.release下的版本。

---
## 启动方式：
```text
使用 **ImageAudioRecordActivity.start()**方法。
以startActivityForResult方式启动。
```
    参数说明：
 
 - int recordTime ：记录时间。
 - int captureTime ： 拍照次数，拍摄张数。
 - String readContent ： 阅读的文本内容。

---
## 结束方式：
```text
见**ImageAudioRecordActivity.finishAndReturnData()**方法。计时结束并setResult，自动结束并返回之前的页面。
```
    在Activity的OnActivityResult的 data中取出数据，说明：
  
  - 录音文件地址 String ：ImageAudioRecordActivity.INTENT_AUDIO_PATH;
  - 照片列表 List<String> ：ImageAudioRecordActivity.INTENT_IMAGE_PATH
    
---
## 其它说明
```text
因为权限弹窗有定制，所以该模块不负责权限检查。使用该模块之前需要做好权限检查.
```




