
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

import { isFunction, isString } from '@react-native-firebase/app/lib/common';
import { getFirebaseRoot } from '@react-native-firebase/app/lib/internal';
import validateAdRequestOptions from '../validateAdRequestOptions';
import validateAdShowOptions from '../validateAdShowOptions';
import MobileAd from './MobileAd';

let _appOpenRequest = 0;

export default class AppOpenAd extends MobileAd {
  static createForAdRequest(adUnitId, requestOptions) {
    if (!isString(adUnitId)) {
      throw new Error(
        "firebase.admob() AppOpenAd.createForAdRequest(*) 'adUnitId' expected an string value.",
      );
    }

    let options = {};
    try {
      options = validateAdRequestOptions(requestOptions);
    } catch (e) {
      throw new Error(`firebase.admob() AppOpenAd.createForAdRequest(_, *) ${e.message}.`);
    }

    const requestId = _appOpenRequest++;
    const admob = getFirebaseRoot().admob();
    return new AppOpenAd('appopen', admob, requestId, adUnitId, options);
  }

  load() {
    // Prevent multiple load calls
    if (this._loaded) {
      return;
    }

    this._loaded = true;
    this._admob.native.appOpenLoad(this._requestId, this._adUnitId, this._requestOptions);
  }

  onAdEvent(handler) {
    if (!isFunction(handler)) {
      throw new Error(
        "firebase.admob() AppOpenAd.onAdEvent(*) 'handler' expected a function.",
      );
    }

    return this._setAdEventHandler(handler);
  }

  show(showOptions) {
    if (!this._loaded) {
      throw new Error(
        'firebase.admob() AppOpenAd.show() The requested AppOpenAd has not loaded and could not be shown.',
      );
    }

    let options;
    try {
      options = validateAdShowOptions(showOptions);
    } catch (e) {
      throw new Error(`firebase.admob() InterstitialAd.show(*) ${e.message}.`);
    }

    return this._admob.native.appOpenShow(this._requestId, options);
  }
}
