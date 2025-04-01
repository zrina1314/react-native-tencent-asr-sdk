//
//  AsrRealTimeManager.h
//  TencentCloudAsrSdk
//
//  Created by Eggsy on 2024/2/22.
//
#import <Foundation/Foundation.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTBridgeModule.h>

@interface AsrRealTimeManager : RCTEventEmitter<RCTBridgeModule>

-(instancetype)initWithAppid:(int)appid projectId:(int)projectId secretId:(NSString*)secretId secretKey:(NSString*)secretKey saveFilePath:(NSString*)saveFilePath;

-(void)startRecorder;
-(void)stopRecorder;
-(void)cancel;

+(void)checkPermissons;

@end
