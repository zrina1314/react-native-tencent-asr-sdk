package com.tencent.cloud.asr.rn.utils;

import com.facebook.react.ReactApplication;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RNUtils {
  public static void sendEvent(ReactApplicationContext context, String eventName, WritableMap params) {
    context
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit(eventName, params);


//    context.getJSModule(RCTEventEmitter.class).receiveEvent(
//            getId(),
//            "onButtonClick",
//            null
//    );
  }


  public static WritableArray convertListToWritable(List jsonArr) {
    WritableArray arr = Arguments.createArray();
    for (int i = 0; i < jsonArr.size(); i++) {
      Object obj = null;
      try {
        obj = jsonArr.get(i);
      } catch (Exception jsonException) {
        // Should not happen.
        throw new RuntimeException(i + " should be within bounds of array " + jsonArr.toString(), jsonException);
      }
      if (obj instanceof Map)
        arr.pushMap(convertMapToWritable((Map<String, Object>) obj));
      else if (obj instanceof List)
        arr.pushArray(convertListToWritable((List) obj));
      else if (obj instanceof String)
        arr.pushString((String) obj);
      else if (obj instanceof Double)
        arr.pushDouble((Double) obj);
      else if (obj instanceof Integer)
        arr.pushInt((Integer) obj);
      else if (obj instanceof Boolean)
        arr.pushBoolean((Boolean) obj);
      else if (obj == null)
        arr.pushNull();
      else
        throw new RuntimeException("Unrecognized object: " + obj);
    }

    return arr;
  }

  public static WritableMap convertMapToWritable(Map jsonObj) {
    WritableMap map = Arguments.createMap();
    Iterator<String> it = jsonObj.keySet().iterator();
    while (it.hasNext()) {
      String key = it.next();
      Object obj = null;
      try {
        obj = jsonObj.get(key);
      } catch (Exception jsonException) {
        // Should not happen.
        throw new RuntimeException("Key " + key + " should exist in " + jsonObj.toString() + ".", jsonException);
      }
      if (obj instanceof Map)
        map.putMap(key, convertMapToWritable((Map) obj));
      else if (obj instanceof List<?>)
        map.putArray(key, convertListToWritable((List) obj));
      else if (obj instanceof String)
        map.putString(key, (String) obj);
      else if (obj instanceof Double)
        map.putDouble(key, (Double) obj);
      else if (obj instanceof Long)
        map.putDouble(key, ((Long) obj).doubleValue());
      else if (obj instanceof Integer)
        map.putInt(key, (Integer) obj);
      else if (obj instanceof Boolean)
        map.putBoolean(key, (Boolean) obj);
      else if (obj == null)
        map.putNull(key);
      else
        throw new RuntimeException("Unrecognized object: " + obj);
    }
    return map;
  }


  /**
   * ReadableMap 数据类型 转为 Map<String, String>
   *
   * @param readableMap
   * @return
   */
  public static Map<String, String> ReadableMapToMap(ReadableMap readableMap) {
    Map<String, String> mapStr = new HashMap<>();
    if (readableMap == null) {
      return mapStr;
    }
    HashMap<String, Object> mapObj = readableMap.toHashMap();
    String key;
    Object value;
    for (Map.Entry<String, Object> entry : mapObj.entrySet()) {
      key = entry.getKey();
      value = entry.getValue();
      mapStr.put(key, value != null ? value.toString() : "");
    }
    return mapStr;
  }



}
