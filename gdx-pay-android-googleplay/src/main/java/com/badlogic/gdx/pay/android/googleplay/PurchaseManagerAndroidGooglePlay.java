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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;

import java.util.ArrayList;

/** The purchase manager implementation for Google Play (Android).
 * <p>
 * Include the gdx-pay-android-googleplay.jar for this to work (plus gdx-pay-android.jar). Also update the "uses-permission" settings
 * in AndroidManifest.xml and your proguard settings.
 *
 * @author noblemaster */
public class PurchaseManagerAndroidGooglePlay implements PurchaseManager {

    public static final int BILLING_API_VERSION = 3;
    public static final String PURCHASE_TYPE_IN_APP = "inapp";

    /** Our Android activity. */
    private Activity activity;
    /** The request code to use for onActivityResult (arbitrary chosen). */
    private int requestCode;

    private ServiceConnection inAppBillingServiceConnection;

    private IInAppBillingService inAppBillingService;

    Logger logger = new GdxAppLogger("GdxPay/AndroidGooglePlay");

    public PurchaseManagerAndroidGooglePlay(Activity activity, int requestCode) {
        this.activity = activity;

        // the request code for onActivityResult
        this.requestCode = requestCode;
    }

    @Override
    public void install(final PurchaseObserver observer, final PurchaseManagerConfig config, final boolean autoFetchInformation) {

        try {
            inAppBillingServiceConnection = new BillingServiceInitializingServiceConnection(observer, config);

            activity.bindService(createBindBillingServiceIntent(), inAppBillingServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            observer.handleInstallError(new GdxPayInstallFailureException(e, config));
        }
    }

    // TODO: really run async.
    protected void runAsync(Runnable runnable) {
        runnable.run();
    }

    private Intent createBindBillingServiceIntent() {
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        return serviceIntent;
    }

    private void requestSkus(final PurchaseObserver observer, final PurchaseManagerConfig purchaseManagerConfig) {
        runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle skuDetails = inAppBillingService.getSkuDetails(BILLING_API_VERSION,
                            activity.getPackageName(), PURCHASE_TYPE_IN_APP,
                            createSkus(purchaseManagerConfig));

                    convertSkuResponseToInventory(skuDetails, observer);
                } catch (RemoteException e) {
                    // TODO: not yet unit tested.
                    observer.handleInstallError(new GdxPayInstallFailureException(e, purchaseManagerConfig));
                }
            }
        });
    }

    private void convertSkuResponseToInventory(Bundle skuDetails, PurchaseObserver observer) {
        // TODO: store data, and unit test

        observer.handleInstall();
    }

    private Bundle createSkus(PurchaseManagerConfig purchaseManagerConfig) {
        Bundle bundle = new Bundle();

        ArrayList<String> skuList = new ArrayList<String>();

        for (int i = 0; i < purchaseManagerConfig.getOfferCount(); i++) {
            Offer offer = purchaseManagerConfig.getOffer(i);

            skuList.add(offer.getIdentifier());
        }

        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

        return bundle;
    }


    @Override
    public boolean installed() {
        // FIXME
        return false;
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


    private class BillingServiceInitializingServiceConnection implements ServiceConnection {
        private final PurchaseObserver observer;
        private final PurchaseManagerConfig config;

        public BillingServiceInitializingServiceConnection(PurchaseObserver observer, PurchaseManagerConfig config) {
            this.observer = observer;
            this.config = config;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            inAppBillingService = lookupByStubAsInterface(service);

            logger.debug("CashierAndroidGoogle: Service Connected SUCCESSFULLY!");

            requestSkus(observer, config);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            inAppBillingService = null;

            logger.debug("CashierAndroidGoogle: Service Disconnected.");
        }
    }

    protected IInAppBillingService lookupByStubAsInterface(IBinder service) {
        return IInAppBillingService.Stub.asInterface(service);
    }
}
