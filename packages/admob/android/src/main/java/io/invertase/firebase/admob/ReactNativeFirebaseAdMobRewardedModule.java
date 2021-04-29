package io.invertase.firebase.admob;

import android.app.Activity;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions;

import io.invertase.firebase.common.ReactNativeFirebaseModule;
import io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent;

import static io.invertase.firebase.admob.ReactNativeFirebaseAdMobCommon.buildAdRequest;
import static io.invertase.firebase.admob.ReactNativeFirebaseAdMobCommon.getCodeAndMessageFromAdErrorCode;
import static io.invertase.firebase.admob.ReactNativeFirebaseAdMobCommon.sendAdEvent;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_CLOSED;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_ERROR;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_OPENED;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_REWARDED_EARNED_REWARD;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_REWARDED_LOADED;

public class ReactNativeFirebaseAdMobRewardedModule extends ReactNativeFirebaseModule {
  private static final String SERVICE = "AdMobRewarded";
  private static SparseArray<RewardedAd> rewardedAdArray = new SparseArray<>();

  private static Activity sCurrentActivity;

  @javax.annotation.Nullable
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

  public ReactNativeFirebaseAdMobRewardedModule(ReactApplicationContext reactContext) {
    super(reactContext, SERVICE);
  }

  private void sendRewardedEvent(String type, int requestId, String adUnitId, @Nullable WritableMap error, @Nullable WritableMap data) {
    sendAdEvent(
      ReactNativeFirebaseAdMobEvent.EVENT_REWARDED,
      requestId,
      type,
      adUnitId,
      error,
      data
    );
  }

  @ReactMethod
  public void rewardedLoad(int requestId, String adUnitId, ReadableMap adRequestOptions) {
    Activity currentActivity = maybeGetCurrentActivity();
    if (currentActivity == null) {
      WritableMap error = Arguments.createMap();
      error.putString("code", "null-activity");
      error.putString("message", "Rewarded ad attempted to load but the current Activity was null.");
      sendRewardedEvent(AD_ERROR, requestId, adUnitId, error, null);
      return;
    }
    currentActivity.runOnUiThread(() -> {

      AdRequest adRequest = new AdRequest.Builder().build();

      Activity targetActivity = currentActivity;

      if(currentActivity == null && getReactApplicationContext() != null && getReactApplicationContext().getCurrentActivity() != null)
        targetActivity = getReactApplicationContext().getCurrentActivity();

      RewardedAd.load(targetActivity != null ? targetActivity : getReactApplicationContext(), adUnitId,
        adRequest, new RewardedAdLoadCallback(){
          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            WritableMap errorMap = Arguments.createMap();
            String[] codeAndMessage = getCodeAndMessageFromAdErrorCode(loadAdError.getCode());
            errorMap.putString("code", codeAndMessage[0]);
            errorMap.putString("message", codeAndMessage[1]);
            sendRewardedEvent(AD_ERROR, requestId, adUnitId, errorMap, null);
          }

          @Override
          public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
            RewardItem rewardItem = rewardedAd.getRewardItem();
            WritableMap data = Arguments.createMap();
            data.putString("type", rewardItem.getType());
            data.putInt("amount", rewardItem.getAmount());
            sendRewardedEvent(AD_REWARDED_LOADED, requestId, adUnitId, null, data);

            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback(){
              @Override
              public void onAdShowedFullScreenContent() {
                // Called when ad is shown.
                rewardedAdArray.remove(requestId);
                sendRewardedEvent(AD_OPENED, requestId, adUnitId, null, null);
              }

              @Override
              public void onAdFailedToShowFullScreenContent(AdError adError) {
                WritableMap errorMap = Arguments.createMap();
                String[] codeAndMessage = getCodeAndMessageFromAdErrorCode(adError.getCode());
                errorMap.putString("code", codeAndMessage[0]);
                errorMap.putString("message", codeAndMessage[1]);
                sendRewardedEvent(AD_ERROR, requestId, adUnitId, errorMap, null);
              }

              @Override
              public void onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Don't forget to set the ad reference to null so you
                // don't show the ad a second time.
                sendRewardedEvent(AD_CLOSED, requestId, adUnitId, null, null);
              }
            });

            rewardedAdArray.put(requestId, rewardedAd);
          }
        });
    });
  }

  @ReactMethod
  public void rewardedShow(int requestId, String adUnitId, ReadableMap showOptions, Promise promise) {
    Activity currentActivity = maybeGetCurrentActivity();
    if (currentActivity == null) {
      rejectPromiseWithCodeAndMessage(promise, "null-activity", "Rewarded ad attempted to show but the current Activity was null.");
      return;
    }
    currentActivity.runOnUiThread(() -> {
      RewardedAd rewardedAd = rewardedAdArray.get(requestId);

      if (rewardedAd != null) {

        boolean immersiveModeEnabled = false;
        if (showOptions.hasKey("immersiveModeEnabled")) {
          immersiveModeEnabled = showOptions.getBoolean("immersiveModeEnabled");
        }

        rewardedAd.setImmersiveMode(immersiveModeEnabled);

        Activity targetActivity = currentActivity;

        if(currentActivity == null && getReactApplicationContext() != null && getReactApplicationContext().getCurrentActivity() != null)
          targetActivity = getReactApplicationContext().getCurrentActivity();

        rewardedAd.show(targetActivity, new OnUserEarnedRewardListener() {
          @Override
          public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
            WritableMap data = Arguments.createMap();
            data.putString("type", rewardItem.getType());
            data.putInt("amount", rewardItem.getAmount());
            sendRewardedEvent(AD_REWARDED_EARNED_REWARD, requestId, adUnitId, null, data);
          }
        });

        promise.resolve(null);

      } else {
        rejectPromiseWithCodeAndMessage(promise, "not-ready", "Rewarded ad attempted to show but was not ready.");
      }
    });
  }
}
