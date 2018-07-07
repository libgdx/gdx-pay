package com.badlogic.gdx.pay.android.googlebilling;

import android.app.Activity;
import android.support.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The purchase manager implementation for Google Play (Android) using Google Billing Library
 * <p>
 * https://developer.android.com/google/play/billing/billing_java_kotlin
 * <p>
 * Created by Benjamin Schulte on 07.07.2018.
 */

public class PurchaseManagerGoogleBilling implements PurchaseManager, PurchasesUpdatedListener {
    private static final String TAG = "GdxPay/GoogleBilling";
    private final BillingClient mBillingClient;
    private final Map<String, Information> informationMap = new ConcurrentHashMap<String, Information>();
    private boolean serviceConnected;
    private boolean installationComplete;
    private PurchaseObserver observer;
    private PurchaseManagerConfig config;

    public PurchaseManagerGoogleBilling(Activity activity) {
        mBillingClient = BillingClient.newBuilder(activity).setListener(this).build();
    }

    @Override
    public Information getInformation(String identifier) {
        Information information = informationMap.get(identifier);
        return information == null ? Information.UNAVAILABLE : information;
    }

    @Override
    public String storeName() {
        return PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE;
    }

    @Override
    public void install(final PurchaseObserver observer, PurchaseManagerConfig config, final boolean
            autoFetchInformation) {
        this.observer = observer;
        this.config = config;

        // make sure to call the observer again
        installationComplete = false;

        startServiceConnection(new Runnable() {
            @Override
            public void run() {
                if (!serviceConnected)
                    observer.handleInstallError(new RuntimeException("Connection to Play Billing not possible"));
                else if (autoFetchInformation) {
                    fetchOfferDetails();
                } else
                    setInstalledAndNotifyObserver();
            }
        });
    }

    public void startServiceConnection(final Runnable excecuteOnSetupFinished) {
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(int billingResponseCode) {
                Gdx.app.debug(TAG, "Setup finished. Response code: " + billingResponseCode);

                serviceConnected = (billingResponseCode == BillingClient.BillingResponse.OK);

                if (excecuteOnSetupFinished != null) {
                    excecuteOnSetupFinished.run();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                serviceConnected = false;
            }
        });
    }

    private void fetchOfferDetails() {
        int offerSize = config.getOfferCount();
        List skuList = new ArrayList<>(offerSize);
        for (int z = 0; z < config.getOfferCount(); z++) {
            skuList.add(config.getOffer(z).getIdentifierForStore(storeName()));
        }

        if (skuList.size() > 0) {

            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
            mBillingClient.querySkuDetailsAsync(params.build(),
                    new SkuDetailsResponseListener() {
                        @Override
                        public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {
                            if (responseCode != BillingClient.BillingResponse.OK) {
                                Gdx.app.error(TAG, "onSkuDetailsResponse failed, error code is " + responseCode);
                                if (!installationComplete)
                                    observer.handleInstallError(new RuntimeException("onSkuDetailsResponse failed, " +
                                            "status code is " + responseCode));

                            } else {
                                if (skuDetailsList != null) {
                                    for (SkuDetails skuDetails : skuDetailsList) {
                                        informationMap.put(skuDetails.getSku(), convertSkuDetailsToInformation
                                                (skuDetails));
                                    }
                                }
                                setInstalledAndNotifyObserver();
                            }
                        }
                    });
        } else
            setInstalledAndNotifyObserver();

    }

    private Information convertSkuDetailsToInformation(SkuDetails skuDetails) {
        String priceString = skuDetails.getPrice();
        return Information.newBuilder()
                .localName(skuDetails.getTitle())
                .localDescription(skuDetails.getDescription())
                .localPricing(priceString)
                .priceCurrencyCode(skuDetails.getPriceCurrencyCode())
                .priceInCents((int) (skuDetails.getPriceAmountMicros() / 10000))
                .build();
    }

    private void setInstalledAndNotifyObserver() {
        if (!installationComplete) {
            installationComplete = true;
            observer.handleInstall();
        }
    }

    @Override
    public boolean installed() {
        return installationComplete;
    }

    @Override
    public void dispose() {

    }

    @Override
    public void purchase(String identifier) {

    }

    @Override
    public void purchaseRestore() {

    }

    @Override
    public void onPurchasesUpdated(int i, @Nullable List<Purchase> list) {

    }
}
