//
//  AsrRealTimeManager.m
//  TencentCloudAsrSdk
//
//  Created by Eggsy on 2024/2/22.
//
#import "AsrRealTimeManager.h"
#import <QCloudRealTime/QCloudRealTimeRecognizer.h>
#import <QCloudRealTime/QCloudConfig.h>
#import <QCloudRealTime/QCloudRealTimeResult.h>
#import <QCloudRealTime/QCloudAudioDataSource.h>
#import <AVFoundation/AVFoundation.h>
#import <React/RCTLog.h>

#define AsrEmitterMsg @"AsrEmitterMsg"
#define AsrEventName @"AsrEventName"

#define asr_RecognizeResult_onSliceSuccess @"asr_RecognizeResult_onSliceSuccess"
#define asr_RecognizeResult_onSegmentSuccess @"asr_RecognizeResult_onSegmentSuccess"
#define asr_RecognizeResult_onSuccess @"asr_RecognizeResult_onSuccess"
#define asr_RecognizeResult_onFailure @"asr_RecognizeResult_onFailure"
#define asr_RecognizeState_onStartRecord @"asr_RecognizeState_onStartRecord"
#define asr_RecognizeState_onStopRecord @"asr_RecognizeState_onStopRecord"
#define asr_RecognizeState_onVoiceDb @"asr_RecognizeState_onVoiceDb"
#define asr_RecognizeState_onVoiceVolume @"asr_RecognizeState_onVoiceVolume"


@interface AsrRealTimeManager ()<QCloudRealTimeRecognizerDelegate>

@property (nonatomic, strong) QCloudRealTimeRecognizer *realTimeRecognizer;

@property (nonatomic, assign) BOOL isRecording;

@property (nonatomic, strong) NSString *saveFilePath;
@property (nonatomic, strong) NSString *saveFileName;

@end

@implementation AsrRealTimeManager

RCT_EXPORT_MODULE();

#pragma mark - RCTEventEmitter supportedEvents

//注册本地通知来实现在其他模块中调用sendEventWithName的方法
- (void)startObserving{
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(emitEventInternal:)
                                                 name:AsrEmitterMsg
                                               object:nil];
}
- (void)stopObserving{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

-(void)emitEventInternal:(NSNotification *)notification
{
    NSString * eventName = [notification.userInfo objectForKey:AsrEventName];
    [self sendEventWithName:eventName
                       body:notification.userInfo];
}

- (NSArray<NSString *> *)supportedEvents {
  return @[asr_RecognizeResult_onSliceSuccess, asr_RecognizeResult_onSegmentSuccess, asr_RecognizeState_onVoiceDb, asr_RecognizeState_onVoiceVolume,asr_RecognizeState_onStartRecord, asr_RecognizeState_onStopRecord,asr_RecognizeResult_onFailure,asr_RecognizeResult_onSuccess];
}

#pragma mark - Native Method

-(instancetype)initWithAppid:(int)appid projectId:(int)projectId secretId:(NSString*)secretId secretKey:(NSString*)secretKey saveFilePath:(NSString*)saveFilePath{
    if (self = [super init]) {
        
        if (!saveFilePath){
          _saveFilePath = [NSString stringWithFormat:@"%@%@",NSTemporaryDirectory(), @"tencent_audio_sdk_cache"];
        }else{
            _saveFilePath = saveFilePath;
        }
        
        NSString * appIdStr = [NSString stringWithFormat:@"%d",appid];
        QCloudConfig *config = [[QCloudConfig alloc] initWithAppId:appIdStr
                                  secretId: secretId
                                  secretKey: secretKey
                                  projectId: projectId];
        config.shouldSaveAsFile = YES;
        config.endRecognizeWhenDetectSilence = NO;
        config.enableDetectVolume = YES;
        _realTimeRecognizer = [[QCloudRealTimeRecognizer alloc] initWithConfig:config];
        _realTimeRecognizer.delegate = self;
//        [_realTimeRecognizer EnableDebugLog:YES];
    }
    return  self;
}

-(void)startRecorder{
    if (_isRecording) {
        [_realTimeRecognizer stop];
    } else {
      _saveFileName = [NSString stringWithFormat:@"%ld%@",(NSInteger)[[NSDate date] timeIntervalSince1970]*1000,@".wav"];
      [_realTimeRecognizer start];
    }
}

-(void)stopRecorder{
    if (_isRecording) {
        [_realTimeRecognizer stop];
    }
}

-(void)cancel{
    [_realTimeRecognizer stop];
    
}

+(void)checkPermissons{
    NSError *error = nil;
    [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryRecord error:&error];
    if (error) {
        NSLog(@"AVAudioSession setCategory error %@", error);
    }
    [[AVAudioSession sharedInstance] setActive:YES error:nil];
}

#pragma mark - QCloudRealTimeRecognizerDelegate

/**
 * 每个语音包分片识别结果
 * @param result 语音分片的识别结果（非稳态结果，会持续修正）
 */
- (void)realTimeRecognizerOnSliceRecognize:(QCloudRealTimeRecognizer *)recognizer result:(QCloudRealTimeResult *)result
{
  NSDictionary * body = @{AsrEventName:asr_RecognizeResult_onSliceSuccess, @"seq":[NSNumber numberWithInteger:result.seq], @"result":@{@"text":result.recognizedText ?: @""}};
    [[NSNotificationCenter defaultCenter] postNotificationName:AsrEmitterMsg
                                                           object:self
                                                         userInfo:body];
}

/**
 * 开始录音回调
 * @param recognizer 实时语音识别实例
 * @param error 开启录音失败，错误信息
 */
- (void)realTimeRecognizerDidStartRecord:(QCloudRealTimeRecognizer *)recorder error:(NSError *)error
{
    if (!error) {
        _isRecording = YES;
    }
    NSDictionary * body = @{AsrEventName:asr_RecognizeState_onStartRecord};
    [[NSNotificationCenter defaultCenter] postNotificationName:AsrEmitterMsg
                                                           object:self
                                                         userInfo:body];

}

/**
 * 结束录音回调
 * @param recognizer 实时语音识别实例
 */
- (void)realTimeRecognizerDidStopRecord:(QCloudRealTimeRecognizer *)recorder
{
    _isRecording = NO;

}

/**
 * 录音停止后回调一次，再次开始录音会清空上一次保存的文件
 * @param recognizer 实时语音识别实例
 * @param audioFilePath 音频文件路径
 */
- (void)realTimeRecognizerDidSaveAudioDataAsFile:(QCloudRealTimeRecognizer *)recognizer
                                   audioFilePath:(NSString *)audioFilePath{

    // 保存录音文件到本地，防止语音消息因为网络问题发送失败，临时文件会被清除
    NSFileManager *fileManager = [NSFileManager defaultManager];
    if (![[NSFileManager defaultManager] fileExistsAtPath:_saveFilePath]) {
            
        [fileManager createDirectoryAtPath:_saveFilePath withIntermediateDirectories:YES attributes:nil error:nil];
            
    }else{
        NSLog(@"file Exists");
    }
    NSData *data = [NSData dataWithContentsOfFile:audioFilePath];
    [fileManager createFileAtPath:[NSString stringWithFormat:@"%@/%@", _saveFilePath, _saveFileName] contents:data attributes:nil];
    NSDictionary * body = @{AsrEventName:asr_RecognizeState_onStopRecord, @"filePath":_saveFilePath,@"fileName":_saveFileName};
    [[NSNotificationCenter defaultCenter] postNotificationName:AsrEmitterMsg
                                                               object:self
                                                             userInfo:body];
}
/**
 * 录音音量实时回调用
 * @param recognizer 实时语音识别实例
 * @param volume 声音音量，取值范围（-40-0)
 */
- (void)realTimeRecognizerDidUpdateVolume:(QCloudRealTimeRecognizer *)recognizer volume:(float)volume{
    NSDictionary * body = @{AsrEventName:asr_RecognizeState_onVoiceVolume, @"volume":[NSString stringWithFormat:@"%lf",volume]};
    [[NSNotificationCenter defaultCenter] postNotificationName:AsrEmitterMsg
                                                           object:self
                                                         userInfo:body];
}
/**
 * 录音音量实时回调用
 * @param recognizer 实时语音识别实例
 * @param volume 声音音量，计算方式如下$A_{i}$为采集音频振幅值
 * $$A_{mean} = \frac{1}{n} \sum_{i=1}^{n} A_{i}^{2}$$
 * $$volume=\max (10*\log_{10}(A_{mean}), 0)$$
 */
- (void)realTimeRecognizerDidUpdateVolumeDB:(QCloudRealTimeRecognizer *)recognizer volume:(float)volume
{
    NSDictionary * body = @{AsrEventName:asr_RecognizeState_onVoiceDb, @"volumeDb":[NSString stringWithFormat:@"%lf",volume]};
    [[NSNotificationCenter defaultCenter] postNotificationName:AsrEmitterMsg
                                                           object:self
                                                         userInfo:body];
}

/**
 * 语音流的开始识别
 * @param recognizer 实时语音识别实例
 * @param voiceId 语音流对应的voiceId，唯一标识
 * @param seq flow的序列号
 */
- (void)realTimeRecognizerOnFlowRecognizeStart:(QCloudRealTimeRecognizer *)recognizer voiceId:(NSString *)voiceId seq:(NSInteger)seq
{
    NSLog(@"realTimeRecognizerOnFlowRecognizeStart:%@ seq:%ld", voiceId, seq);
}

/**
 * 语音流的结束识别
 * @param recognizer 实时语音识别实例
 * @param voiceId 语音流对应的voiceId，唯一标识
 * @param seq flow的序列号
 */
- (void)realTimeRecognizerOnFlowRecognizeEnd:(QCloudRealTimeRecognizer *)recognizer voiceId:(NSString *)voiceId seq:(NSInteger)seq
{
    NSLog(@"realTimeRecognizerOnFlowRecognizeEnd:%@ seq:%ld", voiceId, seq);
}

/**
 * 语音流的识别结果
 * 一次识别中可以包括多句话，这里持续返回的每句话的识别结果
 * @param recognizer 实时语音识别实例
 * @param result 语音分片的识别结果 （稳态结果）
 */
- (void)realTimeRecognizerOnSegmentSuccessRecognize:(QCloudRealTimeRecognizer *)recognizer result:(QCloudRealTimeResult *)result
{
    QCloudRealTimeResultResponse *currentResult = [result.resultList firstObject];
    NSLog(@"realTimeRecognizerOnSegmentSuccessRecognize:%@ index:%ld", currentResult.voiceTextStr, currentResult.index);
  NSDictionary * body = @{AsrEventName:asr_RecognizeResult_onSegmentSuccess, @"seq":[NSNumber numberWithInteger:result.seq], @"result":@{@"text":currentResult.voiceTextStr ?: @""}};
    [[NSNotificationCenter defaultCenter] postNotificationName:AsrEmitterMsg
                                                           object:self
                                                         userInfo:body];
}

/**
 * 一次识别成功回调
 @param recognizer 实时语音识别实例
 @param result 一次识别出的总文本, 实际是由SDK本地处理，将本次识别的realTimeRecognizerOnSegmentSuccessRecognize 识别结果拼接后一次性返回
 */
- (void)realTimeRecognizerDidFinish:(QCloudRealTimeRecognizer *)recorder result:(NSString *)result
{
    NSLog(@"realTimeRecognizerDidFinish:%@", result);
  NSDictionary * body = @{AsrEventName:asr_RecognizeResult_onSuccess,  @"result":result ?: @""};
    [[NSNotificationCenter defaultCenter] postNotificationName:AsrEmitterMsg
                                                           object:self
                                                         userInfo:body];
}

/**
 * 一次识别失败回调
 * @param recognizer 实时语音识别实例
 * @param result 识别结果信息，错误信息详情看QCloudRealTimeResponse内错误码
 */
- (void)realTimeRecognizerDidError:(QCloudRealTimeRecognizer *)recognizer result:(QCloudRealTimeResult *)result;
{
    NSString* msg = nil;
    NSString* code = nil;
    NSString* message = nil;
    if(result.clientErrCode != QCloudRealTimeClientErrCode_Success){ //客户端返回的错误
        msg = [NSString stringWithFormat:@"realTimeRecognizerDidError:code=%@ errmsg=%@", @(result.clientErrCode),result.clientErrMessage];
        NSLog(@"%@", msg);
        code = [NSString stringWithFormat:@"%ld",(long)result.clientErrCode];
        message = result.clientErrMessage;
        
    }else{ //后端返回的错误
        msg = [NSString stringWithFormat:@"realTimeRecognizerDidError:code=%@ errmsg=%@", @(result.code),result.message];
        NSLog(@"%@", msg);
        code = [NSString stringWithFormat:@"%ld",(long)result.code];
        message = result.message;
    }
  NSDictionary * body =@{AsrEventName:asr_RecognizeResult_onFailure,@"code":code,@"message":message ?: @"" ,@"response":result};
    [[NSNotificationCenter defaultCenter] postNotificationName:AsrEmitterMsg
                                                           object:self
                                                         userInfo:body];
}

/**
 * 触发静音事件时会回调
 */
-(void)realTimeRecognizerOnSliceDetectTimeOut{
    NSLog(@"realTimeRecognizeronSliceDetectTimeOut：触发了静音超时");
    //当QCloudConfig.endRecognizeWhenDetectSilence 打开时，触发静音超时事件会回调此事件
    //当QCloudConfig.endRecognizeWhenDetectSilenceAutoStop 打开时，回调此事件的同时会停止本次识别，此配置默认打开
    
}

/**
 * 日志输出
 * @param log 日志
 */
- (void)realTimeRecgnizerLogOutPutWithLog:(NSString *)log{
    
    NSLog(@"realTimeRecgnizerLog=====%@",log);
}

@end
