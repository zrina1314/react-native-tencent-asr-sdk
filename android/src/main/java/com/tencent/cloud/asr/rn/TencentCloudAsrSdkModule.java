package com.tencent.cloud.asr.rn;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.tencent.aai.audio.utils.WavCache;
import com.tencent.aai.exception.ClientException;
import com.tencent.aai.exception.ServerException;
import com.tencent.aai.listener.AudioRecognizeResultListener;
import com.tencent.aai.listener.AudioRecognizeStateListener;
import com.tencent.aai.log.AAILogger;
import com.tencent.aai.model.AudioRecognizeRequest;
import com.tencent.aai.model.AudioRecognizeResult;
import com.tencent.cloud.asr.rn.core.realtime.AsrRealTimeManager;
import com.tencent.cloud.asr.rn.core.sentence.AsrSentenceManager;
import com.tencent.cloud.asr.rn.utils.PermissionsUtils;
import com.tencent.cloud.asr.rn.utils.RNUtils;

import java.io.DataOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TencentCloudAsrSdkModule extends TencentCloudAsrSdkSpec {
  public static final String NAME = "TencentAsrSdk";

  /** 语音实时识别 SDK Manager */
  private AsrRealTimeManager asrRealTimeManager;

  /** 一句话（短）语音识别 SDK Manager */
  private AsrSentenceManager asrSentenceManager;

  private int appid;
  private int projectId;
  private String secretId;
  private String secretKey;

  private String saveFilePath;

  TencentCloudAsrSdkModule(ReactApplicationContext context) {
    super(context);
  }
  @ReactMethod
  public void init(int appid,int projectId,String secretId,String secretKey,String saveFilePath,Promise promise){
    this.appid =appid;
    this.projectId=projectId;
    this.secretId=secretId;
    this.secretKey=secretKey;
    this.saveFilePath= saveFilePath;
    // 检查sdk运行的必要条件权限
    promise.resolve(true);
  }

  private void initRealTimeSDK(){
    if (asrRealTimeManager==null){
      PermissionsUtils.checkPermissions(getCurrentActivity());
      asrRealTimeManager=new AsrRealTimeManager(getReactApplicationContext());
      asrRealTimeManager.init(appid, projectId, secretId, secretKey,saveFilePath);
    }
  }

  private void initSentenceSDK(){
    if (asrSentenceManager==null){
      asrSentenceManager=new AsrSentenceManager(getReactApplicationContext());
      asrSentenceManager.init(appid, projectId, secretId, secretKey);
    }
  }

  @ReactMethod
  public void startRecorder() {
    initRealTimeSDK();
    asrRealTimeManager.startRecorder();
  }

  @ReactMethod
  public void stopRecorder(Promise promise) {
    initRealTimeSDK();
    asrRealTimeManager.stopRecorder(promise);
  }

  @ReactMethod
  public void cancelRecorder(Promise promise){
    initRealTimeSDK();
    asrRealTimeManager.cancel(promise);
  }
  @ReactMethod
  public void recognizerFile(String filePath,String voiceFormat,Promise promise){
    initSentenceSDK();
    asrSentenceManager.recognizerFile(filePath, voiceFormat, promise);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @Override
  public void onCatalystInstanceDestroy() {
    if (asrRealTimeManager!=null){
      asrRealTimeManager.onDestroy();
    }
    super.onCatalystInstanceDestroy();
  }
}
