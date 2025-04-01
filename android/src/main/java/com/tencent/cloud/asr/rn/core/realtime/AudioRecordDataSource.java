package com.tencent.cloud.asr.rn.core.realtime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.util.Log;

import com.tencent.aai.audio.data.PcmAudioDataSource;
import com.tencent.aai.exception.ClientException;
import com.tencent.aai.exception.ClientExceptionType;
import com.tencent.aai.log.AAILogger;

public class AudioRecordDataSource implements PcmAudioDataSource {

  private String TAG= AudioRecordDataSource.class.getName();

  private static boolean recording = false;

  private static boolean isSetSaveAudioRecordFiles = false;

  private AudioRecord audioRecord;
  private int audioSource ;
  private int sampleRate;
  private int channel;
  private int audioFormat;
  private int bufferSize;
  private boolean enableAEC;

  private Context context;

  public AudioRecordDataSource(boolean isSetSaveAudioRecordFiles, Context context) {
    this.isSetSaveAudioRecordFiles = isSetSaveAudioRecordFiles;
    this.audioSource = MediaRecorder.AudioSource.MIC;
    this.sampleRate = 16000; //设置采样率
    this.channel = AudioFormat.CHANNEL_IN_MONO;//单声道
    this.audioFormat = AudioFormat.ENCODING_PCM_16BIT;//16BIT PCM
    bufferSize = AudioRecord.getMinBufferSize(sampleRate, channel, audioFormat) * 2; //使用允许的最小值2倍作为缓冲区
    if (bufferSize < 0) {
      //无效的采样率缓存数据
      bufferSize = 0;
      AAILogger.error(TAG,"AudioRecord.getMinBufferSize error");
    }
    this.context = context;
  }

  public void enableAEC(boolean val) {
    enableAEC = val;
  }


  /**
   * 将长度为length的语音数据从下标0依次放入到audioPcmData数组中。
   *
   * @param audioPcmData 音频数据缓存数组
   * @param length 理论缓存数据的长度（最大长度）,这里的长度对应的时长与AudioRecognizeConfiguration.sliceTime(xxx)设置的分片时长一致
   * @return 实际缓存数据的长度  -1表示结束
   * 注意：如果您使用自定义数据源（例如第三方推流获取的音频流），而非录音器，为了最好的识别效果，应尽量保证每次读取的数据长度都填充满length
   * ！！！为了保证数据完整性，除最后一个数据包外，每次的读取长度必须为20ms(长度640)的整倍数！！！
   *
   * 如果您没有调整AudioRecognizeConfiguration.sliceTime()设置的分片时长，SDK默认40ms，如果您要调整length的长度，应调用AudioRecognizeConfiguration.sliceTime()调整分片时间
   * 16000 采样率40ms ,长度计算方式--->对应数据长度 = 16000 * (40/1000)  * (16/8*1) = 1280 （length=采样率 * 采样时间 * 采样位数/8*通道数（Bytes））
   *
   * SDK仅支持16000采样率16bit采样深度单通道PCM音频
   */
  @Override
  public int read(short[] audioPcmData, int length) {
    if (audioRecord == null) {
      return -1;
    }else  {
      //audioRecord.read方法会阻塞，直到填充满length长度数据才会返回
      return audioRecord.read(audioPcmData, 0, length);
    }
  }

  /**
   * 用户在开始识别后，sdk会主动调用该方法，用户可以在这里做一些初始化的工作
   * @throws ClientException
   */
  @SuppressLint("MissingPermission")
  @Override
  public void start() throws ClientException {

    if (recording) {
      throw new ClientException(ClientExceptionType.AUDIO_RECORD_MULTIPLE_START);
    }

    if (enableAEC) {
      audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
      /**
       * 注: 部分android机型可以通过该方式解决回音消除失效的问题
       * https://blog.csdn.net/wyw0000/article/details/125195997
       */
      AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
      audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    } else {
      audioSource = MediaRecorder.AudioSource.MIC;
    }

    /**
     *  注：这里做个判断,以规避系统bug
     *  部分android 10设备开启无障碍服务后，通过MIC或者DEFAULT获取到的音频数据为空 例如OPPO R15x, Pixel 3
     *  https://stackoverflow.com/questions/61673599/accessibilityservice-turned-on-causes-silence-from-microphone-on-android-10
     * */

    if (Build.VERSION.SDK_INT  == 29 /*Build.VERSION_CODES.Q*/) {
      audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, sampleRate, channel, audioFormat, bufferSize);
    }
    else{
      audioRecord = new AudioRecord(audioSource, sampleRate, channel, audioFormat, bufferSize);
    }

    if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
      throw new ClientException(ClientExceptionType.AUDIO_RECORD_INIT_FAILED);
    }

    if (recording || audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
      throw new ClientException(ClientExceptionType.AUDIO_RECORD_START_FAILED);
    }

    boolean aecAvailable = false;
    boolean noiseSuppressorAvailable = false;
    if (enableAEC) {
      /**
       * 注: 以下两个能力(AcousticEchoCanceler和NoiseSuppressor)和手机硬件能力相关，有些机型(比如小米11)即使isAvailable()==true，回音消除也不生效
       */
      if (AcousticEchoCanceler.isAvailable()) {
        Log.d(TAG, "AcousticEchoCanceler isAvailable.");
        AcousticEchoCanceler acousticEchoCanceler = AcousticEchoCanceler
          .create(audioRecord.getAudioSessionId());
        if (acousticEchoCanceler != null) {
          int resultCode = acousticEchoCanceler.setEnabled(true);
          if (AudioEffect.SUCCESS == resultCode) {
            Log.d(TAG, "AcousticEchoCanceler AudioEffect.SUCCESS");
            aecAvailable = true;
          }
        } else {
          Log.d(TAG, "AcousticEchoCanceler create failed.");
        }
      }

      if (NoiseSuppressor.isAvailable()) {
        Log.d(TAG, "NoiseSuppressor isAvailable.");
        NoiseSuppressor noiseSuppressor = NoiseSuppressor
          .create(audioRecord.getAudioSessionId());
        if (noiseSuppressor != null) {
          int resultCode = noiseSuppressor.setEnabled(true);
          if (AudioEffect.SUCCESS == resultCode) {
            Log.d(TAG, "NoiseSuppressor AudioEffect.SUCCESS");
            noiseSuppressorAvailable = true;
          }
        } else {
          Log.d(TAG, "NoiseSuppressor create failed.");
        }
      }

      if (!aecAvailable) {
        Log.d(TAG, "AcousticEchoCanceler is not available.");
      }

      if (!noiseSuppressorAvailable) {
        Log.d(TAG, "NoiseSuppressor is not available.");
      }
    }
    recording = true;
    try {
      audioRecord.startRecording();
    } catch (IllegalStateException e) {
      throw new ClientException(ClientExceptionType.AUDIO_RECORD_START_FAILED);
    }
  }

  /**
   * 用户在停止识别后，sdk会主动调用该方法，用户可以在这里做一些数据清理工作
   */
  @Override
  public void stop() {
    if (audioRecord != null){
      audioRecord.stop();
      audioRecord.release();
    }
    audioRecord = null;
    recording = false;
  }

  /**
   *  是否保存语音源文件的开关，打开后，音频数据将通过onNextAudioData回调返回给调用层；
   */
  @Override
  public boolean isSetSaveAudioRecordFiles() {
    return isSetSaveAudioRecordFiles;
  }

}
