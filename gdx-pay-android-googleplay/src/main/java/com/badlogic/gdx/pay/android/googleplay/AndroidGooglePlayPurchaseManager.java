/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.pay.android.googleplay;

import android.app.Activity;
import android.util.Log;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.android.googleplay.billing.GoogleInAppBillingService;
import com.badlogic.gdx.pay.android.googleplay.billing.V3GoogleInAppBillingService;
import com.badlogic.gdx.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The purchase manager implementation for Google Play (Android).
 *
 * @author noblemaster
 */
public class AndroidGooglePlayPurchaseManager implements PurchaseManager {

    public static final String PURCHASE_TYPE_IN_APP = "inapp";
    public static final String LOG_TAG = "GdxPay/AndroidPlay";

    private final GoogleInAppBillingService googleInAppBillingService;


    Logger logger = new Logger(LOG_TAG);

    private final Map<String, Information> informationMap = new ConcurrentHashMap<>();


    public AndroidGooglePlayPurchaseManager(GoogleInAppBillingService googleInAppBillingService) {
        this.googleInAppBillingService = googleInAppBillingService;
    }

    // TODO unit test method called by IAP.java
    @SuppressWarnings({"UnusedParameters", "unused"})
    // requestCode is set by IAP.java which auto-configures IAP.
    // not yet using it though (probably needed when doing purchases and restores).
    public AndroidGooglePlayPurchaseManager(Activity activity, int requestCode) {

        googleInAppBillingService = new V3GoogleInAppBillingService(activity);
    }

    @Override
    public void install(final PurchaseObserver observer, final PurchaseManagerConfig config, final boolean autoFetchInformation) {

        googleInAppBillingService.connect(new GoogleInAppBillingService.ConnectResultListener() {
            @Override
            public void connected() {
                onServiceConnected(observer, config);
            }

            @Override
            public void disconnected(Exception exception) {
                observer.handleInstallError(new GdxPayException("Failed to bind to service", exception));
            }
        });
    }

    /**
     * Used by IAP for automatic configuration of gdx-pay.
     */
    public static boolean isRunningViaGooglePlay(Activity activity) {
        // who installed us?
        String packageNameInstaller;
        try {
            // obtain the package name for the installer!
            packageNameInstaller = activity.getPackageManager().getInstallerPackageName(activity.getPackageName());

            // package name matches the string below if we were installed by Google Play!
            return packageNameInstaller.equals("com.android.vending");
        } catch (Throwable e) {
            // error: output to console (we usually shouldn't get here!)
            Log.e(LOG_TAG, "Cannot determine installer package name.", e);

            return false;
        }
    }


    protected void runAsync(Runnable runnable) {
        new Thread(runnable).start();
    }

    protected void loadSkusAndFillPurchaseInformation(PurchaseManagerConfig purchaseManagerConfig) throws android.os.RemoteException {

        List<String> productIds = productIdStringList(purchaseManagerConfig);


        Map<String, Information> skuDetails = googleInAppBillingService.getProductSkuDetails(productIds);

        informationMap.clear();
        informationMap.putAll(skuDetails);
    }

    private List<String> productIdStringList(PurchaseManagerConfig purchaseManagerConfig) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < purchaseManagerConfig.getOfferCount(); i++) {
            Offer offer = purchaseManagerConfig.getOffer(i);

            list.add(offer.getIdentifier());
        }

        return list;
    }

    @Override
    public boolean installed() {
        return googleInAppBillingService.isConnected();
    }

    @Override
    public void dispose() {
        googleInAppBillingService.disconnect();
        clearCaches();
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
        Information information = informationMap.get(identifier);

        if (information == null) {
            return Information.UNAVAILABLE;
        }

        return information;
    }

    @Override
    public String storeName() {
        return PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE;
    }


    private void clearCaches() {
        informationMap.clear();
    }


    private void onServiceConnected(final PurchaseObserver observer, final PurchaseManagerConfig config) {
        runAsync(new Runnable() {
            @Override
            public void run() {

                try {
                    loadSkusAndFillPurchaseInformation(config);
                } catch (Exception e) {
                    // TODO: this situation not yet unit-tested

                    logger.error("Failed to load skus in onServiceConnected()", e);
                }
                observer.handleInstall();
            }
        });
    }

}
