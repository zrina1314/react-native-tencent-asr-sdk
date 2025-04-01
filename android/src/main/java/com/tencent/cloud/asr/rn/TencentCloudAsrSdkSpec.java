package com.tencent.cloud.asr.rn;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

abstract class TencentCloudAsrSdkSpec extends ReactContextBaseJavaModule {
  TencentCloudAsrSdkSpec(ReactApplicationContext context) {
    super(context);
  }

  public abstract void setup(int appid,int projectId,String secretId,String secretKey,String saveFilePath,Promise promise);

  public abstract void startRecording();

  public abstract void stopRecording(Promise promise);

  public abstract void cancelRecording(Promise promise);

  public abstract void recognizerFile(String filePath,String voiceFormat,String engSerViceType,Promise promise);
}
