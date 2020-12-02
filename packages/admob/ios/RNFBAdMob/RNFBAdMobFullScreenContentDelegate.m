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


@implementation RNFBAdMobFullScreenContentDelegate

+ (instancetype)sharedInstance {
  static dispatch_once_t once;
  static RNFBAdMobFullScreenContentDelegate *sharedInstance;
  dispatch_once(&once, ^{
    sharedInstance = [[RNFBAdMobFullScreenContentDelegate alloc] init];
  });
  return sharedInstance;
}

#pragma mark -
#pragma mark Helper Methods

+ (void)sendAppOpenEvent:(NSString *)type
                        error:(nullable NSDictionary *)error {
  [RNFBAdMobCommon sendAdEvent:EVENT_APPOPEN requestId:[NSNumber numberWithInt:0]  type:type adUnitId:@"" error:error data:nil];
}


#pragma mark -
#pragma mark GADFullScreenContentDelegate Methods

- (void)ad:(nonnull id<GADFullScreenPresentingAd>)ad
    didFailToPresentFullScreenContentWithError:(nonnull NSError *)error {
    NSDictionary *codeAndMessage = [RNFBAdMobCommon getCodeAndMessageFromAdError:error];
    [RNFBAdMobFullScreenContentDelegate sendAppOpenEvent:ADMOB_EVENT_ERROR error:codeAndMessage];

}

/// Tells the delegate that the ad presented full screen content.
- (void)adDidPresentFullScreenContent:(nonnull id<GADFullScreenPresentingAd>)ad {
    [RNFBAdMobFullScreenContentDelegate sendAppOpenEvent:ADMOB_EVENT_OPENED error:nil];
}

/// Tells the delegate that the ad dismissed full screen content.
- (void)adDidDismissFullScreenContent:(nonnull id<GADFullScreenPresentingAd>)ad {
    [RNFBAdMobFullScreenContentDelegate sendAppOpenEvent:ADMOB_EVENT_CLOSED error:nil];
}


@end
