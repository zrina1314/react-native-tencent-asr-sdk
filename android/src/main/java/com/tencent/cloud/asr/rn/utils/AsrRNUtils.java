package com.tencent.cloud.asr.rn.utils;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.tencent.aai.model.AudioRecognizeResult;

public class AsrRNUtils {
  public static WritableMap AudioRecognizeResultToWritableMap(AudioRecognizeResult result){
    WritableMap  obj = Arguments.createMap();
    if (result!=null){
      obj.putString("voiceId",result.getVoiceId());
      obj.putInt("seq",result.getSeq());
      obj.putString("text",result.getText());
      obj.putInt("sliceType",result.getSliceType());
      obj.putInt("startTime",result.getStartTime());
      obj.putInt("endTime",result.getEndTime());
      obj.putInt("code",result.getCode());
      obj.putString("message",result.getMessage());
      obj.putString("resultJson",result.getResultJson());
    }
    return obj;
  }
}
