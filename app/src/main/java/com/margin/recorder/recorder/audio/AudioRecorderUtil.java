package com.margin.recorder.recorder.audio;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by : mr.lu
 * Created at : 2020-04-20 at 16:29
 * Description:
 */
class AudioRecorderUtil {
    private static final String TAG = "AudioRecorderUtil";

    static boolean mergePCMFilesToWAVFile(List<String> filePaths, String fileName) {
        int fragmentNum = filePaths.size();
        final List<File> fragments = new ArrayList<>(fragmentNum);
        byte[] buffer = null;
        int TOTAL_SIZE = 0;

        for (int i = 0; i < fragmentNum; i++) {
            File fragmentFile = new File(filePaths.get(i));
            fragments.add(fragmentFile);
            TOTAL_SIZE += fragmentFile.length();
        }
        // 填入参数，比特率等等。这里用的是16位单声道 8000 hz
        WaveHeader header = new WaveHeader();
        // 长度字段 = 内容的大小（TOTAL_SIZE) +
        // 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        header.fileLength = TOTAL_SIZE + (44 - 8);
        header.FmtHdrLeth = 16;
        header.BitsPerSample = 16;
        header.Channels = 2;
        header.FormatTag = 0x0001;
        header.SamplesPerSec = 8000;
        header.BlockAlign = (short) (header.Channels * header.BitsPerSample / 8);
        header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;

        byte[] h = null;
        try {
            h = header.getHeader();

            //WAV标准，头部应该是44字节,如果不是44个字节则不进行转换文件
            if (h.length != 44) {
                return false;
            }
            //删除目标文件
            File desFile = new File(fileName);
            if (desFile.exists()) {
                desFile.delete();
            }


            /* 合成所有PCM文件的数据，写到目标文件*/
            buffer = new byte[1024 * 4];
            InputStream is = null;
            OutputStream os = new BufferedOutputStream(new FileOutputStream(fileName));
            //写入头
            os.write(h, 0, h.length);

            for (int i = 0; i < fragmentNum; i++) {
                is = new BufferedInputStream(new FileInputStream(fragments.get(i)));
                int size = is.read(buffer);
                while (size != -1) {
                    os.write(buffer);
                    size = is.read(buffer);
                }
                is.close();
            }
            os.close();
            Log.d(TAG, "mergePCMFilesToWAVFile: success !");

//            clearFragments(filePaths);
            return true;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return false;
    }

    /**
     * 将录音片段删除
     *
     * @param filePaths
     */
    static void clearFragments(List<String> filePaths) {
        if (filePaths != null && filePaths.size() > 0) {

            for (String p : filePaths) {
                File file = new File(p);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }
}
