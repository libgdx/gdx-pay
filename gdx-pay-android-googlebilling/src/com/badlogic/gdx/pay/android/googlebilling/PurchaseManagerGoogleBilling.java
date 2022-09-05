package com.badlogic.gdx.pay.android.googlebilling;

import android.app.Activity;
import com.android.billingclient.api.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.pay.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The purchase manager implementation for Google Play (Android) using Google Billing Library
 * <p>
 * <a href="https://developer.android.com/google/play/billing/billing_java_kotlin">Reference docs</a>
 * <p>
 * Created by Benjamin Schulte on 07.07.2018.
 */

public class PurchaseManagerGoogleBilling implements PurchaseManager, PurchasesUpdatedListener {
    private static final String TAG = "GdxPay/GoogleBilling";
    private final Map<String, Information> informationMap = new ConcurrentHashMap<>();
    private final Activity activity;
    private final Map<String, ProductDetails> productDetailsMap = new HashMap<>();
    private boolean serviceConnected;
    private boolean installationComplete;
    private BillingClient mBillingClient;
    private PurchaseObserver observer;
    private PurchaseManagerConfig config;

    public PurchaseManagerGoogleBilling(Activity activity) {
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
    public String storeName() {
        return PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE;
    }

    @Override
    public void install(PurchaseObserver observer, PurchaseManagerConfig config, boolean autoFetchInformation) {
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
            public void onBillingSetupFinished(@Nonnull BillingResult result) {
                int billingResponseCode = result.getResponseCode();

                Gdx.app.debug(TAG, "Setup finished. Response code: " + billingResponseCode);

                serviceConnected = (billingResponseCode == BillingClient.BillingResponseCode.OK);

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
        productDetailsMap.clear();
        int offerSize = config.getOfferCount();

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();

        for (int z = 0; z < offerSize; z++) {
            productList.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(config.getOffer(z).getIdentifierForStore(storeName()))
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build());
        }

        if (productList.isEmpty()) {
            Gdx.app.log(TAG, "No skus configured");
            setInstalledAndNotifyObserver();
            return;
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        mBillingClient.queryProductDetailsAsync(
                params,
                new ProductDetailsResponseListener() {
                    public void onProductDetailsResponse(@Nonnull BillingResult billingResult, @Nonnull List<ProductDetails> productDetailsList) {
                        int responseCode = billingResult.getResponseCode();
                        // it might happen that this was already disposed until the response comes back
                        if (observer == null || Gdx.app == null)
                            return;

                        if (responseCode != BillingClient.BillingResponseCode.OK) {
                            Gdx.app.error(TAG, "onProductDetailsResponse failed, error code is " + responseCode);
                            if (!installationComplete) {
                                observer.handleInstallError(new FetchItemInformationException(String.valueOf(responseCode)));
                            }

                        } else {
                            for (ProductDetails productDetails : productDetailsList) {
                                informationMap.put(productDetails.getProductId(), convertSkuDetailsToInformation
                                        (productDetails));
                                productDetailsMap.put(productDetails.getProductId(), productDetails);
                            }

                            setInstalledAndNotifyObserver();
                        }
                    }
                }
        );
    }

    private String mapOfferType(OfferType type) {
        switch (type) {
            case CONSUMABLE:
            case ENTITLEMENT:
                return BillingClient.ProductType.INAPP;
            case SUBSCRIPTION:
                return BillingClient.ProductType.SUBS;
        }
        throw new IllegalStateException("Unsupported OfferType: " + type);
    }

    private Information convertSkuDetailsToInformation(ProductDetails productDetails) {
        String priceString = productDetails.getOneTimePurchaseOfferDetails().getFormattedPrice();
        return Information.newBuilder()
                .localName(productDetails.getTitle())
                //TODO Removed - Dont know if this is applied in some other form
                //.freeTrialPeriod(convertToFreeTrialPeriod(productDetails.getFreeTrialPeriod()))
                .localDescription(productDetails.getDescription())
                .localPricing(priceString)
                .priceCurrencyCode(productDetails.getOneTimePurchaseOfferDetails().getPriceCurrencyCode())
                .priceInCents((int) (productDetails.getOneTimePurchaseOfferDetails().getPriceAmountMicros() / 10_000))
                .priceAsDouble(productDetails.getOneTimePurchaseOfferDetails().getPriceAmountMicros() / 1_000_000.0)
                .build();
    }

    /**
     * @param iso8601Duration in ISO 8601 format.
     */
    @Nullable
    private FreeTrialPeriod convertToFreeTrialPeriod(@Nullable  String iso8601Duration) {
        if (iso8601Duration == null || iso8601Duration.isEmpty()) {
            return null;
        }

        try {
            return Iso8601DurationStringToFreeTrialPeriodConverter.convertToFreeTrialPeriod(iso8601Duration);
        } catch(RuntimeException e) {
            Gdx.app.error(TAG, "Failed to parse iso8601Duration: " + iso8601Duration, e);
            return null;
        }
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
        ProductDetails productDetails = productDetailsMap.get(identifier);

        if (productDetails == null) {
            observer.handlePurchaseError(new InvalidItemException(identifier));
        } else {
            BillingResult billingResult = mBillingClient.launchBillingFlow(activity, getBillingFlowParams(productDetails).build());
            //billingResult.getResponseCode()
        }
    }

    /**
     * @param productDetails SKU details to set in the billing flow params.
     * @return The params builder to be used while launching the billing flow.
     */
    protected BillingFlowParams.Builder getBillingFlowParams(ProductDetails productDetails) {
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                Collections.singletonList(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                );

        return BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList);
    }

    @Override
    public void purchaseRestore() {
        mBillingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),

                new PurchasesResponseListener() {
                    @Override
                    public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> list) {
                        int responseCode = billingResult.getResponseCode();
                        if (responseCode == BillingClient.BillingResponseCode.OK && list != null) {
                            handlePurchase(list, true);
                        } else {
                            Gdx.app.error(TAG, "queryPurchases failed with responseCode " + responseCode);
                            observer.handleRestoreError(new GdxPayException("queryPurchases failed with " +
                                    "responseCode " + responseCode));
                        }
                    }
                });
    }

    @Override
    public void onPurchasesUpdated(BillingResult result, @Nullable List<Purchase> purchases) {
        int responseCode = result.getResponseCode();

        // check the edge case that the callback comes with a delay right after dispose() was called
        if (observer == null)
            return;

        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchase(purchases, false);
        } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            observer.handlePurchaseCanceled();
        } else if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            observer.handlePurchaseError(new ItemAlreadyOwnedException());
        } else if (responseCode == BillingClient.BillingResponseCode.ITEM_UNAVAILABLE) {
            observer.handlePurchaseError(new InvalidItemException());
        } else {
            Gdx.app.error(TAG, "onPurchasesUpdated failed with responseCode " + responseCode);
            observer.handlePurchaseError(new GdxPayException("onPurchasesUpdated failed with responseCode " +
                    responseCode));
        }

    }

    private void handlePurchase(List<Purchase> purchases, boolean fromRestore) {
        List<Transaction> transactions = new ArrayList<>(purchases.size());

        for (Purchase purchase : purchases) {
            // ignore pending purchases, just return successful purchases to the client
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                //TODO - Only handling one product per purchase. This can be changed at Play Store - In app Product Settings
                String product = purchase.getProducts().get(0);

                // build the transaction from the purchase object
                Transaction transaction = new Transaction();
                transaction.setIdentifier(product);
                transaction.setOrderId(purchase.getOrderId());
                transaction.setRequestId(purchase.getPurchaseToken());
                transaction.setStoreName(PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE);
                transaction.setPurchaseTime(new Date(purchase.getPurchaseTime()));
                transaction.setPurchaseText("Purchased: " + product);
                transaction.setReversalTime(null);
                transaction.setReversalText(null);
                transaction.setTransactionData(purchase.getOriginalJson());
                transaction.setTransactionDataSignature(purchase.getSignature());

                // if this is from restoring old transactions, we call handlePurchaseRestore with the complete list
                // from a direct purchase, we call handlePurchase directly
                if (fromRestore)
                    transactions.add(transaction);
                else
                    observer.handlePurchase(transaction);

                Offer purchasedOffer = config.getOffer(product);
                if (purchasedOffer != null) {
                    // CONSUMABLES need to get consumed, ENTITLEMENTS/SUBSCRIPTIONS need to geed acknowledged
                    switch (purchasedOffer.getType()) {
                        case CONSUMABLE:
                            mBillingClient.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(),
                                    new ConsumeResponseListener() {
                                        @Override
                                        public void onConsumeResponse(@Nonnull BillingResult result, @Nonnull String outToken) {
                                            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                                // handlepurchase is done before item is consumed for compatibility with other
                                                // gdx-pay implementations
                                                //TODO what to do if it did not return OK?
                                            }
                                        }
                                    });
                            break;
                        case ENTITLEMENT:
                        case SUBSCRIPTION:
                            if (!purchase.isAcknowledged()) {
                                mBillingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(),
                                        new AcknowledgePurchaseResponseListener() {
                                            @Override
                                            public void onAcknowledgePurchaseResponse(@Nonnull BillingResult billingResult) {
                                                // payment is acknowledged
                                            }
                                        });
                            }
                            break;
                    }
                }
            }
        }

        if (fromRestore)
            observer.handleRestore(transactions.toArray(new Transaction[0]));
    }
}
