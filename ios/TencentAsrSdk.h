
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNTencentAsrSdkSpec.h"

@interface TencentAsrSdk : NSObject <NativeTencentAsrSdkSpec>
#else
#import <React/RCTBridgeModule.h>

@interface TencentAsrSdk : NSObject <RCTBridgeModule>
#endif

@end
