package com.badlogic.gdx.pay.android.googlebilling;

import android.app.Activity;
import android.support.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.pay.FetchItemInformationException;
import com.badlogic.gdx.pay.GdxPayException;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.ItemAlreadyOwnedException;
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
    private final Map<String, Information> informationMap = new ConcurrentHashMap<>();
    private final Activity activity;
    private boolean serviceConnected;
    private boolean installationComplete;
    private BillingClient mBillingClient;
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
                    observer.handleInstallError(new GdxPayException("Connection to Play Billing not possible"));
                else if (autoFetchInformation) {
                    fetchOfferDetails();
                } else
                    setInstalledAndNotifyObserver();
            }
        });
    }

    private void startServiceConnection(final Runnable excecuteOnSetupFinished) {
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
        List<String> skuList = new ArrayList<>(offerSize);
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
                                    observer.handleInstallError(new FetchItemInformationException(
                                            String.valueOf(responseCode)));

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
        if (observer != null) {
            // remove observer and config as well
            observer = null;
            config = null;
            Gdx.app.log(TAG, "disposed observer and config");
        }
        if (mBillingClient != null && mBillingClient.isReady()) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
        installationComplete = false;
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
        mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, new PurchaseHistoryResponseListener() {
            @Override
            public void onPurchaseHistoryResponse(int responseCode, List<Purchase> purchases) {
                if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
                    handlePurchase(purchases, true);
                } else {
                    Gdx.app.error(TAG, "onPurchaseHistoryResponse failed with responseCode " + responseCode);
                    observer.handleRestoreError(new GdxPayException("onPurchaseHistoryResponse failed with " +
                            "responseCode " + responseCode));
                }
            }
        });

    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        // check the edge case that the callback comes with a delay right after dispose() was called
        if (observer == null)
            return;

        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            handlePurchase(purchases, false);
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            observer.handlePurchaseCanceled();
        } else if (responseCode == BillingClient.BillingResponse.ITEM_ALREADY_OWNED) {
            observer.handlePurchaseError(new ItemAlreadyOwnedException());
        } else {
            Gdx.app.error(TAG, "onPurchasesUpdated failed with responseCode " + responseCode);
            observer.handlePurchaseError(new GdxPayException("onPurchasesUpdated failed with responseCode " +
                    responseCode));
        }

    }

    private void handlePurchase(List<Purchase> purchases, boolean fromRestore) {
        List<Transaction> transactions = new ArrayList<>(purchases.size());

        for (Purchase purchase : purchases) {
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

            // if this is from restoring old transactions, we call handlePurchaseRestore with the complete list
            // from a direct purchase, we call handlePurchase directly
            if (fromRestore)
                transactions.add(transaction);
            else
                observer.handlePurchase(transaction);

            // CONSUMABLES need to get consumed
            if (config.getOffer(purchase.getSku()).getType().equals(OfferType.CONSUMABLE)) {
                mBillingClient.consumeAsync(purchase.getPurchaseToken(), new ConsumeResponseListener() {
                    @Override
                    public void onConsumeResponse(@BillingClient.BillingResponse int responseCode, String outToken) {
                        if (responseCode == BillingClient.BillingResponse.OK) {
                            // handlepurchase is done before item is consumed for compatibility with other
                            // gdx-pay implementations
                            //TODO what to do if it did not return OK?
                        }
                    }
                });
            }
        }

        if (fromRestore)
            observer.handleRestore(transactions.toArray(new Transaction[transactions.size()]));
    }
}
