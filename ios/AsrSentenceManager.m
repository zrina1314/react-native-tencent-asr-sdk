//
//  AsrSentenceManager.m
//  TencentCloudAsrSdk
//
//  Created by Eggsy on 2024/2/23.
//


#import "AsrSentenceManager.h"
#import <QCloudOneSentence/QCloudSentenceRecognizeParams.h>
#import <QCloudOneSentence/QCloudSentenceRecognizer.h>

@interface AsrSentenceManager ()<QCloudSentenceRecognizerDelegate>

@property (nonatomic, strong) QCloudSentenceRecognizer *recognizer;

@property (nonatomic, copy) RCTPromiseResolveBlock resolve;
@property (nonatomic, copy) RCTPromiseRejectBlock reject;

@end

@implementation AsrSentenceManager

-(instancetype)initWithAppid:(int)appid projectId:(int)projectId secretId:(NSString*)secretId secretKey:(NSString*)secretKey{
    if (self = [super init]) {
        NSString * appIdStr = [NSString stringWithFormat:@"%d",appid];
        _recognizer = [[QCloudSentenceRecognizer alloc] initWithAppId:appIdStr secretId:secretId secretKey:secretKey];
        _recognizer.delegate = self;
//        [_recognizer EnableDebugLog:YES];
    }
    return  self;
}

-(void)recognizerWithFilePath:(NSString*)filePath voiceFormat:(NSString*)voiceFormat engSerViceType:(NSString*)engSerViceType resolve:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject{
    
    _resolve = resolve;
    _reject = reject;
    
    NSData *audioData = [[NSData alloc] initWithContentsOfFile:filePath];
    NSString * tempEngSerViceType = [NSString stringWithFormat:@"%@",engSerViceType].length >0 ? engSerViceType : @"16k_zh";
    //获取一个已设置默认参数params
    QCloudSentenceRecognizeParams *params = [_recognizer defaultRecognitionParams];
    params.data = audioData;
    params.voiceFormat = voiceFormat;
    params.sourceType = QCloudAudioSourceTypeAudioData;
    params.engSerViceType = tempEngSerViceType;
    //以下参数选填
    params.filterDirty = 0; //是否过滤脏词
    params.filterModal = 0;//是否过语气词
    params.filterPunc = 0; //是否过滤标点符号
    params.convertNumMode = 1;//是否进行阿拉伯数字智能转换
    params.wordInfo = 1;//是否显示词级别时间戳,
    //params.hotwordId = @"" //热词id
    params.reinforceHotword = 1;// 开启热词增强
    
    BOOL didStart = [_recognizer recognizeWithParams:params];
}

#pragma mark - QCloudOneSentenceRecognizerDelegate
- (void)oneSentenceRecognizerDidRecognize:(QCloudSentenceRecognizer *)recognizer text:(NSString *)text error:(NSError *)error resultData:(NSDictionary *)resultData
{
    
    NSString *rawDataString = @"";
    if (resultData) {
        NSData *jsonData = [NSJSONSerialization dataWithJSONObject:resultData options:0 error:nil];
        rawDataString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    }
//    NSLog(@"oneSentenceRecognizerDidRecognize 识别结果: %@ error : %@ callStackSymbols:%@", rawDataString, error, [NSThread callStackSymbols]);
    if (error) {
        NSLog(@"oneSentenceRecognizerDidRecognize 识别结果: error: %@ resultData:%@", [error localizedDescription], rawDataString);
       self.reject([NSString stringWithFormat:@"%ld",(long)error.code],error.localizedDescription,error);
    }
    else {
        NSLog(@"oneSentenceRecognizerDidRecognize 识别结果: %@", text);
       self.resolve(rawDataString);
    }
}


- (void)oneSentenceRecognizerDidStartRecord:(QCloudSentenceRecognizer *)recognizer error:(NSError *)error
{
    NSLog(@"oneSentenceRecognizerDidStartRecord");
    if (error) {
        NSLog(@"oneSentenceRecognizerDidStartRecord error %@", error);
    }
    else {
        //一句话识别音频文件限制60s以内，超过60s将会识别失败，这里做个定时限制
//        [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(AutoStop) object:nil];
//        [self performSelector:@selector(AutoStop) withObject:nil afterDelay:59];
    }
}

- (void)oneSentenceRecognizerDidEndRecord:(QCloudSentenceRecognizer *)recognizer audioFilePath:(nonnull NSString *)audioFilePath
{
    NSLog(@"oneSentenceRecognizerDidEndRecord audioFilePath %@",audioFilePath);
}

- (void)oneSentenceRecognizerDidUpdateVolume:(QCloudSentenceRecognizer *)recognizer volume:(float)volume
{
    NSLog(@"oneSentenceRecognizerDidUpdateVolume %f", volume);
}
-(void)SentenceRecgnizerLogOutPutWithLog:(NSString *)log
{
    NSLog(@"log====%@",log);
}

@end
