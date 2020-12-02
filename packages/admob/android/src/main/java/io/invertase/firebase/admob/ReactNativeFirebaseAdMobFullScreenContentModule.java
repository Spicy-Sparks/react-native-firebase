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
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.internal.ads.zzsm;
import com.google.android.gms.internal.ads.zzxg;

import java.util.Calendar;
import java.util.Date;

import javax.annotation.Nullable;

import io.invertase.firebase.common.ReactNativeFirebaseModule;
import io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent;

import static io.invertase.firebase.admob.ReactNativeFirebaseAdMobCommon.buildAdRequest;
import static io.invertase.firebase.admob.ReactNativeFirebaseAdMobCommon.getCodeAndMessageFromAdErrorCode;
import static io.invertase.firebase.admob.ReactNativeFirebaseAdMobCommon.sendAdEvent;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_CLICKED;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_CLOSED;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_ERROR;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_LEFT_APPLICATION;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_LOADED;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_OPENED;

public class ReactNativeFirebaseAdMobFullScreenContentModule extends ReactNativeFirebaseModule {
  private static final String SERVICE = "AdMobFullScreenContent";
  private static SparseArray<RNFBGADAppOpenAd> appOpenAdArray = new SparseArray<>();
  private AppOpenAd.AppOpenAdLoadCallback loadCallback;

  private class RNFBGADAppOpenAd {

    public AppOpenAd _appOpenAd;
    public int _requestId;
    public String _adUnitId;
    public long _loadTime;

    public RNFBGADAppOpenAd(AppOpenAd appOpenAd, int requestId, String adUnitId, long loadTime)
    {
      this._appOpenAd = appOpenAd;
      this._requestId = requestId;
      this._adUnitId = adUnitId;
      this._loadTime = loadTime;
    }
  }

  public ReactNativeFirebaseAdMobFullScreenContentModule(ReactApplicationContext reactContext) {
    super(reactContext, SERVICE);
  }

  private void sendAppOpenEvent(String type, int requestId, String adUnitId, @Nullable WritableMap error) {
    sendAdEvent(
      ReactNativeFirebaseAdMobEvent.EVENT_APPOPEN,
      requestId,
      type,
      adUnitId,
      error
    );
  }

  private boolean wasLoadTimeLessThanNHoursAgo(long loadTime, int hours){
    Calendar now = Calendar.getInstance();
    long timeAgo = now.getTimeInMillis() - loadTime;

    return !(timeAgo > hours*60*60*1000);
  }

  @ReactMethod
  public void appOpenLoad(int requestId, String adUnitId, ReadableMap adRequestOptions) {
    if (getCurrentActivity() == null) {
      WritableMap error = Arguments.createMap();
      error.putString("code", "null-activity");
      error.putString("message", "Interstitial ad attempted to load but the current Activity was null.");
      sendAppOpenEvent(AD_ERROR, requestId, adUnitId, error);
      return;
    }

    loadCallback =
      new AppOpenAd.AppOpenAdLoadCallback() {
        /**
         * Called when an app open ad has loaded.
         *
         * @param ad the loaded app open ad.
         */
        @Override
        public void onAppOpenAdLoaded(AppOpenAd ad) {
          RNFBGADAppOpenAd RNFBGADAppOpenAd = new RNFBGADAppOpenAd(ad, requestId, adUnitId, (new Date()).getTime());
          appOpenAdArray.put(requestId, RNFBGADAppOpenAd);
          sendAppOpenEvent(AD_LOADED, requestId, adUnitId, null);
        }

        /**
         * Called when an app open ad has failed to load.
         *
         * @param loadAdError the error.
         */
        @Override
        public void onAppOpenAdFailedToLoad(LoadAdError loadAdError) {

          WritableMap error = Arguments.createMap();
          String[] codeAndMessage = getCodeAndMessageFromAdErrorCode(loadAdError.getCode());
          error.putString("code", codeAndMessage[0]);
          error.putString("message", codeAndMessage[1]);
          sendAppOpenEvent(AD_ERROR, requestId, adUnitId, error);
        }

      };

    getCurrentActivity().runOnUiThread(() -> {
      AppOpenAd.load(getApplicationContext(), adUnitId, buildAdRequest(adRequestOptions), AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback);
    });
  }

  @ReactMethod
  public void appOpenShow(int requestId, ReadableMap showOptions, Promise promise) {
    if (getCurrentActivity() == null) {
      rejectPromiseWithCodeAndMessage(promise, "null-activity", "Interstitial ad attempted to show but the current Activity was null.");
      return;
    }
    getCurrentActivity().runOnUiThread(() -> {
      RNFBGADAppOpenAd RNFBGADAppOpenAd = appOpenAdArray.get(requestId);

      FullScreenContentCallback fullScreenContentCallback =
        new FullScreenContentCallback() {
          @Override
          public void onAdDismissedFullScreenContent() {
            sendAppOpenEvent(AD_CLOSED, requestId, RNFBGADAppOpenAd._adUnitId, null);
          }

          @Override
          public void onAdFailedToShowFullScreenContent(AdError adError) {
            WritableMap error = Arguments.createMap();
            String[] codeAndMessage = getCodeAndMessageFromAdErrorCode(adError.getCode());
            error.putString("code", codeAndMessage[0]);
            error.putString("message", codeAndMessage[1]);
            sendAppOpenEvent(AD_ERROR, requestId, RNFBGADAppOpenAd._adUnitId, error);
          }

          @Override
          public void onAdShowedFullScreenContent() {
            sendAppOpenEvent(AD_OPENED, requestId, RNFBGADAppOpenAd._adUnitId, null);
          }
        };

      if (RNFBGADAppOpenAd._appOpenAd != null && wasLoadTimeLessThanNHoursAgo(RNFBGADAppOpenAd._loadTime, 4)) {
        RNFBGADAppOpenAd._appOpenAd.show(getCurrentActivity(), fullScreenContentCallback);
        promise.resolve(null);
      } else {
        rejectPromiseWithCodeAndMessage(promise, "not-ready", "AppOpen ad attempted to show but was not ready.");
      }
    });
  }
}
