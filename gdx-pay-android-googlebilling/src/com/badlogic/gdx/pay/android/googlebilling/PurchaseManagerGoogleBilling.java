package com.badlogic.gdx.pay.android.googlebilling;

import android.app.Activity;
import com.android.billingclient.api.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.pay.*;


import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PurchaseManagerGoogleBilling implements PurchaseManager, PurchasesUpdatedListener {
    private static final String TAG = "Sylveria:  ";
    private final Map<String, Information> informationMap = new ConcurrentHashMap<>();
    private final Activity activity;
    private Map<String, SkuDetails> skuDetailsMap = new HashMap<>();
    private boolean serviceConnected;
    private boolean installationComplete;
    private BillingClient mBillingClient;
    private PurchaseObserver observer;
    private PurchaseManagerConfig config;
    private AccountIdentifiers accountIdentifiers;


    public PurchaseManagerGoogleBilling( Activity activity ) {
        this.activity = activity;
        mBillingClient = BillingClient.newBuilder(activity).setListener(this)
                .enablePendingPurchases().build();
    }


    @Override
    public Information getInformation(String identifier) {
        Information information = informationMap.get(identifier);
        return information == null ? Information.UNAVAILABLE : information;
    }


    @Override
    public String storeName() { return PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE; }


    @Override
    public void install(
            PurchaseObserver observer,
            PurchaseManagerConfig config,
            boolean autoFetchInformation) {
        this.observer = observer;
        this.config = config;

        // make sure to call the observer again
        installationComplete = false;

        startServiceConnection(new Runnable() {
            @Override
            public void run() {
                // it might happen that this was already disposed until the service connection was established
                if (PurchaseManagerGoogleBilling.this.observer == null)
                    return;

                if (!serviceConnected)
                    PurchaseManagerGoogleBilling.this.observer.handleInstallError(
                            new GdxPayException("Connection to Play Billing not possible"));
                else
                    fetchOfferDetails();
            }
        });
    }
    

    private void startServiceConnection(final Runnable excecuteOnSetupFinished) {
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                int billingResponseCode = result.getResponseCode();

                Log.info(TAG +  "Setup finished. Response code: " + billingResponseCode);

                serviceConnected = (billingResponseCode == BillingClient.BillingResponseCode.OK);

                if (excecuteOnSetupFinished != null) {
                    excecuteOnSetupFinished.run();
                }
            }

            @Override
            public void onBillingServiceDisconnected() { serviceConnected = false; }
        });
    }


    //todo  This can be altered, so it doesn't support just one.
    private String getGlobalSkuTypeFromConfig() {
        String skuType = null;
        for (int z = 0; z < config.getOfferCount(); z++) {
            String offerSkuType = mapOfferType(config.getOffer(z).getType());
            if (skuType != null && !skuType.equals(offerSkuType)) {
                throw new IllegalStateException("Cannot support OfferType Subscription and other types in the same app");
            }
            skuType = offerSkuType;
        }

        return skuType;
    }



    private void fetchOfferDetails() {
        skuDetailsMap.clear();
        int offerSize = config.getOfferCount();
        List<String> skuList = new ArrayList<>(offerSize);
        for (int z = 0; z < offerSize; z++) {
            skuList.add(config.getOffer(z).getIdentifierForStore(storeName()));
        }

        if (skuList.isEmpty()) {
            Log.info(TAG +  "No skus configured");
            setInstalledAndNotifyObserver();
            return;
        }

        mBillingClient.querySkuDetailsAsync(
                SkuDetailsParams.newBuilder()
                        .setSkusList(skuList)
                        .setType(getGlobalSkuTypeFromConfig())
                        .build(),
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(
                            BillingResult result,
                            List<SkuDetails> skuDetailsList) {
                        int responseCode = result.getResponseCode();
                        // it might happen that this was already disposed until the response comes back
                        if (observer == null || Gdx.app == null)
                            return;

                        if (responseCode != BillingClient.BillingResponseCode.OK) {
                            Log.info(TAG + "onSkuDetailsResponse failed, error code is " + responseCode);
                            if (!installationComplete)
                                observer.handleInstallError(new FetchItemInformationException(
                                        String.valueOf(responseCode)));

                        } else {
                            if (skuDetailsList != null) {

                                for (SkuDetails skuDetails : skuDetailsList) {
                                    informationMap.put(skuDetails.getSku(), convertSkuDetailsToInformation
                                            (skuDetails));
                                    skuDetailsMap.put(skuDetails.getSku(), skuDetails);
                                }
                            } else {
                                Log.info(TAG +  "skuDetailsList is null");
                            }
                            setInstalledAndNotifyObserver();
                        }
                    }
                });
    }


    private String mapOfferType(OfferType type) {
        switch (type) {
            case CONSUMABLE:
                return BillingClient.SkuType.INAPP;
            case ENTITLEMENT:
                return BillingClient.SkuType.INAPP;
            case SUBSCRIPTION:
                return BillingClient.SkuType.SUBS;
        }
        throw new IllegalStateException("Unsupported OfferType: " + type);
    }


    private Information convertSkuDetailsToInformation( SkuDetails skuDetails ) {
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
    public boolean installed() { return installationComplete; }


    @Override
    public void dispose() {
        if (observer != null) {
            // remove observer and config as well
            observer = null;
            config = null;
        }
        if (mBillingClient != null && mBillingClient.isReady()) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
        installationComplete = false;
    }


    @Override
    public void purchase(String identifier) {
        try {
            SkuDetails skuDetails = skuDetailsMap.get(identifier);

            if (skuDetails == null) {
                observer.handlePurchaseError(new InvalidItemException(identifier));
            } else {

                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetails)
                        .build();
                mBillingClient.launchBillingFlow(
                        activity,
                        flowParams);
            }
        } catch ( Exception e ) {
            Log.info( "Sylveria:  Error during Purchase." );
        }
    }


    @Override
    public void purchaseRestore() {
        Log.info(TAG + "entering purchaseRestore");
        try {
            //Checks if SUBSCRIPTIONS are active, and acknowledge them if necessary.
            Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases( BillingClient.SkuType.SUBS );

            int responseCode = purchasesResult.getResponseCode();
            List<Purchase> purchases = purchasesResult.getPurchasesList();

            if ( purchases != null ) {
                Log.info("Sylveria:  Purchases not NULL.  " + purchases.size() + " available." );
                for ( Purchase purchase : purchases ) {
                    Log.info( TAG + "Purchase acknowledged?  " + purchase.isAcknowledged() );
                    boolean purchasedResult = false;
                    if ( purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED ) {
                        purchasedResult = true;

                        //Log.info( TAG + "JSON = " + purchase.getOriginalJson() );
                    }
                    Log.info( TAG + "Purchase registers as purchased?  " + purchasedResult );
                    Log.info( TAG + "Purchase has the SKU:  " + purchase.getSku() );
                    checkAcknowledgementAndConfirm( purchase );
                    Log.info( TAG + "Reported as " + purchase.getSku() );

                    Map< String, Boolean > dataPass = new ConcurrentHashMap<>();
                    dataPass.put(
                            purchase.getSku(),
                            purchase.isAcknowledged() );

                    observer.handleRestore( dataPass );
                }
            }
        } catch (Exception e) {
            Log.info("Sylveria:  Error during PurchaseRestore." );
        }

        try {
            //Checks if CONSUMABLES and ENTITLEMENTS are active and acknowledges them if necessary.
            Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases( BillingClient.SkuType.INAPP );

            int responseCode = purchasesResult.getResponseCode();
            List<Purchase> purchases = purchasesResult.getPurchasesList();

            if ( purchases != null ) {
                Log.info("Sylveria:  Purchases not NULL.  " + purchases.size() + " available." );
                for ( Purchase purchase : purchases ) {
                    Log.info( TAG + "Purchase acknowledged?  " + purchase.isAcknowledged() );
                    boolean purchasedResult = false;
                    if ( purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED ) {
                        purchasedResult = true;
                    }
                    Log.info( TAG + "Purchase registers as purchased?  " + purchasedResult );
                    Log.info( TAG + "Purchase has the SKU:  " + purchase.getSku() );
                    checkAcknowledgementAndConfirm( purchase );
                }
            }
        } catch (Exception e) {
            Log.info("Sylveria:  Error during PurchaseRestore." );
        }
    }


    private void checkAcknowledgementAndConfirm( Purchase purchase ) {
        if (!purchase.isAcknowledged()) {
            mBillingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(),
                    new AcknowledgePurchaseResponseListener() {
                        @Override
                        public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                            Log.info( TAG + "Tried to acknowledge purchase." );
                        }
                    });
        }
        Log.info( TAG + "Double-checking.  Is purchase acknowledged?  " + purchase.isAcknowledged() );
    }


    @Override
    public void onPurchasesUpdated(
            BillingResult result,
            @Nullable List<Purchase> purchases) {
        //THIS TRIGGERS AFTER A SUCCESSFUL PURCHASE.
        Log.info(TAG + "entering PurchasesUpdated");
        try {
            int responseCode = result.getResponseCode();

            if (observer == null) {
                return;
            }

            //This will fire if the user bought something.
            if ( purchases != null) {
                if (responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.info(TAG + "onPurchasesUpdated responseCode " + responseCode + " of OK");
                    //handlePurchase( purchases );
                } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    Log.info(TAG + "onPurchasesUpdated responseCode " + responseCode + " of USER_CANCELED");

                } else if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                    Log.info(TAG + "onPurchasesUpdated responseCode " + responseCode + " of ITEM_ALREADY_OWNED");

                } else if (responseCode == BillingClient.BillingResponseCode.ITEM_UNAVAILABLE) {
                    Log.info(TAG + "onPurchasesUpdated responseCode " + responseCode + " of ITEM_UNAVAILABLE");

                } else if (responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
                    Log.info(TAG + "onPurchasesUpdated responseCode " + responseCode + " of FEATURE_NOT_SUPPORTED");

                } else if (responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                    Log.info(TAG + "onPurchasesUpdated responseCode " + responseCode + " of BILLING_UNAVAILABLE");

                } else if (responseCode == BillingClient.BillingResponseCode.SERVICE_TIMEOUT) {
                    Log.info(TAG + "onPurchasesUpdated responseCode " + responseCode + " of SERVICE_TIMEOUT");

                } else if (responseCode == BillingClient.BillingResponseCode.ERROR) {
                    Log.info(TAG + "onPurchasesUpdated responseCode " + responseCode + " of ERROR");

                } else if (responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
                    Log.info(TAG + "onPurchasesUpdated responseCode " + responseCode + " of SERVICE_DISCONNECTED");

                } else {
                    Log.info(TAG + "onPurchasesUpdated failed with responseCode " + responseCode);
                    observer.handlePurchaseError(
                            new GdxPayException("onPurchasesUpdated failed with responseCode " + responseCode));
                }
            }
        } catch( Exception e ) {
            Log.info( "Sylveria:  Error during onPurchasesUpdate. ");
        }
    }


    private void handlePurchase( Purchase purchase) {
        Log.info(TAG + "entering handlePurchase" );
        try {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
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
                transaction.setTransactionDataSignature(purchase.getSignature());

                //Send a request to the game.
                observer.handlePurchase(transaction);

                Offer purchasedOffer = config.getOffer(purchase.getSku());
                if (purchasedOffer != null) {
                    //CONSUMABLES need consuming, while ENTITLEMENTS and SUBSCRIPTIONS both need acknowledging.
                    switch (purchasedOffer.getType()) {
                        case CONSUMABLE:
                            mBillingClient.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(),
                                new ConsumeResponseListener() {
                                    @Override
                                    public void onConsumeResponse(
                                            BillingResult result,
                                            String outToken) {
                                        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                                            //TODO what to do if it did not return OK?
                                            //   OLD NOTES:  handlepurchase is done before item is consumed for compatibility with other
                                            //   gdx-pay implementations
                                        }
                                    }
                                });
                            break;
                        case ENTITLEMENT:
                            //Not implemented.
                            break;
                        case SUBSCRIPTION:
                            //THIS IS USEFUL FOR ACKNOWLEDGING A PURCHASE.
                            if (!purchase.isAcknowledged()) {
                                mBillingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(),
                                        new AcknowledgePurchaseResponseListener() {
                                            @Override
                                            public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                                                // payment is acknowledged
                                            }
                                        });
                            }
                            break;
                    }
                }
            }
        } catch( Exception e ) {
            Log.info( "Sylveria:  Error during handlePurchase." );
        }
    }

}
