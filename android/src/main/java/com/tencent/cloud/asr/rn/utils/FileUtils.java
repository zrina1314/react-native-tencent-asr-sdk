package com.tencent.cloud.asr.rn.utils;

import android.util.Log;

import com.tencent.cloud.asr.rn.core.realtime.AsrRealTimeManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileUtils {

    private static String TAG = FileUtils.class.getName();

    public static byte[] readFileToByteArray(String path) {
        File file = new File(path);
        if(!file.exists()) {
            Log.e(TAG,"File doesn't exist!");
            return null;
        }
        FileInputStream in =null;
        try {
             in = new FileInputStream(file);
            long inSize = in.getChannel().size();//判断FileInputStream中是否有内容
            if (inSize == 0) {
                Log.d(TAG,"The FileInputStream has no content!");
                return null;
            }
            byte[] buffer = new byte[in.available()];//in.available() 表示要读取的文件中的数据长度
            in.read(buffer);  //将文件中的数据读到buffer中
            return buffer;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (in!=null){
                    in.close();
                }
            } catch (IOException e) {
                return null;
            }
        }
    }
}
