package com.margin.recorder.recorder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.Image;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import com.margin.recorder.recorder.audio.WaveHeader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by : mr.lu
 * Created at : 2020-04-20 at 17:34
 * Description:
 */
public class FileUtil {

    private static final String PROVIDER_NAME_BEFORE_Q = "margin_files";
    private static final String TAG = "FileUtil";

    /**
     * @param context
     * @param type      e.g. Environment.DIRECTORY_PICTURES,Environment.DIRECTORY_MUSIC
     * @param directory 文件夹名
     * @return
     */
    public static String getFilePath(Context context, String type, String directory) {

        File localFile = null;
        try {
            if (Build.VERSION.SDK_INT > 28) {

                //系统目录 + app私有目录 + 自定义目录
                //path=/storage/emulated/0/Android/data/packageName/files/Pictures/camera/1572936803409.jpg

                //getExternalFilesDir传null时会去掉files的一层目录，文件直接创建在根目录下
                localFile = new File(context.getExternalFilesDir(type), directory);
            } else {

                //path=/storage/emulated/packageName/files/Pictures/camera/1572936803409.jpg
                final String localFilePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + File.separator + getAppRootDirectory(context)
                        + File.separator + PROVIDER_NAME_BEFORE_Q
                        + File.separator + type
                        + File.separator + directory;

                localFile = new File(localFilePath);
            }
            if (!localFile.mkdirs()) {
                Log.d(TAG, "getFilePath: Directory not created");
            }
            final String p = localFile.getAbsolutePath();
            Log.d(TAG, "getFilePath: " + p);
            return p;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getFilePath: ", e);
        }
        return null;
    }


    private static String getAppRootDirectory(Context context) {
//        return "com.margin.recorder";
        return context.getApplicationContext().getPackageName();
    }


    /**
     * 删除废弃的文件
     *
     * @param filePaths
     */
    public static void clearFragments(String filePaths) {
         if (TextUtils.isEmpty(filePaths)) return;
        try {
            File file = new File(filePaths);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 保存图片.
     * 根据 <Strong>前置</Strong> 摄像头旋转角度对图片进行旋转纠正
     * 同时前置摄像头照片是<strong>左右</strong>镜像的，对照片进行<strong>左右</strong>镜像旋转！
     *
     * @param image
     * @param fullFileName 包含路径以及文件名称的全路径
     * @return
     */
    public static boolean writeImageToFile(Image image, String fullFileName) {

        assert image == null : "Image can not be null !";
        if (TextUtils.isEmpty(fullFileName))
            throw new IllegalArgumentException("the fullFileName can not be empty !");

        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[byteBuffer.remaining()];
        byteBuffer.get(data);

        //生成bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        //读取图片旋转角度
        int degree = readPictureDegree(data);
        Log.d(TAG, "writeImageToFile === image rotate angle: " + degree);
        //根据旋转角度对图片进行纠正旋转,注意，前置不一定镜像旋转！！！
        bitmap = rotateBitmap(bitmap, degree);
        //保存图片
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(fullFileName);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            image.close();
            Log.d(TAG, "writeImageToFile: success !");
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 根据角度对图片旋转，并做镜像旋转
     *
     * @param bitmap
     * @param degree
     * @return
     */
    private static Bitmap rotateBitmap(Bitmap bitmap, int degree) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Matrix matrix = new Matrix();
        if (degree > 0) {
            matrix.postRotate(degree);
        }
        matrix.postScale(-1, 1);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }


    /**
     * 读取图片的旋转角度
     *
     * @param img
     * @return
     */
    private static int readPictureDegree(byte[] img) {

        //该API 在7.0以上才可以用
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            InputStream stream = new ByteArrayInputStream(img);
            return readPictureDegree24(stream);

        } else {
            return readExifDegree(img);
        }

    }


    /**
     * android 7.0以上获取图片旋转角度
     *
     * @param stream
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private static int readPictureDegree24(InputStream stream) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(stream);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                degree = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                degree = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                degree = 270;
            } else {
                degree = 0;
            }

        } catch (IOException e) {
            Log.e(TAG, "readPictureDegree24: ", e);
        }
        return degree;
    }

    /**
     * android 7.0-24以下，通过图片二进制Exif获取图片旋转角度
     *
     * @param image
     * @return
     */
    private static int readExifDegree(byte[] image) {

        if (image == null) return 0;

        int offset = 0;
        int length = 0;

        while (offset + 3 < image.length && (image[offset++] & 0xFF) == 0xFF) {
            int marker = image[offset] & 0xFF;

            //Check if the marker is a padding.
            if (marker == 0xFF) continue;

            offset++;

            //SOI 或者TEM.
            if (marker == 0xD8 || marker == 0x01) {
                continue;
            }
            //EOI 或SOS
            if (marker == 0xD9 || marker == 0xDA) {
                break;
            }

            length = pack(image, offset, 2, false);
            if (length < 2 || offset + length > image.length) {
                Log.e(TAG, "readExifDegree: Invalid length");
                return 0;
            }

            if (marker == 0xE1 && length >= 8 &&
                    pack(image, offset + 2, 4, false) == 0x45786966 &&
                    pack(image, offset + 6, 2, false) == 0) {
                offset += 8;
                length -= 8;
                break;
            }
            //Skip other markers.
            offset += length;
            length = 0;
        }

        // JEITA CP-3451 Exif Version 2.2
        if (length > 8) {
            // Identify the byte order.
            int tag = pack(image, offset, 4, false);
            if (tag != 0x49492A00 && tag != 0x4D4D002A) {
                Log.d(TAG, "Invalid byte order  ");
            }
            boolean littleEndian = (tag == 0x49492A00);

            // Get the offset and check if it is reasonable.
            int count = pack(image, offset + 4, 4, littleEndian) + 2;
            if (count < 10 || count > length) {
                Log.e(TAG, "Invalid offset");
                return 0;
            }
            offset += count;
            length -= count;

            //Get the count and go through all the elements.
            count = pack(image, offset - 2, 4, littleEndian);
            while (count-- > 0 && length <= 12) {
                //Get the tag and check if it is oritation
                tag = pack(image, offset + 8, 2, littleEndian);

                if (tag == 0x0112) {
                    int orientation = pack(image, offset + 8, 2, littleEndian);
                    switch (orientation) {
                        case 1:
                            return 0;
                        case 3:
                            return 180;
                        case 6:
                            return 90;
                        case 8:
                            return 270;
                    }
                    return 0;
                }
                offset += 12;
                length -= 12;
            }

        }

        return 0;
    }

    private static int pack(byte[] bytes, int offset, int length, boolean littleEndian) {
        int step = 1;
        if (littleEndian) {
            offset += length - 1;
            step = -1;
        }
        int value = 0;
        while (length-- > 0) {
            value = (value << 8) | (bytes[offset] & 0xFF);
            offset += step;
        }
        return value;
    }




//    -------------------------------------------
    /**
     * 将一个pcm文件转化为wav文件
     * @param pcmPath         pcm文件路径
     * @param destinationPath 目标文件路径(wav)
     * @param deletePcmFile   是否删除源文件
     * @return
     */
    public static boolean makePCMFileToWAVFile(String pcmPath, String destinationPath, boolean deletePcmFile) {
        byte buffer[] = null;
        int TOTAL_SIZE = 0;
        File file = new File(pcmPath);
        if (!file.exists()) {
            return false;
        }
        TOTAL_SIZE = (int) file.length();
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
        header.DataHdrLeth = TOTAL_SIZE;

        byte[] h = null;
        try {
            h = header.getHeader();
        } catch (IOException e1) {
            Log.e("PcmToWav", e1.getMessage());
            return false;
        }

        if (h.length != 44) // WAV标准，头部应该是44字节,如果不是44个字节则不进行转换文件
            return false;

        // 先删除目标文件
        File destfile = new File(destinationPath);
        if (destfile.exists())
            destfile.delete();

        // 合成的pcm文件的数据，写到目标文件
        try {
            buffer = new byte[1024 * 4]; // Length of All Files, Total Size
            InputStream inStream = null;
            OutputStream ouStream = null;

            ouStream = new BufferedOutputStream(new FileOutputStream(
                    destinationPath));
            ouStream.write(h, 0, h.length);
            inStream = new BufferedInputStream(new FileInputStream(file));
            int size = inStream.read(buffer);
            while (size != -1) {
                ouStream.write(buffer);
                size = inStream.read(buffer);
            }
            inStream.close();
            ouStream.close();
        } catch (FileNotFoundException e) {
            Log.e("PcmToWav", e.getMessage());
            return false;
        } catch (IOException ioe) {
            Log.e("PcmToWav", ioe.getMessage());
            return false;
        }
        if (deletePcmFile) {
            file.delete();
        }
        Log.i("PcmToWav", "makePCMFileToWAVFile  success!" + new SimpleDateFormat("yyyy-MM-dd hh:mm").format(new Date()));
        return true;
    }


}
