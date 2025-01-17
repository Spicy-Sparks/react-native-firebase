package io.invertase.firebase.admob;

/*
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

import android.app.Activity;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import javax.annotation.Nullable;

import io.invertase.firebase.common.ReactNativeFirebaseModule;
import io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent;

import static io.invertase.firebase.admob.ReactNativeFirebaseAdMobCommon.getCodeAndMessageFromAdErrorCode;
import static io.invertase.firebase.admob.ReactNativeFirebaseAdMobCommon.sendAdEvent;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_CLOSED;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_ERROR;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_LOADED;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_OPENED;

public class ReactNativeFirebaseAdMobInterstitialModule extends ReactNativeFirebaseModule {
  private static final String SERVICE = "AdMobInterstitial";
  private static SparseArray<InterstitialAd> interstitialAdArray = new SparseArray<>();

  private static Activity sCurrentActivity;

  @Nullable
  private Activity maybeGetCurrentActivity()
  {
    // React Native has a bug where `getCurrentActivity()` returns null: https://github.com/facebook/react-native/issues/18345
    // To alleviate the issue - we will store as a static reference (WeakReference unfortunately did not suffice)
    if ( getReactApplicationContext().hasCurrentActivity() )
    {
      sCurrentActivity = getReactApplicationContext().getCurrentActivity();
    }

    return sCurrentActivity;
  }

  public ReactNativeFirebaseAdMobInterstitialModule(ReactApplicationContext reactContext) {
    super(reactContext, SERVICE);
  }

  private void sendInterstitialEvent(String type, int requestId, String adUnitId, @Nullable WritableMap error) {
    sendAdEvent(
      ReactNativeFirebaseAdMobEvent.EVENT_INTERSTITIAL,
      requestId,
      type,
      adUnitId,
      error
    );
  }

  @ReactMethod
  public void interstitialLoad(int requestId, String adUnitId, ReadableMap adRequestOptions) {
    Activity currentActivity = maybeGetCurrentActivity();
    if (currentActivity == null) {
      WritableMap error = Arguments.createMap();
      error.putString("code", "null-activity");
      error.putString("message", "Interstitial ad attempted to load but the current Activity was null.");
      sendInterstitialEvent(AD_ERROR, requestId, adUnitId, error);
      return;
    }
    currentActivity.runOnUiThread(() -> {

      AdRequest adRequest = new AdRequest.Builder().build();

      Activity targetActivity = currentActivity;

      if(currentActivity == null && getReactApplicationContext() != null && getReactApplicationContext().getCurrentActivity() != null)
        targetActivity = getReactApplicationContext().getCurrentActivity();

      InterstitialAd.load(targetActivity != null ? targetActivity : getReactApplicationContext(), adUnitId, adRequest, new InterstitialAdLoadCallback() {
        @Override
        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
          // The mInterstitialAd reference will be null until
          // an ad is loaded.
          sendInterstitialEvent(AD_LOADED, requestId, adUnitId, null);

          interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
            @Override
            public void onAdDismissedFullScreenContent() {
              // Called when fullscreen content is dismissed.
              sendInterstitialEvent(AD_CLOSED, requestId, adUnitId, null);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
              // Called when fullscreen content failed to show.
              WritableMap errorMap = Arguments.createMap();
              String[] codeAndMessage = getCodeAndMessageFromAdErrorCode(adError.getCode());
              errorMap.putString("code", codeAndMessage[0]);
              errorMap.putString("message", codeAndMessage[1]);
              sendInterstitialEvent(AD_ERROR, requestId, adUnitId, errorMap);
            }

            @Override
            public void onAdShowedFullScreenContent() {
              // Called when fullscreen content is shown.
              // Make sure to set your reference to null so you don't
              // show it a second time.
              interstitialAdArray.remove(requestId);
              sendInterstitialEvent(AD_OPENED, requestId, adUnitId, null);

            }
          });

          interstitialAdArray.put(requestId, interstitialAd);
        }

        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
          WritableMap errorMap = Arguments.createMap();
          ResponseInfo info = loadAdError.getResponseInfo();

          String[] codeAndMessage = getCodeAndMessageFromAdErrorCode(loadAdError.getCode());
          errorMap.putString("code", codeAndMessage[0]);
          errorMap.putString("message", codeAndMessage[1]);
          sendInterstitialEvent(AD_ERROR, requestId, adUnitId, errorMap);
        }
      });
    });
  }

  @ReactMethod
  public void interstitialShow(int requestId, ReadableMap showOptions, Promise promise) {
    Activity currentActivity = maybeGetCurrentActivity();
    if (currentActivity == null) {
      rejectPromiseWithCodeAndMessage(promise, "null-activity", "Interstitial ad attempted to show but the current Activity was null.");
      return;
    }
    currentActivity.runOnUiThread(() -> {
      InterstitialAd interstitialAd = interstitialAdArray.get(requestId);

      if (interstitialAd != null) {

        boolean immersiveModeEnabled = false;
        if (showOptions.hasKey("immersiveModeEnabled")) {
          immersiveModeEnabled = showOptions.getBoolean("immersiveModeEnabled");
        }

        interstitialAd.setImmersiveMode(immersiveModeEnabled);

        Activity targetActivity = currentActivity;

        if(currentActivity == null && getReactApplicationContext() != null && getReactApplicationContext().getCurrentActivity() != null)
          targetActivity = getReactApplicationContext().getCurrentActivity();

        interstitialAd.show(targetActivity);

        promise.resolve(null);
      } else {
        rejectPromiseWithCodeAndMessage(promise, "not-ready", "Interstitial ad attempted to show but was not ready.");
      }
    });
  }
}
