package com.badlogic.gdx.pay.android.googlebilling;

import android.app.Activity;
import android.support.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.OfferType;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;

import java.util.ArrayList;
import java.util.Date;
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
    private final Activity activity;
    private boolean serviceConnected;
    private boolean installationComplete;
    private PurchaseObserver observer;
    private PurchaseManagerConfig config;

    public PurchaseManagerGoogleBilling(Activity activity) {
        this.activity = activity;
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
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSku(identifier)
                .setType(BillingClient.SkuType.INAPP) // SkuType.SUB for subscription
                .build();
        int responseCode = mBillingClient.launchBillingFlow(activity, flowParams);
    }

    @Override
    public void purchaseRestore() {
        //TODO
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            observer.handlePurchaseCanceled();
        } else {
            Gdx.app.error(TAG, "onPurchasesUpdated failed with responseCode " + responseCode);
            observer.handlePurchaseError(new RuntimeException("onPurchasesUpdated failed with responseCode " +
                    responseCode));
        }

    }

    private void handlePurchase(Purchase purchase) {
        // build the transaction from the purchase object
        Transaction transaction = new Transaction();
        transaction.setIdentifier(purchase.getSku());
        transaction.setOrderId(purchase.getOrderId());
        transaction.setRequestId(purchase.getPurchaseToken());
        transaction.setStoreName(PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE);
        transaction.setPurchaseTime(new Date(purchase.getPurchaseTime()));
        transaction.setPurchaseText("Purchased: " + purchase.getSku());
        transaction.setReversalTime(null);
        transaction.setReversalText(null);
        transaction.setTransactionData(purchase.getOriginalJson());

        observer.handlePurchase(transaction);

        // CONSUMABLES need to get consumed
        if (config.getOffer(purchase.getSku()).getType().equals(OfferType.CONSUMABLE)) {
            ConsumeResponseListener listener = new ConsumeResponseListener() {
                @Override
                public void onConsumeResponse(@BillingClient.BillingResponse int responseCode, String outToken) {
                    if (responseCode == BillingClient.BillingResponse.OK) {
                        // Handle the success of the consume operation.
                        // For example, increase the number of coins inside the user&#39;s basket.
                    }
                }
            };

            mBillingClient.consumeAsync(purchase.getPurchaseToken(), listener);
        }
    }
}
