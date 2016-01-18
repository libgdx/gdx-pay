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

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;
import com.badlogic.gdx.pay.android.googleplay.billing.GoogleInAppBillingService;
import com.badlogic.gdx.pay.android.googleplay.billing.GoogleInAppBillingService.ConnectionListener;
import com.badlogic.gdx.pay.android.googleplay.billing.GoogleInAppBillingService.PurchaseRequestCallback;
import com.badlogic.gdx.pay.android.googleplay.billing.V3GoogleInAppBillingService;
import com.badlogic.gdx.pay.android.googleplay.billing.converter.PurchaseResponseActivityResultConverter;
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
    public static final String GOOGLE_PLAY_PACKAGE_INSTALLER = "com.android.vending";

    private final GoogleInAppBillingService googleInAppBillingService;

    Logger logger = new Logger(LOG_TAG);

    private final Map<String, Information> informationMap = new ConcurrentHashMap<>();
    private PurchaseObserver observer;
    private PurchaseManagerConfig purchaseManagerConfig;


    public AndroidGooglePlayPurchaseManager(GoogleInAppBillingService googleInAppBillingService) {
        this.googleInAppBillingService = googleInAppBillingService;
    }

    @SuppressWarnings("unused") // Unit tested with reflection. (as in IAP.java)
    public AndroidGooglePlayPurchaseManager(Activity activity, int activityRequestCode) {
        if (!(activity instanceof  AndroidApplication)) {
            throw new IllegalArgumentException("Bootstrapping gdx-pay only supported with AndroidApplication activity.");
        }
        AndroidApplication application = (AndroidApplication) activity;
        PurchaseResponseActivityResultConverter converter = new PurchaseResponseActivityResultConverter(this);
        googleInAppBillingService = new V3GoogleInAppBillingService(application, activityRequestCode, converter);
    }

    @Override
    public void install(final PurchaseObserver observer, final PurchaseManagerConfig purchaseManagerConfig, final boolean autoFetchInformation) {
        this.observer = observer;
        this.purchaseManagerConfig = purchaseManagerConfig;

        googleInAppBillingService.requestConnect(new ConnectionListener() {
            @Override
            public void connected() {
                onServiceConnected(observer);
            }

            @Override
            public void disconnected(GdxPayException exception) {
                observer.handleInstallError(new GdxPayException("Failed to bind to service", exception));
            }
        });

    }

    /**
     * Used by IAP.java with reflection for automatic configuration of gdx-pay.
     */
    public static boolean isRunningViaGooglePlay(Activity activity) {
        String packageNameInstaller;
        try {
            packageNameInstaller = activity.getPackageManager().getInstallerPackageName(activity.getPackageName());

            return packageNameInstaller.equals(GOOGLE_PLAY_PACKAGE_INSTALLER);
        } catch (Throwable e) {
            Log.e(LOG_TAG, "Cannot determine installer package name.", e);

            return false;
        }
    }

    protected void runAsync(Runnable runnable) {
        new Thread(runnable).start();
    }

    private void loadSkusAndFillPurchaseInformation()  {
        List<String> productIds = productIdStringList();

        Map<String, Information> skuDetails = googleInAppBillingService.getProductsDetails(productIds);

        informationMap.clear();
        informationMap.putAll(skuDetails);
    }

    private List<String> productIdStringList() {
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
        observer = null;
    }



    @Override
    public void purchase(String identifier) {
        assertInstalled();

        if (!productsLoaded()) {
            loadProductsAndPurchaseAsynchronously(identifier);

            return;
        }

        googleInAppBillingService.startPurchaseRequest(identifier, new PurchaseRequestCallback() {

            @Override
            public void purchaseSuccess(Transaction transaction) {
                if (observer != null) {
                    observer.handlePurchase(transaction);
                }
            }

            @Override
            public void purchaseError(GdxPayException exception) {
                if (observer != null) {
                    observer.handlePurchaseError(exception);
                }

            }

            @Override
            public void purchaseCanceled() {
                if (observer != null) {
                    observer.handlePurchaseCanceled();
                }
            }
        });
    }

    private boolean productsLoaded() {
        return !informationMap.isEmpty();
    }

    private void loadProductsAndPurchaseAsynchronously(final String identifier) {
        runAsync(new Runnable() {
            @Override
            public void run() {
                loadSkusAndFillPurchaseInformation();

                if (productsLoaded()) {
                    purchase(identifier);
                }
            }
        });
    }

    // TODO: call in new thread if called from UI thread (check if this is necessary).
    @Override
    public void purchaseRestore() {

        try {
            List<Transaction> transactions = googleInAppBillingService.getPurchases();

            if (observer != null) {
                observer.handleRestore(transactions.toArray(new Transaction[transactions.size()]));
            }
        } catch(GdxPayException e) {
            if (observer != null) {
                observer.handleRestoreError(e);
            }
        }
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

    private void onServiceConnected(final PurchaseObserver observer) {
        runAsync(new Runnable() {
            @Override
            public void run() {

                try {
                    loadSkusAndFillPurchaseInformation();
                } catch (Exception e) {
                    // TODO: this situation not yet unit-tested.
                    logger.error("Failed to load skus in onServiceConnected()", e);
                }
                observer.handleInstall();
            }
        });
    }

    private void assertInstalled() {
        if (!installed()) {
            throw new GdxPayException("Payment system must be installed to perform this action.");
        }
    }


}
