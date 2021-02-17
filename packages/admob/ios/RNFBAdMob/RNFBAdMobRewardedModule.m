//
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

#import <React/RCTUtils.h>

#import "RNFBAdMobRewardedModule.h"
#import "RNFBAdMobFullScreenContentDelegate.h"
#import "RNFBAdMobCommon.h"
#import "RNFBSharedUtils.h"
#import <RNFBApp/RNFBSharedUtils.h>

@implementation RNFBAdMobRewardedModule
#pragma mark -
#pragma mark Module Setup

RCT_EXPORT_MODULE();

- (dispatch_queue_t)methodQueue {
  return dispatch_get_main_queue();
}

- (id)init {
  self = [super init];
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    _rewardedMap = [[NSMutableDictionary alloc] init];
  });
  return self;
}

+ (BOOL)requiresMainQueueSetup {
  return YES;
}

- (void)dealloc {
  [self invalidate];
}

- (void)invalidate {
  for (NSNumber *id in [_rewardedMap allKeys]) {
    RNFBGADRewarded * ad = _rewardedMap[id];
    [ad setRequestId:@-1];
    [_rewardedMap removeObjectForKey:id];
  }
}

#pragma mark -
#pragma mark Firebase AdMob Methods

RCT_EXPORT_METHOD(rewardedLoad
  :
  (nonnull
    NSNumber *)requestId
    :(NSString *)adUnitId
    :(NSDictionary *)adRequestOptions
    :(RCTPromiseResolveBlock) resolve
    :(RCTPromiseRejectBlock) reject
) {

    [GADRewardedAd loadWithAdUnitID:adUnitId
                             request:[RNFBAdMobCommon buildAdRequest:adRequestOptions]
                   completionHandler:^(GADRewardedAd *_Nullable ad, NSError *_Nullable error) {
                     if (error) {
                         [RNFBSharedUtils rejectPromiseWithNSError:reject error:error];
                       return;
                     }

                    NSDictionary *serverSideVerificationOptions = [adRequestOptions objectForKey:@"serverSideVerificationOptions"];

                    if (serverSideVerificationOptions != nil) {
                      GADServerSideVerificationOptions *options = [[GADServerSideVerificationOptions alloc] init];

                      NSString *userId = [serverSideVerificationOptions valueForKey:@"userId"];

                      if (userId != nil) {
                          options.userIdentifier = userId;
                      }

                      NSString *customData = [serverSideVerificationOptions valueForKey:@"customData"];

                      if (customData != nil) {
                        options.customRewardString = customData;
                      }

                      [ad setServerSideVerificationOptions:options];
                    }
        
                    RNFBAdMobFullScreenContentDelegate *delegate = [RNFBAdMobFullScreenContentDelegate initWithParams:self
                                      requestId:requestId
                                      adUnitId:adUnitId];

                    ad.fullScreenContentDelegate = delegate;

                     RNFBAdMobFullScreenContent *RNFBAdMobFullScreenContentAd = [RNFBAdMobFullScreenContent alloc];

                     [RNFBAdMobFullScreenContentAd setRequestId:requestId];
                     [RNFBAdMobFullScreenContentAd setLoadTime:[NSDate date]];
                     [RNFBAdMobFullScreenContentAd setFullScreenPresentingAd:ad];
                     [RNFBAdMobFullScreenContentAd setFullScreenDelegate:delegate];
        
                     if(self->_rewardedMap == nil)
                        self->_rewardedMap = [[NSMutableDictionary alloc] init];
          
                     self->_rewardedMap[requestId] = RNFBAdMobFullScreenContentAd;
          
                    [RNFBAdMobFullScreenContentDelegate sendFullScreenContentEvent:EVENT_REWARDED type:ADMOB_EVENT_LOADED requestId:requestId adUnitId:adUnitId error:nil];
          
                     resolve([NSNull null]);
                   }];
}

RCT_EXPORT_METHOD(rewardedShow
  :
  (nonnull
    NSNumber *)requestId
    :(NSString *)adUnitId
    :(NSDictionary *)showOptions
    :(RCTPromiseResolveBlock) resolve
    :(RCTPromiseRejectBlock) reject
) {
   RNFBAdMobFullScreenContent *RNFBAdMobFullScreenContentAd = _rewardedMap[requestId];
   if (RNFBAdMobFullScreenContentAd && RNFBAdMobFullScreenContentAd.fullScreenPresentingAd) {
     [(GADRewardedAd*)RNFBAdMobFullScreenContentAd.fullScreenPresentingAd presentFromRootViewController:RCTKeyWindow().rootViewController
         userDidEarnRewardHandler:^ {
            GADAdReward *reward = ((GADRewardedAd*)RNFBAdMobFullScreenContentAd.fullScreenPresentingAd).adReward;
            resolve(@{@"amount":reward.amount,@"type":reward.type});
       }];
     resolve([NSNull null]);
   } else {
     [RNFBSharedUtils rejectPromiseWithUserInfo:reject userInfo:[@{
         @"code": @"not-ready",
         @"message": @"Interstitial ad attempted to show but was not ready.",
     } mutableCopy]];
   }
}

@end

