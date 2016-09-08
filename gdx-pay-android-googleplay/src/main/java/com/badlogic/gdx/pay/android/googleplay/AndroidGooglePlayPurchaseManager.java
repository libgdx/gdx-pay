/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.pay.android.googleplay;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.OfferType;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseManagerTestSupport;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.PurchaseSystem;
import com.badlogic.gdx.pay.Transaction;
import com.badlogic.gdx.pay.android.googleplay.billing.ApplicationProxy;
import com.badlogic.gdx.pay.android.googleplay.billing.AsyncExecutor;
import com.badlogic.gdx.pay.android.googleplay.billing.GoogleInAppBillingService;
import com.badlogic.gdx.pay.android.googleplay.billing.GoogleInAppBillingService.ConnectionListener;
import com.badlogic.gdx.pay.android.googleplay.billing.GoogleInAppBillingService.PurchaseRequestCallback;
import com.badlogic.gdx.pay.android.googleplay.billing.NewThreadSleepAsyncExecutor;
import com.badlogic.gdx.pay.android.googleplay.billing.V3GoogleInAppBillingService;
import com.badlogic.gdx.pay.android.googleplay.billing.converter.PurchaseResponseActivityResultConverter;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The purchase manager implementation for Google Play (Android).
 *
 * @author noblemaster
 */
public class AndroidGooglePlayPurchaseManager implements PurchaseManager, PurchaseManagerTestSupport {

    public static final String LOG_TAG = "GdxPay/AndroidPlay";

    public static final String GOOGLE_MARKET_NAME = "com.google.market";
    public static final String GOOGLE_PLAY_STORE_NAME = "com.android.vending";

    private final GoogleInAppBillingService googleInAppBillingService;

    private final Map<String, Information> informationMap = new ConcurrentHashMap<>();
    private PurchaseObserver observer;
    private PurchaseManagerConfig purchaseManagerConfig;


    public AndroidGooglePlayPurchaseManager(GoogleInAppBillingService googleInAppBillingService) {
        this.googleInAppBillingService = googleInAppBillingService;
    }

    @SuppressWarnings("unused") // Unit tested with reflection. (as in IAP.java)
    public AndroidGooglePlayPurchaseManager(Activity activity, int activityRequestCode) {
        if (!(activity instanceof AndroidApplication)) {
            throw new IllegalArgumentException("Bootstrapping gdx-pay only supported with AndroidApplication activity.");
        }
        AndroidApplication application = (AndroidApplication) activity;
        PurchaseResponseActivityResultConverter converter = new PurchaseResponseActivityResultConverter(this);
        AsyncExecutor executor = new NewThreadSleepAsyncExecutor();
        googleInAppBillingService = new V3GoogleInAppBillingService(application, activityRequestCode, converter, executor);
    }

    @SuppressWarnings("unused") // Unit tested with reflection. (as in IAP.java)
    public AndroidGooglePlayPurchaseManager(Activity activity,
                                            AndroidFragmentApplication application,
                                            int activityRequestCode) {

        PurchaseResponseActivityResultConverter converter = new PurchaseResponseActivityResultConverter(this);
        AsyncExecutor executor = new NewThreadSleepAsyncExecutor();
        ApplicationProxy.FragmentProxy proxy = new ApplicationProxy.FragmentProxy(activity, application);
        googleInAppBillingService = new V3GoogleInAppBillingService(proxy, activityRequestCode, converter, executor);

        PurchaseSystem.setManager(this);
    }

    @Override
    public void install(final PurchaseObserver observer, final PurchaseManagerConfig purchaseManagerConfig, final boolean autoFetchInformation) {
        assertConfigSupported(purchaseManagerConfig);
        this.observer = observer;
        this.purchaseManagerConfig = purchaseManagerConfig;

        if (googleInAppBillingService.isListeningForConnections()) {
            // Supports calling me multiple times.
            // TODO: scenario not unit tested, test this!
            googleInAppBillingService.disconnect();
        }

        googleInAppBillingService.requestConnect(new ConnectionListener() {
            @Override
            public void connected() {
                onServiceConnected(observer, autoFetchInformation);
            }

            @Override
            public void disconnected(GdxPayException exception) {
                observer.handleInstallError(new GdxPayException("Failed to bind to service", exception));
            }
        });

    }

    private void assertConfigSupported(PurchaseManagerConfig purchaseManagerConfig) {
        for (int i = 0; i < purchaseManagerConfig.getOfferCount(); i++) {
            Offer offer = purchaseManagerConfig.getOffer(i);
            if (offer.getType() == OfferType.SUBSCRIPTION) {
                throw new IllegalArgumentException("Unsupported offer: " + offer);
            }
        }
    }

    /**
     * Detect if running on Phone which has Google Play installed.
     *
     * <p>Used when purchase system is installed via IAP class.</p>
     * <p>If Google changes the package identifier of Google Play, this method will not return the
     * new name.</p>
     */
    public static boolean isRunningViaGooglePlay(Activity activity) {

        PackageManager packageManager = activity.getPackageManager();
        List<PackageInfo> packages = packageManager
                .getInstalledPackages(0);
        for (PackageInfo packageInfo : packages) {
            String packageName = packageInfo.packageName;
            if (packageName.equals(GOOGLE_MARKET_NAME) || packageName.equals(GOOGLE_PLAY_STORE_NAME)) {
                return true;
            }
        }
        return false;
    }


    protected void runAsync(Runnable runnable) {
        new Thread(runnable).start();
    }

    private void loadSkusAndFillPurchaseInformation() {
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
        return googleInAppBillingService.isListeningForConnections();
    }

    @Override
    public void dispose() {
        googleInAppBillingService.dispose();
        clearCaches();
        observer = null;
    }

    @Override
    public void purchase(final String identifier) {
        assertInstalled();
        final OfferType offerType = getOfferType(identifier);

        googleInAppBillingService.startPurchaseRequest(identifier, new PurchaseRequestCallback() {
            @Override
            public void purchaseSuccess(Transaction transaction) {
                if (observer != null) {
                    switch (offerType) {
                        case CONSUMABLE:
                            // Warning: observer.handlePurchase is called in googleInAppBillingService.consumePurchase.
                            // That is not clean, I would prefer to keep it on one place.
                            // Should be refactored later.
                            googleInAppBillingService.consumePurchase(transaction, observer);
                            break;
                        case ENTITLEMENT:
                            observer.handlePurchase(transaction);
                            break;
                        default:
                            String error = "Unsupported OfferType=" + getOfferType(identifier)
                                    + " for identifier=" + identifier;
                            throw new GdxPayException(error);
                    }
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

    private OfferType getOfferType(String identifier) {
        Offer offer = purchaseManagerConfig.getOffer(identifier);
        if (offer == null || offer.getType() == null) {
            throw new IllegalStateException("No offer or offerType configured for identifier: " + identifier + ", offer: " + offer);
        }

        return offer.getType();
    }

    // TODO: call in new thread if called from UI thread (check if this is necessary).
    @Override
    public void purchaseRestore() {

        try {
            List<Transaction> transactions = googleInAppBillingService.getPurchases();
            Array<Transaction> entitlements = new Array<>(Transaction.class);
            for (int i = 0; i < transactions.size(); i++) {
                Transaction transaction = transactions.get(i);
                if (OfferType.CONSUMABLE == getOfferType(transaction.getIdentifier())) {
                    googleInAppBillingService.consumePurchase(transaction, observer);
                } else {
                    entitlements.add(transaction);
                }
            }


            if (observer != null) {
                observer.handleRestore(entitlements.toArray());
            }
        } catch (GdxPayException e) {
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

    private void onServiceConnected(final PurchaseObserver observer, final boolean autofetchInformation) {
        runAsync(new Runnable() {
            @Override
            public void run() {

                try {
                    if (autofetchInformation) {
                        loadSkusAndFillPurchaseInformation();
                    }
                } catch (Exception e) {
                    // TODO: this situation not yet unit-tested.
                    Log.e(LOG_TAG, "Failed to load skus in onServiceConnected()", e);
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

    @Override
    public void cancelTestPurchases() {
        googleInAppBillingService.cancelTestPurchases();
    }
}
