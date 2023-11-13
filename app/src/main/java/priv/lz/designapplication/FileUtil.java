/*
 * Copyright (C) 2023 Baidu, Inc. All Rights Reserved.
 */
package priv.lz.designapplication;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.baidu.aip.asrwakeup3.core.inputstream.InFileStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * @author lizan
 */
public class FileUtil {
    public static final String M4A = "m4a";
    public static final String MP3 = "mp3";
    public static final String PCM = "pcm";

    // context 为ASR读入文件方法调用准备
    private static Context context;
    public static void setContext(Context context) {
        FileUtil.context = context;
    }

    /**
     * OCR 模块调用为图片设置缓存
     */
    public static File getSaveFile(Context context) {
        File file = new File(context.getFilesDir(), "pic.jpg");
        return file;
    }

    /**
     * ASR 模块sdk IN_FILE参数需要的文件流
     */
    public static InputStream getMyFileInputStream() {
        String path = "/storage/emulated/0/Android/data/priv.lz.designapplication/files/ASR/asr.pcm";
        File  file = new File(path);
        InputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (in == null) Log.e("TAG", "ASR待识别的pcm文件的输入流未打开");
//        return in;
        InFileStream.setContext(context, in);
        return InFileStream.create16kStream();
    }

    /**
     * ASR 拷贝文件到应用data目录并转换pcm格式
     */
    @SuppressLint("Recycle")
    public static void cacheFile(Context context, Uri uri) {
        String dirPath = "/storage/emulated/0/Android/data/priv.lz.designapplication/files/ASR";
        String type = getExtensionName(context, uri);
        try {
            // 建ASR data文件夹
            File dirFile = new File(dirPath);
            if (!dirFile.exists()) {
                boolean mkdirs = dirFile.mkdirs();
                if (!mkdirs) {
                    Log.i("TAG", "ASR data文件夹创建失败");
                } else {
                    Log.i("TAG", "ASR data文件夹创建成功");
                }
            }
            // 拷贝公共存储区上的音频到data文件夹中
            File desFile = new File(dirPath +  "/raw." + type);
            if (desFile.exists()) {
                desFile.delete();
            }
            desFile.createNewFile();

            InputStream in =  context.getContentResolver().openInputStream(uri);
            OutputStream out = new FileOutputStream(desFile);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                FileUtils.copy(in, out);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String desFilePath = dirPath + "/asr.pcm";
        String originFilePath = dirPath + "/raw." + type;
        // m4a/mp3 转 pcm
        int rc = FFmpeg.execute("-y -i " + originFilePath + " -acodec pcm_s16le -f s16le -ac 1 -ar 16000 " + desFilePath);

        if (rc == RETURN_CODE_SUCCESS) {
            Log.i(Config.TAG, "Command execution completed successfully.");
        } else if (rc == RETURN_CODE_CANCEL) {
            Log.i(Config.TAG, "Command execution cancelled by user.");
        } else {
            Log.i(Config.TAG, String.format("Command execution failed with rc=%d and the output below.", rc));
            Config.printLastCommandOutput(Log.INFO);
        }
    }

    /**
     * 通过uri解析文件的扩展名
     * @return 扩展名
     */
    public static String getExtensionName(Context context, Uri uri) {
        DocumentFile file = DocumentFile.fromSingleUri(context, uri);
        String fileName = file.getName();
        String type = fileName.substring(fileName.lastIndexOf(".") + 1);
        return type;
    }

    /**
     * TTS 模块用户选择下载目录时预设置文件名 [.mp3后缀]
     * @return tempName.mp3
     */
    public static String getTempFileName() {
        Calendar calendar = Calendar.getInstance();
        String time = calendar.get(Calendar.YEAR) + "." + (calendar.get(Calendar.MONTH) + 1) + "."
                + calendar.get(Calendar.DAY_OF_MONTH) + "-"
                + calendar.get(Calendar.HOUR_OF_DAY) + "." + calendar.get(Calendar.MINUTE) + "." + calendar.get(Calendar.SECOND);
        String baseName = "output-" + time;
        String tempFileName = baseName + ".mp3";
        return  tempFileName;
    }

    /**
     * TTS 模块 <br/>
     * <hr/>
     * 复制文件到公共存储空间
      * @param context
     * @param oldPath
     * @param newUri
     */
    @SuppressLint("Recycle")
    public static void dataFileCopyToPublicStorage(Context context, String oldPath, Uri newUri){
        File file = new File(oldPath);
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(file);
            out = context.getContentResolver().openOutputStream(newUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (in == null || out == null) {
            Log.e("FileUtil", "文件流未正常打开");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                FileUtils.copy(in, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * TTS 模块把合成的原音频文件(.pcm)转换成.mp3格式文件
     * @param PCMPath
     * @return
     */
    public static String convertPCMToMP3InDataFile(String PCMPath) {
        String MP3Path = PCMPath.replace(".pcm", ".mp3");
        // pcm转m4a
        int rc = FFmpeg.execute("-y -f s16be -ac 1 -ar 16.0k -acodec pcm_s16le -i "+PCMPath+" "+MP3Path);

        if (rc == RETURN_CODE_SUCCESS) {
            Log.i(Config.TAG, "Command execution completed successfully.");
        } else if (rc == RETURN_CODE_CANCEL) {
            Log.i(Config.TAG, "Command execution cancelled by user.");
        } else {
            Log.i(Config.TAG, String.format("Command execution failed with rc=%d and the output below.", rc));
            Config.printLastCommandOutput(Log.INFO);
        }
        return  MP3Path;
    }

    /**
     * 跟据uri获取公共区文件父级的绝对路径
     * @param context
     * @param uri
     * @return
     */
    public static String getDirName(Context context, Uri uri) {
        String dirName = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    dirName =  Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
        }
        dirName = "手机存储" + dirName.substring(19, dirName.lastIndexOf("/") + 1);
        Log.w("TAG", dirName);
        return dirName;
    }
}
