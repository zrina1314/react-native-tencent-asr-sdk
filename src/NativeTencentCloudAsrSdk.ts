import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  /**
   * 初始化
   * @param appid 腾讯ASR SDK的：appid
   * @param projectId 腾讯ASR SDK的：projectId - （默认为：0）
   * @param secretId 腾讯ASR SDK的：secretId
   * @param secretKey 腾讯ASR SDK的：secretKey
   * @param saveFilePath 音频文件保存到本地的 文件夹 目录路径
   *
   */
  setup(
    appid: number,
    projectId: number,
    secretId: string,
    secretKey: string,
    saveFilePath: string
  ): boolean;
  // 开始录音
  startRecording(): number;
  // 停止录音
  stopRecording(): number;
  /** 取消录音 */
  cancelRecording(): number;

  /**
   * 识别音频文件
   * @param filePath 完整的文件路径 "file:///var/mobile/.../Library/Caches/gaw_383_17...ssxs0.wav"
   * @param voiceFormat 识别音频的音频格式(支持wav、pcm、ogg-opus、speex、silk、mp3、m4a、aac)
   * @param engSerViceType 默认"16k_zh"，更多引擎参数详见https://cloud.tencent.com/document/product/1093/35646 内的EngSerViceType字段
   */
  recognizerFile(
    filePath: string,
    voiceFormat: string,
    engSerViceType: string
  ): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'TencentCloudAsrSdk'
) as Spec;
