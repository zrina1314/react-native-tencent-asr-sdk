package com.tencent.cloud.asr.rn;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

abstract class TencentCloudAsrSdkSpec extends ReactContextBaseJavaModule {
  TencentCloudAsrSdkSpec(ReactApplicationContext context) {
    super(context);
  }

  public abstract void init(int appid,int projectId,String secretId,String secretKey,String saveFilePath,Promise promise);

  public abstract void startRecorder();

  public abstract void stopRecorder(Promise promise);


  public abstract void cancelRecorder(Promise promise);
}
