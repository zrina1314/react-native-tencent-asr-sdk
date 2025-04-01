#import "TencentCloudAsrSdk.h"
#import "AsrRealTimeManager.h"
#import "AsrSentenceManager.h"
#import <React/RCTLog.h>

@interface TencentCloudAsrSdk()

@property (nonatomic, assign) int appid;
@property (nonatomic, assign) int projectId;
@property (nonatomic, strong) NSString *secretId;
@property (nonatomic, strong) NSString *secretKey;
@property (nonatomic, strong) NSString *saveFilePath;

@property (nonatomic, strong) AsrRealTimeManager *asrRealTimeManager;

@property (nonatomic, strong) AsrSentenceManager *asrSentenceManager;

@end

@implementation TencentCloudAsrSdk
RCT_EXPORT_MODULE()

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeTencentCloudAsrSdkSpecJSI>(params);
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(setup:(double)appid projectId:(double)projectId secretId:(NSString *)secretId secretKey:(NSString *)secretKey saveFilePath:(NSString *)saveFilePath)
{

  _appid = appid;
  _projectId = projectId;
  _secretId = secretId;
  _secretKey = secretKey;
  _saveFilePath = saveFilePath;
  if (!saveFilePath || saveFilePath.length == 0) {
    _saveFilePath = NSTemporaryDirectory();
  }

  return @(1);
}


RCT_EXPORT_METHOD(recognizerFile:(NSString *)filePath voiceFormat:(NSString *)voiceFormat engSerViceType:(NSString *)engSerViceType resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
  [self initSentenceSDK];

  [_asrSentenceManager recognizerWithFilePath:filePath voiceFormat:voiceFormat engSerViceType:engSerViceType resolve:^(id result) {
      resolve(result);
  } rejecter:^(NSString *code, NSString *message, NSError *error) {
      reject(code,message,error);
  }];
}


RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(startRecording) {
  [self initRealTimeSDK];
  [_asrRealTimeManager startRecorder];
  return @(1);
}


RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(stopRecording) {
  [self initRealTimeSDK];
  [_asrRealTimeManager stopRecorder];
  return @(1);
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(cancelRecording) {
  [self initRealTimeSDK];
  [_asrRealTimeManager cancel];
  return @(1);
}

#pragma mark - init
//
- (void)initRealTimeSDK{
  if (!_asrRealTimeManager){
      [AsrRealTimeManager checkPermissons];
      _asrRealTimeManager = [[AsrRealTimeManager alloc] initWithAppid:_appid projectId:_projectId secretId:_secretId secretKey:_secretKey saveFilePath:_saveFilePath];
  }
}

//
- (void)initSentenceSDK{
    if (!_asrSentenceManager){
        _asrSentenceManager = [[AsrSentenceManager alloc] initWithAppid:_appid projectId:_projectId secretId:_secretId secretKey:_secretKey];
    }
}


@end
