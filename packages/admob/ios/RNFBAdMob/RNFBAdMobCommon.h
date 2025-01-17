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

#import <React/RCTBridgeModule.h>
#import <Firebase/Firebase.h>
@import GoogleMobileAds;

@interface RNFBAdMobCommon : NSObject

+ (GADRequest *)buildAdRequest:(NSDictionary *)adRequestOptions;

+ (NSDictionary *)getCodeAndMessageFromAdError:(NSError *)error;

+ (void)sendAdEvent:(NSString *)adType
          type:(NSString *)type
          requestId:(NSNumber *)requestId
          adUnitId:(NSString *)adUnitId
          error:(nullable NSDictionary *)error
               data:(nullable NSDictionary *)data;

+ (GADAdSize)stringToAdSize:(NSString *)value;

@end

@interface RNFBGADInterstitial : GADInterstitialAd
@property(nonatomic) NSNumber *requestId;
- (void)setRequestId:(NSNumber *)requestId;
@end

@interface RNFBGADRewarded : GADRewardedAd
@property(nonatomic) NSNumber *requestId;
- (void)setRequestId:(NSNumber *)requestId;
@end

@interface RNFBAdMobFullScreenContent : NSObject
@property(nonatomic) NSNumber *requestId;
- (void)setRequestId:(NSNumber *)requestId;
@property(nonatomic) NSDate *loadTime;
- (void)setLoadTime:(NSDate *)loadTime;
@property(nonatomic) id<GADFullScreenPresentingAd> fullScreenPresentingAd;
- (void)setFullScreenPresentingAd:(__strong id<GADFullScreenPresentingAd>)fullScreenPresentingAd;
@property(nonatomic) id<GADFullScreenContentDelegate> fullScreenDelegate;
- (void)setFullScreenDelegate:(__strong id<GADFullScreenContentDelegate>) fullScreenDelegate;
@end

extern NSString *const EVENT_INTERSTITIAL;
extern NSString *const EVENT_REWARDED;
extern NSString *const EVENT_APPOPEN;

extern NSString *const ADMOB_EVENT_LOADED;
extern NSString *const ADMOB_EVENT_ERROR;
extern NSString *const ADMOB_EVENT_OPENED;
extern NSString *const ADMOB_EVENT_CLICKED;
extern NSString *const ADMOB_EVENT_LEFT_APPLICATION;
extern NSString *const ADMOB_EVENT_CLOSED;

extern NSString *const ADMOB_EVENT_REWARDED_LOADED;
extern NSString *const ADMOB_EVENT_REWARDED_EARNED_REWARD;
