//
//  AsrSentenceManager.h
//  TencentCloudAsrSdk
//
//  Created by Eggsy on 2024/2/23.
//
#import <Foundation/Foundation.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTBridgeModule.h>

@interface AsrSentenceManager : RCTEventEmitter<RCTBridgeModule>

-(instancetype)initWithAppid:(int)appid projectId:(int)projectId secretId:(NSString*)secretId secretKey:(NSString*)secretKey;

-(void)recognizerWithFilePath:(NSString*)filePath voiceFormat:(NSString*)voiceFormat engSerViceType:(NSString*)engSerViceType resolve:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject;

@end
