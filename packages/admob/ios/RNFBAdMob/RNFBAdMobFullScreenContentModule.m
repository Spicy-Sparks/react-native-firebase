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

#import "RNFBAdMobFullScreenContentModule.h"
#import "RNFBAdMobCommon.h"
#import "RNFBSharedUtils.h"
#import "RNFBAdMobFullScreenContentDelegate.h"

static __strong NSMutableDictionary *appOpenMap;

@implementation RNFBAdMobFullScreenContentModule
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
    appOpenMap = [[NSMutableDictionary alloc] init];
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
  for (NSNumber *id in [appOpenMap allKeys]) {
    [appOpenMap removeObjectForKey:id];
  }
}

- (BOOL)wasLoadTimeLessThanNHoursAgo:(nonnull
                                      NSDate *)loadTime
                                    : (int)n
  {
  NSDate *now = [NSDate date];
  NSTimeInterval timeIntervalBetweenNowAndLoadTime = [now timeIntervalSinceDate:loadTime];
  double secondsPerHour = 3600.0;
  double intervalInHours = timeIntervalBetweenNowAndLoadTime / secondsPerHour;
  return intervalInHours < n;
}

#pragma mark -
#pragma mark Firebase AdMob Methods

RCT_EXPORT_METHOD(appOpenLoad
  :
  (nonnull
    NSNumber *)requestId
    :(NSString *)adUnitId
    :(NSDictionary *)adRequestOptions
    :(RCTPromiseResolveBlock) resolve
    :(RCTPromiseRejectBlock) reject
) {
      [GADAppOpenAd loadWithAdUnitID:adUnitId
                             request:[RNFBAdMobCommon buildAdRequest:adRequestOptions]
                         orientation:UIInterfaceOrientationPortrait
                   completionHandler:^(GADAppOpenAd *_Nullable ad, NSError *_Nullable error) {
                     if (error) {
                         [RNFBSharedUtils rejectPromiseWithUserInfo:reject userInfo:[@{
                             @"code": @"not-loaded",
                             @"message": @"Failed to load app open ad",
                         } mutableCopy]];
                       return;
                     }
          
                     ad.fullScreenContentDelegate = [RNFBAdMobFullScreenContentDelegate initWithParams:requestId adUnitId:adUnitId];
          
                     RNFBAdMobFullScreenContent *RNFBAdMobFullScreenContentAd = [RNFBAdMobFullScreenContent alloc];
  
                     [RNFBAdMobFullScreenContentAd setRequestId:requestId];
                     [RNFBAdMobFullScreenContentAd setLoadTime:[NSDate date]];
                     [RNFBAdMobFullScreenContentAd setFullScreenPresentingAd:ad];
          
                     appOpenMap[requestId] = RNFBAdMobFullScreenContentAd;
          
                    [RNFBAdMobFullScreenContentDelegate sendFullScreenContentEvent:EVENT_APPOPEN type:ADMOB_EVENT_LOADED requestId:requestId adUnitId:adUnitId error:nil];
          
                     resolve([NSNull null]);
                   }];
}

RCT_EXPORT_METHOD(appOpenShow
  :
  (nonnull
    NSNumber *)requestId
    :(NSDictionary *)showOptions
    :(RCTPromiseResolveBlock) resolve
    :(RCTPromiseRejectBlock) reject
) {
    RNFBAdMobFullScreenContent *RNFBAdMobFullScreenContentAd = appOpenMap[requestId];
    if (RNFBAdMobFullScreenContentAd && RNFBAdMobFullScreenContentAd.fullScreenPresentingAd && [self wasLoadTimeLessThanNHoursAgo:RNFBAdMobFullScreenContentAd.loadTime:4]) {
    [(GADAppOpenAd*)RNFBAdMobFullScreenContentAd.fullScreenPresentingAd presentFromRootViewController:RCTKeyWindow().rootViewController];
    resolve([NSNull null]);
  } else {
    [RNFBSharedUtils rejectPromiseWithUserInfo:reject userInfo:[@{
        @"code": @"not-ready",
        @"message": @"AppOpen ad attempted to show but was not ready.",
    } mutableCopy]];
  }
}



@end
