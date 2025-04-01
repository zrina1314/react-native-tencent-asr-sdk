package com.tencent.cloud.asr.rn.core.realtime;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.tencent.aai.AAIClient;
import com.tencent.aai.audio.utils.WavCache;
import com.tencent.aai.auth.LocalCredentialProvider;
import com.tencent.aai.config.ClientConfiguration;
import com.tencent.aai.exception.ClientException;
import com.tencent.aai.exception.ServerException;
import com.tencent.aai.listener.AudioRecognizeResultListener;
import com.tencent.aai.listener.AudioRecognizeStateListener;
import com.tencent.aai.log.AAILogger;
import com.tencent.aai.model.AudioRecognizeConfiguration;
import com.tencent.aai.model.AudioRecognizeRequest;
import com.tencent.aai.model.AudioRecognizeResult;
import com.tencent.cloud.asr.rn.utils.AsrRNUtils;
import com.tencent.cloud.asr.rn.utils.RNUtils;

import java.io.DataOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 实时语音识别管理类 */
public class AsrRealTimeManager {
  private String TAG = AsrRealTimeManager.class.getName();

  private final ReactApplicationContext applicationContext;
  private String saveFilePath;
  public AsrRealTimeManager(ReactApplicationContext applicationContext){
    this.applicationContext = applicationContext;
  }

  boolean isRecording = false;

  boolean isCompress = true;//音频压缩，默认true

  boolean isOpenSilentCheck = false;
  AAIClient aaiClient;
  boolean isSaveAudioRecordFiles = true;
  Handler handler;

  public void init(int appid,int projectId,String secretId,String secretKey,String saveFilePath){
    this.saveFilePath = saveFilePath;
    // okhttp全局配置
    ClientConfiguration.setAudioRecognizeConnectTimeout(3000);
    ClientConfiguration.setAudioRecognizeWriteTimeout(5000);
    handler = new Handler(Looper.getMainLooper());
    if (aaiClient==null) {
      /**直接鉴权**/
      // 签名鉴权类，sdk中给出了一个本地的鉴权类，您也可以自行实现CredentialProvider接口，在您的服务器上签名
      aaiClient = new AAIClient(applicationContext, appid, projectId, secretId ,new LocalCredentialProvider(secretKey));
    }
  }



  // 识别结果回调监听器
  final AudioRecognizeResultListener audioRecognizeResultlistener = new AudioRecognizeResultListener() {
    /**
     * 返回分片的识别结果
     * @param request 相应的请求
     * @param result 识别结果
     * @param seq 该分片所在句子的序号 (0, 1, 2...)
     *   此为中间态结果，会被持续修正
     */
    @Override
    public void onSliceSuccess(AudioRecognizeRequest request, AudioRecognizeResult result, int seq) {
      WritableMap resultMap = Arguments.createMap();
      WritableMap writableMap = AsrRNUtils.AudioRecognizeResultToWritableMap(result);
      resultMap.putMap("result",writableMap);
      resultMap.putInt("seq",seq);
      sendEvent("asr_RecognizeResult_onSliceSuccess",resultMap);
    }

    /**
     * 返回语音流的识别结果
     * @param request 相应的请求
     * @param result 识别结果
     * @param seq 该句子的序号 (1, 2, 3...)
     *     此为稳定态结果，可做为识别结果用与业务
     */
    @Override
    public void onSegmentSuccess(AudioRecognizeRequest request, AudioRecognizeResult result, int seq) {
      WritableMap resultMap = Arguments.createMap();
      WritableMap writableMap = AsrRNUtils.AudioRecognizeResultToWritableMap(result);
      resultMap.putMap("result",writableMap);
      resultMap.putInt("seq",seq);
      sendEvent("asr_RecognizeResult_onSegmentSuccess",resultMap);
    }

    /**
     * 识别结束回调，返回所有的识别结果
     * @param request 相应的请求
     * @param result 识别结果,sdk内会把所有的onSegmentSuccess结果合并返回，如果业务不需要，可以只使用onSegmentSuccess返回的结果
     *    注意：仅收到onStopRecord回调时，表明本次语音流录音任务已经停止，但识别任务还未停止，需要等待后端返回最终识别结果，
     *               如果此时立即启动下一次录音，结果上一次结果仍会返回，可以调用cancelAudioRecognize取消上一次识别任务
     *         当收到 onSuccess 或者  onFailure时，表明本次语音流识别完毕，可以进行下一次识别；
     */
    @Override
    public void onSuccess(AudioRecognizeRequest request, String result) {
      WritableMap resultMap = Arguments.createMap();
      resultMap.putString("result",result);
      sendEvent("asr_RecognizeResult_onSuccess",resultMap);



//      AAILogger.info(TAG, "识别结束, onSuccess..");
//      AAILogger.info(TAG, "识别结束, result = " + result);
    }

    /**
     * 识别失败
     * @param request 相应的请求
     * @param clientException 客户端异常
     * @param serverException 服务端异常
     * @param response   服务端返回的json字符串（如果有）
     *    注意：仅收到onStopRecord回调时，表明本次语音流录音任务已经停止，但识别任务还未停止，需要等待后端返回最终识别结果，
     *               如果此时立即启动下一次录音，结果上一次结果仍会返回，可以调用cancelAudioRecognize取消上一次识别任务
     *         当收到 onSuccess 或者  onFailure时，表明本次语音流识别完毕，可以进行下一次识别；
     */
    @Override
    public void onFailure(AudioRecognizeRequest request, final ClientException clientException, final ServerException serverException, String response) {
      WritableMap resultMap = Arguments.createMap();
      if(response != null){
        AAILogger.info(TAG, "onFailure response.. :"+response);
      }
      if (clientException!=null) {
        resultMap.putInt("code",clientException.getCode());
        resultMap.putString("message",clientException.getMessage());
        AAILogger.info(TAG, "onFailure..:"+clientException);
      }
      if (serverException!=null) {
        resultMap.putInt("code",0);
        resultMap.putString("message",serverException.getMessage());
        AAILogger.info(TAG, "onFailure..:"+serverException);
      }
      resultMap.putString("response",response);
      sendEvent("asr_RecognizeResult_onFailure",resultMap);

//      handler.post(new Runnable() {
//        @Override
//        public void run() {
//          // 向RN发送消息
////            start.setEnabled(true);
//          if (clientException!=null) {
////              recognizeState.setText("识别状态：失败,  "+clientException);
//            ShowMsg("识别状态：失败,  "+clientException);
//            AAILogger.info(TAG, "识别状态：失败,  "+clientException);
//          } else if (serverException!=null) {
////              recognizeState.setText("识别状态：失败,  "+serverException);
//            ShowMsg("识别状态：失败,  "+serverException);
//          }
//        }
//      });
    }
  };


  /**
   * 识别状态监听器
   */
  final AudioRecognizeStateListener audioRecognizeStateListener = new AudioRecognizeStateListener() {
    DataOutputStream dataOutputStream;
    String fileName = null;
    String filePath = null;
    ExecutorService mExecutorService;

    float minVoiceDb = Float.MAX_VALUE;
    float maxVoiceDb = Float.MIN_VALUE;
    /**
     * 开始录音
     * @param request
     */
    @Override
    public void onStartRecord(AudioRecognizeRequest request) {
      isRecording = true;
      minVoiceDb = Float.MAX_VALUE;
      maxVoiceDb = Float.MIN_VALUE;
      AAILogger.info(TAG, "onStartRecord..");
//      handler.post(() -> {
//        // todo 识别状态，向RN发送发送事件
////          recognizeState.setText(getString(R.string.start_record));
////          start.setEnabled(true);
////          start.setText("STOP");
//      });
      //为本次录音创建缓存一个文件
      if(isSaveAudioRecordFiles) {
        if(mExecutorService == null){
          mExecutorService = Executors.newSingleThreadExecutor();
        }
        if (saveFilePath!=null && !saveFilePath.isEmpty()){
          filePath = saveFilePath;
        }else{
          filePath = applicationContext.getFilesDir() + "/tencent_audio_sdk_cache";
        }
        // todo,存储路径 需要从RN发送过来
//          fileName = mFileName.getText().toString() + ".pcm";
        if(fileName==null || fileName.isEmpty()) {
          fileName = System.currentTimeMillis() + ".pcm";
        }
        dataOutputStream = WavCache.creatPmcFileByPath(filePath, fileName);
      }

      WritableMap resultMap = Arguments.createMap();
      sendEvent("asr_RecognizeState_onStartRecord",resultMap);


    }

    /**
     * 结束录音
     * @param request
     */
    @Override
    public void onStopRecord(AudioRecognizeRequest request) {
      AAILogger.info(TAG, "onStopRecord..");
      isRecording = false;
//      handler.post(() -> {
//        //todo 向RN发送事件
////          recognizeState.setText(getString(R.string.end_record));
//////                    start.setEnabled(true);
////          start.setText("START");
//      });

      String wavfileName = fileName.replace(".pcm",".wav");

      //如果设置了保存音频
      if(isSaveAudioRecordFiles) {
        mExecutorService.execute(() -> {
          WavCache.closeDataOutputStream(dataOutputStream);
//        // makePCMFileToWAVFile fileName 需要传递原始的 .pcm 文件，内部实现了转换 .wav
          WavCache.makePCMFileToWAVFile(filePath, fileName); //sdk内提供了一套pcm转wav工具类,
          WritableMap resultMap = Arguments.createMap();
          resultMap.putString("filePath",filePath);
          resultMap.putString("fileName",wavfileName);  // 给到上层用的时候，需要返回 .wav 文件名
          sendEvent("asr_RecognizeState_onStopRecord",resultMap);
        });
      }else{
        WritableMap resultMap = Arguments.createMap();
        resultMap.putString("filePath","");
        resultMap.putString("fileName","");
        sendEvent("asr_RecognizeState_onStopRecord",resultMap);
      }
    }

    /**
     * 返回音频流，
     * 用于返回宿主层做录音缓存业务。
     * 由于方法跑在sdk线程上，这里多用于文件操作，宿主需要新开一条线程专门用于实现业务逻辑
     * @param audioDatas
     */
    @Override
    public void onNextAudioData(final short[] audioDatas, final int readBufferLength) {
      if(isSaveAudioRecordFiles) {
        mExecutorService.execute(new Runnable() {
          @Override
          public void run() {
            WavCache.savePcmData(dataOutputStream, audioDatas, readBufferLength);
          }
        });
      }
    }

    /**
     * 静音检测回调
     * 当设置AudioRecognizeConfiguration  setSilentDetectTimeOut为true时，如触发静音超时，将触发此回调
     * 当setSilentDetectTimeOutAutoStop 为true时，触发此回调的同时会停止本次识别，相当于手动调用了 aaiClient.stopAudioRecognize()
     */
    @Override
    public void onSilentDetectTimeOut() {
      Log.d(TAG, "onSilentDetectTimeOut: ");
      //您的业务逻辑
    }

    /**
     * 语音音量回调
     * @param request
     * @param volume
     */
    @Override
    public void onVoiceVolume(AudioRecognizeRequest request, final int volume) {
      AAILogger.info(TAG, "onVoiceVolume..");
//      handler.post(new Runnable() {
//        @Override
//        public void run() {
////            todo 语音的音量大小回调函数
////            MainActivity.this.volume.setText(getString(R.string.volume)+volume);
//        }
//      });

      WritableMap resultMap = Arguments.createMap();
      resultMap.putInt("volume",volume);
      sendEvent("asr_RecognizeState_onVoiceVolume",resultMap);
    }

    @Override
    public void onVoiceDb(float volumeDb) {
      AAILogger.info(TAG, "onVoiceDb: " + volumeDb);
//      handler.post(new Runnable() {
//        @Override
//        public void run() {
//          if (volumeDb > maxVoiceDb) {
//            maxVoiceDb = volumeDb;
//          }
//          if (volumeDb < minVoiceDb) {
//            minVoiceDb = volumeDb;
//          }
//          asrRealTimeCallback.onChangeDbCallback(volumeDb);
//          // todo 分贝的回调
////            if (minVoiceDb != Float.MAX_VALUE && maxVoiceDb != Float.MIN_VALUE) {
////              MainActivity.this.voiceDb.setText(getString(R.string.voice_db) + volumeDb
////                + "(" + minVoiceDb + " ~ " + maxVoiceDb + ")");
////            }
//        }
//      });
      WritableMap resultMap = Arguments.createMap();
      resultMap.putString("volumeDb", String.valueOf(volumeDb));
      sendEvent("asr_RecognizeState_onVoiceDb",resultMap);
    }
  };

  public void startRecorder() {
    AAILogger.info(TAG, "the start button has clicked..");
//    handler.post(new Runnable() {
//      @Override
//      public void run() {
//        // todo 发送RN消息
////        start.setEnabled(false);
//      }
//    });

    if (isRecording){
      AAILogger.info(TAG, "stop button is clicked..");
      new Thread(() -> {
        if (aaiClient!=null) {
          aaiClient.stopAudioRecognize();
        }
//        todo 下面的几行代码，暂时用不到
//        if (mp != null) {
//          mp.stop();
//          mp = null;
//        }
      }).start();
    } else {
      if (aaiClient != null) { //丢弃上一次结果
        boolean taskExist = aaiClient.cancelAudioRecognize();
        AAILogger.info(TAG, "taskExist=" + taskExist);
      }

      isSaveAudioRecordFiles = true;
//      todo 是否保存文件
//      if (filePath != null && !filePath.isEmpty()) {
//        isSaveAudioRecordFiles = true;
//      }
      AudioRecordDataSource dataSource = new AudioRecordDataSource(isSaveAudioRecordFiles, applicationContext);

      //todo RN 传递过来的值
//      dataSource.enableAEC(mEnableAEC.isChecked());

      AudioRecognizeRequest.Builder builder = new AudioRecognizeRequest.Builder();
      // 初始化识别请求
      final AudioRecognizeRequest audioRecognizeRequest = builder

        //设置数据源，数据源要求实现PcmAudioDataSource接口，您可以自己实现此接口来定制您的自定义数据源，例如从第三方推流中获取音频数据
        //注意：音频数据必须为16k采样率的PCM音频，否则将导致语音识别输出错误的识别结果！！！！
        .pcmAudioDataSource(dataSource) //使用Demo提供的录音器源码作为数据源，源码与SDK内置录音器一致，您可以参考此源代码自由定制修改，详情查阅AudioRecordDataSource.java内注释
//                        .pcmAudioDataSource(new AudioRecordDataSource(isSaveAudioRecordFiles)) // 使用SDK内置录音器作为数据源
        .setEngineModelType("16k_zh") // 设置引擎(16k_zh--通用引擎，支持中文普通话+英文),更多引擎请关注官网文档https://cloud.tencent.com/document/product/1093/48982 ，引擎种类持续增加中
        .setFilterDirty(0)  // 0 ：默认状态 不过滤脏话 1：过滤脏话
        .setFilterModal(0) // 0 ：默认状态 不过滤语气词  1：过滤部分语气词 2:严格过滤
        .setFilterPunc(0) // 0 ：默认状态 不过滤句末的句号 1：滤句末的句号
        .setConvert_num_mode(1) //1：默认状态 根据场景智能转换为阿拉伯数字；0：全部转为中文数字。
//                        .setVadSilenceTime(1000) // 语音断句检测阈值，静音时长超过该阈值会被认为断句（多用在智能客服场景，需配合 needvad = 1 使用） 默认不传递该参数，不建议更改
        .setNeedvad(1) //0：关闭 vad，1：默认状态 开启 vad。语音时长超过一分钟需要开启,如果对实时性要求较高,并且时间较短
        // 的输入,建议关闭,可以显著降低onSliceSuccess结果返回的时延以及stop后onSegmentSuccess和onSuccess返回的时延
//                        .setHotWordId("")//热词 id。用于调用对应的热词表，如果在调用语音识别服务时，不进行单独的热词 id 设置，自动生效默认热词；如果进行了单独的热词 id 设置，那么将生效单独设置的热词 id。
        .setWordInfo(1)
//                        .setCustomizationId("")//自学习模型 id。如果设置了该参数，那么将生效对应的自学习模型,如果您不了解此参数，请不要设置
//                        .setReinforceHotword(1)
//                        .setNoiseThreshold(0)
//                        .setMaxSpeakTime(5000) // 强制断句功能，取值范围 5000-90000(单位:毫秒），默认值0(不开启)。 在连续说话不间断情况下，该参数将实现强制断句（此时结果变成稳态，slice_type=2）。如：游戏解说场景，解说员持续不间断解说，无法断句的情况下，将此参数设置为10000，则将在每10秒收到 slice_type=2的回调。
        .build();

      // 自定义识别配置
      final AudioRecognizeConfiguration audioRecognizeConfiguration = new AudioRecognizeConfiguration.Builder()
        //分片默认40ms，可设置40-5000,必须为20的整倍数，如果不是，sdk内将自动调整为20的整倍数，例如77将被调整为60，如果您不了解此参数不建议更改
        //.sliceTime(40)
        // 是否使能静音检测，
        .setSilentDetectTimeOut(isOpenSilentCheck)
        //触发静音超时后是否停止识别，默认为true:停止，setSilentDetectTimeOut为true时参数有效
        .setSilentDetectTimeOutAutoStop(true)
        // 静音检测超时可设置>2000ms，setSilentDetectTimeOut为true有效，超过指定时间没有说话超过指定时间没有说话收到onSilentDetectTimeOut回调；需要大于等于sliceTime，实际时间为sliceTime的倍数，如果小于sliceTime，则按sliceTime的时间为准
        .audioFlowSilenceTimeOut(5000)
        // 音量回调时间，需要大于等于sliceTime，实际时间为sliceTime的倍数，如果小于sliceTime，则按sliceTime的时间为准
        .minVolumeCallbackTime(80)
        //是否压缩音频。默认压缩，压缩音频有助于优化弱网或网络不稳定时的识别速度及稳定性
        //SDK历史版本均默认压缩且未提供配置开关，如无特殊需求，建议使用默认值
        .isCompress(isCompress)
        .build();
      //启动识别器
      new Thread(() -> {
        aaiClient.startAudioRecognize(audioRecognizeRequest,
                audioRecognizeResultlistener,
                audioRecognizeStateListener,
                audioRecognizeConfiguration);
      }).start();
    }
  };

  public void stopRecorder(Promise promise){
    new Thread(() -> {
      if (aaiClient!=null) {
        boolean result =  aaiClient.stopAudioRecognize();
        promise.resolve(result);
      }else{
        promise.reject("asr1001","No initialization, no need to stop");
      }
    }).start();
  }

  /** 取消识别，丢弃识别结Z果，不等待最终识别结果返回 */
  public void cancel(Promise promise) {
    new Thread(() -> {
      if (aaiClient!=null) {
        boolean taskExist = aaiClient.cancelAudioRecognize();
        promise.resolve(taskExist);
      } else {
        promise.reject("asr1000","No initialization, no need to cancel");
      }
    }).start();
  };

  public void onDestroy() {
    if (aaiClient != null) {
      aaiClient.release();
    }
  }


  private void sendEvent(String eventName, WritableMap params){
    handler.post(() -> {
//      Log.i(TAG, "asr: 向RN发送事件 [_"+eventName+"_]");
       applicationContext       
          .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);;
//    RNUtils.sendEvent(activity, eventName, params);
    });
  }
}
