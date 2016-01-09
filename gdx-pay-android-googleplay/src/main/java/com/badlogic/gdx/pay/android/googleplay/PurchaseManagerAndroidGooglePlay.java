/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.pay.android.googleplay;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.OfferType;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The purchase manager implementation for Google Play (Android).
 * <p>
 * Include the gdx-pay-android-googleplay.jar for this to work (plus gdx-pay-android.jar). Also update the "uses-permission" settings
 * in AndroidManifest.xml and your proguard settings.
 * 
 * @author noblemaster */
public class PurchaseManagerAndroidGooglePlay implements PurchaseManager {

    /** Debug tag for logging. */
    private static final String TAG = "GdxPay/" + PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE;

    /** Our Android activity. */
    private Activity activity;
    /** The request code to use for onActivityResult (arbitrary chosen). */
    private int requestCode;


    public PurchaseManagerAndroidGooglePlay (Activity activity, int requestCode) {
        this.activity = activity;

        // the request code for onActivityResult
        this.requestCode = requestCode;
    }

    public static final boolean isRunningViaGooglePlay(Activity activity) {
        // who installed us?
        String packageNameInstaller;
        try {
            // obtain the package name for the installer!
            packageNameInstaller = activity.getPackageManager().getInstallerPackageName(activity.getPackageName());

            // package name matches the string below if we were installed by Google Play!
            return packageNameInstaller.equals("com.android.vending");
        }
        catch (Throwable e) {
            // error: output to console (we usually shouldn't get here!)
            Log.e(TAG, "Cannot determine installer package name.", e);
            e.printStackTrace();

            // reject...
            return false;
        }
    }

    @Override
    public void install(PurchaseObserver observer, PurchaseManagerConfig config, boolean autoFetchInformation) {
        // FIXME
    }

    @Override
    public boolean installed() {
        // FIXME
        return false;
    }

    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        // FIXME
    }

    @Override
    public void dispose() {
        // FIXME
    }

    @Override
    public void purchase(String identifier) {
        // FIXME
    }

    @Override
    public void purchaseRestore() {
        // FIXME
    }

    @Override
    public Information getInformation(String identifier) {
        // FIXME
        return null;
    }

    @Override
    public String storeName() {
        return PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE;
    }

    @Override
    public String toString() {
        return storeName();
    }
}
