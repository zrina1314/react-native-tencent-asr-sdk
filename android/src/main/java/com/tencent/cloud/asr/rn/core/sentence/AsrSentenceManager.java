package com.tencent.cloud.asr.rn.core.sentence;


import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.tencent.cloud.asr.rn.utils.AsrRNUtils;
import com.tencent.cloud.asr.rn.utils.FileUtils;
import com.tencent.cloud.qcloudasrsdk.onesentence.QCloudOneSentenceRecognizer;
import com.tencent.cloud.qcloudasrsdk.onesentence.QCloudOneSentenceRecognizerListener;
import com.tencent.cloud.qcloudasrsdk.onesentence.common.QCloudSourceType;
import com.tencent.cloud.qcloudasrsdk.onesentence.network.QCloudOneSentenceRecognitionParams;

import java.io.IOException;

public class AsrSentenceManager {

    private String TAG = AsrSentenceManager.class.getName();

    private final ReactApplicationContext applicationContext;

    private QCloudOneSentenceRecognizer recognizer;

    private Promise promise;
    public AsrSentenceManager(ReactApplicationContext applicationContext){
        this.applicationContext = applicationContext;
    }

    /** 识别结果 回调函数 */
    private  QCloudOneSentenceRecognizerListener callback = new QCloudOneSentenceRecognizerListener() {
        @Override
        public void didStartRecord() {

        }

        @Override
        public void didStopRecord() {

        }

        @Override
        public void recognizeResult(QCloudOneSentenceRecognizer recognizer, String result, Exception exception) {
//            WritableMap resultMap = Arguments.createMap();
//            resultMap.putString("result",result);
//            sendEvent("asr_sentence_RecognizeResult_onSuccess",resultMap);
            if (exception!=null){
                promise.reject(exception);
            }else{
                promise.resolve(result);
            }
        }
    };

    public void init(int appid,int projectId,String secretId,String secretKey){
        if (recognizer == null) {
            /**直接鉴权**/
            recognizer = new QCloudOneSentenceRecognizer(String.valueOf(appid), secretId, secretKey);
            /**使用临时密钥鉴权
             * * 1.通过sts 获取到临时证书 （secretId secretKey  token） ,此步骤应在您的服务器端实现，见https://cloud.tencent.com/document/product/598/33416
             *   2.通过临时密钥调用接口
             * **/
//            recognizer = new QCloudOneSentenceRecognizer(this,DemoConfig.apppId, "临时secretId", "临时secretKey","对应的token");
            recognizer.setCallback(callback);
        }
    }

    /**
     * 识别音频文件
     * @param filePath 完整的文件路径 "file:///var/mobile/.../Library/Caches/gaw_383_17...ssxs0.wav"
     * @param voiceFormat 识别音频的音频格式(支持wav、pcm、ogg-opus、speex、silk、mp3、m4a、aac)
     */
    public void recognizerFile(String filePath,String voiceFormat,Promise promise){
        this.promise = promise;
        try {
            byte[] audioData =  FileUtils.readFileToByteArray(filePath);
            //配置识别参数,详细参数说明见： https://cloud.tencent.com/document/product/1093/35646
            QCloudOneSentenceRecognitionParams params = (QCloudOneSentenceRecognitionParams)QCloudOneSentenceRecognitionParams.defaultRequestParams();
            params.setFilterDirty(0);// 0 ：默认状态 不过滤脏话 1：过滤脏话
            params.setFilterModal(0);// 0 ：默认状态 不过滤语气词  1：过滤部分语气词 2:严格过滤
            params.setFilterPunc(1); // 0 ：默认状态 不过滤句末的句号 1：滤句末的句号
            params.setConvertNumMode(0);//1：默认状态 根据场景智能转换为阿拉伯数字；0：全部转为中文数字。
//          params.setHotwordId(""); // 热词id。用于调用对应的热词表，如果在调用语音识别服务时，不进行单独的热词id设置，自动生效默认热词；如果进行了单独的热词id设置，那么将生效单独设置的热词id。
            params.setData(audioData);
            params.setVoiceFormat(voiceFormat);//识别音频的音频格式，支持wav、pcm、ogg-opus、speex、silk、mp3、m4a、aac。
            params.setSourceType(QCloudSourceType.QCloudSourceTypeData);
            params.setEngSerViceType("16k_zh"); //默认16k_zh，更多引擎参数详见https://cloud.tencent.com/document/product/1093/35646 内的EngSerViceType字段
            params.setReinforceHotword(1); // 开启热词增强功能
            recognizer.recognize(params);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("exception msg" + e.getMessage());
        }
    }


    private void sendEvent(String eventName, WritableMap params){
//        handler.post(() -> {
            applicationContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
//    RNUtils.sendEvent(activity, eventName, params);
//        });
    }
}
