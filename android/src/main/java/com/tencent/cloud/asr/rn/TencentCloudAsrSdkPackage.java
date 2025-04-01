package com.tencent.cloud.asr.rn;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;
import com.facebook.react.TurboReactPackage;
import com.tencent.aai.audio.data.PcmAudioDataSource;
import com.tencent.cloud.asr.rn.BuildConfig;

import java.util.HashMap;
import java.util.Map;

public class TencentCloudAsrSdkPackage extends TurboReactPackage {

  @Nullable
  @Override
  public NativeModule getModule(String name, ReactApplicationContext reactContext) {
    if (name.equals(TencentCloudAsrSdkModule.NAME)) {
      return new TencentCloudAsrSdkModule(reactContext);
    } else {
      return null;
    }
  }

  @Override
  public ReactModuleInfoProvider getReactModuleInfoProvider() {
    return () -> {
      final Map<String, ReactModuleInfo> moduleInfos = new HashMap<>();
      boolean isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED;
      moduleInfos.put(
              TencentCloudAsrSdkModule.NAME,
              new ReactModuleInfo(
                      TencentCloudAsrSdkModule.NAME,
                      TencentCloudAsrSdkModule.NAME,
                      false, // canOverrideExistingModule
                      false, // needsEagerInit
                      true, // hasConstants
                      false, // isCxxModule
                      isTurboModule // isTurboModule
      ));
      return moduleInfos;
    };
  }
}
