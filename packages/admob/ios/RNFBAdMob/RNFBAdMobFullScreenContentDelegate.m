/**
 * Copyright (c) 2016-present Invertase Limited & Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this library except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#import "RNFBAdMobFullScreenContentDelegate.h"
#import "RNFBAdMobFullScreenContentModule.h"
#import "RNFBAdMobInterstitialModule.h"
#import "RNFBAdMobRewardedModule.h"


@implementation RNFBAdMobFullScreenContentDelegate

- (void)dealloc {
  
}

+ (id)initWithParams:(NSObject *)module
            requestId:(NSNumber *)requestId
            adUnitId:(NSString *)adUnitId {
    RNFBAdMobFullScreenContentDelegate *instance = [[RNFBAdMobFullScreenContentDelegate alloc] init];
    if (instance) {
        instance.module = module;
        instance.requestId = requestId;
        instance.adUnitId = adUnitId;
    }
    
    return instance;
}

#pragma mark -
#pragma mark Helper Methods

+ (void)sendFullScreenContentEvent:(NSString *)adType
                              type:(NSString *)type
                              requestId:(NSNumber *)requestId
                              adUnitId:(NSString *)adUnitId
                        error:(nullable NSDictionary *)error {
    [RNFBAdMobCommon sendAdEvent:adType type:type requestId:requestId adUnitId:adUnitId error:error data:nil];
}

- (NSString*)adTypeFromAd:(id) ad
{
    if([ad isKindOfClass:[GADInterstitialAd class]])
        return EVENT_INTERSTITIAL;
    if([ad isKindOfClass:[GADRewardedAd class]])
        return EVENT_REWARDED;
    
    return EVENT_APPOPEN;
}


#pragma mark -
#pragma mark GADFullScreenContentDelegate Methods

- (void)ad:(nonnull id<GADFullScreenPresentingAd>)ad didFailToPresentFullScreenContentWithError:(NSError *)error{
    NSDictionary *codeAndMessage = [RNFBAdMobCommon getCodeAndMessageFromAdError:error];
    [RNFBAdMobFullScreenContentDelegate sendFullScreenContentEvent:[self adTypeFromAd:ad] type:ADMOB_EVENT_ERROR requestId:_requestId adUnitId:_adUnitId error:codeAndMessage];
}

/// Tells the delegate that the ad presented full screen content.
- (void)adDidPresentFullScreenContent:(nonnull id<GADFullScreenPresentingAd>)ad {
    [RNFBAdMobFullScreenContentDelegate sendFullScreenContentEvent:[self adTypeFromAd:ad] type:ADMOB_EVENT_OPENED requestId:_requestId adUnitId:_adUnitId error:nil];
}

/// Tells the delegate that the ad dismissed full screen content.
- (void)adDidDismissFullScreenContent:(nonnull id<GADFullScreenPresentingAd>)ad {
    
    NSString *adType = [self adTypeFromAd:ad];
    
    [RNFBAdMobFullScreenContentDelegate sendFullScreenContentEvent:adType type:ADMOB_EVENT_CLOSED requestId:_requestId adUnitId:_adUnitId error:nil];
    
    if(adType == EVENT_INTERSTITIAL){
        RNFBAdMobInterstitialModule *m = (RNFBAdMobInterstitialModule*)_module;
        [m.interstitialMap removeObjectForKey:_requestId];
    }
    
    if(adType == EVENT_REWARDED){
        RNFBAdMobRewardedModule *m = (RNFBAdMobRewardedModule*)_module;
        [m.rewardedMap removeObjectForKey:_requestId];
    }
    
    if(adType == EVENT_APPOPEN){
        RNFBAdMobFullScreenContentModule *m = (RNFBAdMobFullScreenContentModule*)_module;
        [m.appOpenMap removeObjectForKey:_requestId];
    }
}


@end
