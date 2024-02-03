import { DeviceEventEmitter } from 'react-native';
import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-tencent-asr-sdk' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// @ts-expect-error
const isTurboModuleEnabled = global.__turboModuleProxy != null;

const TencentAsrSdkModule = isTurboModuleEnabled
  ? require('./NativeTencentAsrSdk').default
  : NativeModules.TencentAsrSdk;

const TencentAsrSdk = TencentAsrSdkModule
  ? TencentAsrSdkModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );


// const TencentAsrSdkEmitter = new NativeEventEmitter(TencentAsrSdkModule);
// const TencentAsrSdkEmitter = new NativeEventEmitter();
// const Emitter = Platform.OS === 'ios' ? TencentAsrSdkEmitter : DeviceEventEmitter;
const Emitter = DeviceEventEmitter;
export namespace AsrSdk {
  export interface AudioRecognizeResult {
    voiceId: string;
    seq: number;
    text: string;
    sliceType: number;
    startTime: number;
    endTime: number;
    code: number;
    message: string;
    resultJson: string;
  }

  export interface RecorderListener {
    /**
     * 返回分片的识别结果
     * @param request 相应的请求
     * @param result 识别结果
     * @param seq 该分片所在句子的序号 (0, 1, 2...)
     *   此为中间态结果，会被持续修正
     */
    onRecognizeResult_onSliceSuccess?: (params: {
      result: AudioRecognizeResult;
      seq: number;
    }) => void;
    /**
     * 返回语音流的识别结果
     * @param request 相应的请求
     * @param result 识别结果
     * @param seq 该句子的序号 (1, 2, 3...)
     *     此为稳定态结果，可做为识别结果用与业务
     */
    onRecognizeResult_onSegmentSuccess?: (params: {
      result: AudioRecognizeResult;
      seq: number;
    }) => void;
    /**
     * 识别结束回调，返回所有的识别结果
     * @param request 相应的请求
     * @param result 识别结果,sdk内会把所有的onSegmentSuccess结果合并返回，如果业务不需要，可以只使用onSegmentSuccess返回的结果
     *    注意：仅收到onStopRecord回调时，表明本次语音流录音任务已经停止，但识别任务还未停止，需要等待后端返回最终识别结果，
     *               如果此时立即启动下一次录音，结果上一次结果仍会返回，可以调用cancelAudioRecognize取消上一次识别任务
     *         当收到 onSuccess 或者  onFailure时，表明本次语音流识别完毕，可以进行下一次识别；
     */
    onRecognizeResult_onSuccess?: (params: { result: string }) => void;
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
    onRecognizeResult_onFailure?: (params: { code: string; message: string }) => void;
    /**
     * 开始录音
     * @param request
     */
    onRecognizeState_onStartRecord?: (params: {}) => void;
    /**
     * 结束录音
     * @param filePath 文件存储路径
     * @param fileName 文件名称 - (后缀名：".wav")
     * @param duration 录音时长
     */
    onRecognizeState_onStopRecord?: (params: {
      filePath: string;
      fileName: string;
      duration: number;
    }) => void;
    /**
     * 语音音量回调
     * @param volume
     */
    onRecognizeState_onVoiceVolume?: (params: { volume: number }) => void;
    /**
     * 语音分贝回调
     * @param volumeDb
     */
    onRecognizeState_onVoiceDb?: (params: { volumeDb: string }) => void;
  }

  let mListener: RecorderListener | undefined;

  let asr_RecognizeResult_onSliceSuccess: any;
  let asr_RecognizeResult_onSegmentSuccess: any;
  let asr_RecognizeResult_onSuccess: any;
  let asr_RecognizeResult_onFailure: any;
  let asr_RecognizeState_onStartRecord: any;
  let asr_RecognizeState_onStopRecord: any;
  let asr_RecognizeState_onVoiceVolume: any;
  let asr_RecognizeState_onVoiceDb: any;

  /** 开始录音 */
  export const init = (
    appid: number,
    projectId: number,
    secretId: string,
    secretKey: string,
    saveFilePath: string
  ): Promise<number> => {
    return TencentAsrSdk.init(appid, projectId, secretId, secretKey, saveFilePath);
  };

  /** 开始录音 */
  export const startRecorder = () => {
    if (Platform.OS === 'ios') return;
    TencentAsrSdk.startRecorder();
  };
  /** 结束录音 */
  export const stopRecorder = async (): Promise<number> => {
    if (Platform.OS === 'ios') return 0;
    return TencentAsrSdk.stopRecorder();
  };

  /** 取消录音 */
  export const cancelRecorder = async (): Promise<number> => {
    if (Platform.OS === 'ios') return 0;
    return TencentAsrSdk.cancelRecorder();
  };

  /**
   * 识别音频文件转文字
   * @param filePath 文件路径
   * @param voiceFormat 识别音频的音频格式 (支持wav、pcm、ogg-opus、speex、silk、mp3、m4a、aac)
   * @return 识别出来的文字
   */
  export const recognizerFile = async (filePath: string, voiceFormat: string): Promise<string> => {
    return TencentAsrSdk.recognizerFile(filePath, voiceFormat);
  };

  let beginDate: number = 0;

  export const addListener = (listener: RecorderListener | (() => RecorderListener)) => {
    mListener = typeof listener === 'function' ? listener() : listener;

    asr_RecognizeResult_onSliceSuccess = Emitter.addListener(
      'asr_RecognizeResult_onSliceSuccess',
      (result: { result: AudioRecognizeResult; seq: number }) => {
        // console.log(`事件监听asr_RecognizeResult_onSliceSuccess`, result);
        mListener?.onRecognizeResult_onSliceSuccess?.(result);
      }
    );

    asr_RecognizeResult_onSegmentSuccess = Emitter.addListener(
      'asr_RecognizeResult_onSegmentSuccess',
      (result: { result: AudioRecognizeResult; seq: number }) => {
        // console.log(`事件监听asr_RecognizeResult_onSegmentSuccess`, result);
        mListener?.onRecognizeResult_onSegmentSuccess?.(result);
      }
    );

    asr_RecognizeResult_onSuccess = Emitter.addListener('asr_RecognizeResult_onSuccess', (params: { result: string }) => {
      // console.log(`事件监听asr_RecognizeResult_onSuccess`, e);
      mListener?.onRecognizeResult_onSuccess?.(params);
    });

    asr_RecognizeResult_onFailure = Emitter.addListener('asr_RecognizeResult_onFailure', (params: { code: string; message: string }) => {
      // console.log(`事件监听asr_RecognizeResult_onFailure`, e);
      mListener?.onRecognizeResult_onFailure?.(params);
    });

    asr_RecognizeState_onStartRecord = Emitter.addListener(
      'asr_RecognizeState_onStartRecord',
      (params: {}) => {
        // console.log(`事件监听asr_RecognizeState_onStartRecord`, e);
        beginDate = new Date().getTime();
        mListener?.onRecognizeState_onStartRecord?.(params);
      }
    );

    asr_RecognizeState_onStopRecord = Emitter.addListener(
      'asr_RecognizeState_onStopRecord',
      (result: { filePath: string; fileName: string }) => {
        // console.log(`事件监听asr_RecognizeState_onStopRecord`, result);
        let duration = 0;
        if (beginDate) {
          const endTime = new Date().getTime();
          duration = endTime - beginDate;
        }
        mListener?.onRecognizeState_onStopRecord?.({ ...result, duration });
      }
    );

    asr_RecognizeState_onVoiceVolume = Emitter.addListener(
      'asr_RecognizeState_onVoiceVolume',
      (params: { volume: number }) => {
        mListener?.onRecognizeState_onVoiceVolume?.(params);
      }
    );

    asr_RecognizeState_onVoiceDb = Emitter.addListener('asr_RecognizeState_onVoiceDb', (params: { volumeDb: string }) => {
      mListener?.onRecognizeState_onVoiceDb?.(params);
    });
  };

  export const removeListener = () => {
    // console.log(`移除所有事件`);
    mListener = undefined;
    asr_RecognizeResult_onSliceSuccess?.remove();
    asr_RecognizeResult_onSliceSuccess = undefined;
    asr_RecognizeResult_onSegmentSuccess?.remove();
    asr_RecognizeResult_onSegmentSuccess = undefined;
    asr_RecognizeResult_onSuccess?.remove();
    asr_RecognizeResult_onSuccess = undefined;
    asr_RecognizeResult_onFailure?.remove();
    asr_RecognizeResult_onFailure = undefined;
    asr_RecognizeState_onStartRecord?.remove();
    asr_RecognizeState_onStartRecord = undefined;
    asr_RecognizeState_onStopRecord?.remove();
    asr_RecognizeState_onStopRecord = undefined;
    asr_RecognizeState_onVoiceVolume?.remove();
    asr_RecognizeState_onVoiceVolume = undefined;
    asr_RecognizeState_onVoiceDb?.remove();
    asr_RecognizeState_onVoiceDb = undefined;
  };
}
