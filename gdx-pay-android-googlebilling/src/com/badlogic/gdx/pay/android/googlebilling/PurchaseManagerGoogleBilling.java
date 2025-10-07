package com.badlogic.gdx.pay.android.googlebilling;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import com.android.billingclient.api.*;
import com.android.billingclient.api.BillingClient.ProductType;
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
 * Exponential back-off copied from (a clone of) an
 * <a href="https://github.com/john990/play-billing-samples/blob/main/TrivialDriveJava/app/src/main/java/com/sample/android/trivialdrivesample/billing/BillingDataSource.java">Android
 * sample</a>.
 * <p>
 * Created by Benjamin Schulte on 07.07.2018.
 */

public class PurchaseManagerGoogleBilling implements PurchaseManager, PurchasesUpdatedListener {
    private static final String TAG = "GdxPay/GoogleBilling";
    private static final long RECONNECT_TIMER_START_MILLISECONDS = 1000L;
    private static final long RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L; // 15 mins
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private final Map<String, Information> informationMap = new ConcurrentHashMap<>();
    private final Activity activity;
    private final Map<String, ProductDetails> productDetailsMap = new HashMap<>();
    private boolean serviceConnected;
    private boolean installationComplete;
    private BillingClient mBillingClient;
    private PurchaseObserver observer;
    private PurchaseManagerConfig config;
    private boolean hasBillingSetupFinishedSuccessfully = false;
    private long reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS;

    public PurchaseManagerGoogleBilling(Activity activity) {
        this.activity = activity;
        PendingPurchasesParams params = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build();

        mBillingClient = BillingClient.newBuilder(activity)
                .setListener(this)
                .enablePendingPurchases(params)
                .build();
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

        Gdx.app.debug(TAG, "Called install()");
        this.observer = observer;
        this.config = config;

        // make sure to call the observer again
        installationComplete = false;

        startServiceConnection(this::handleBillingSetupFinished);
    }

    private void handleBillingSetupFinished() {
        // it might happen that this was already disposed until the service connection was established
        if (PurchaseManagerGoogleBilling.this.observer == null)
            return;

        if (!serviceConnected)
            PurchaseManagerGoogleBilling.this.observer.handleInstallError(
                    new GdxPayException("Connection to Play Billing not possible"));
        else
            fetchOfferDetails();
    }

    private void startServiceConnection(final Runnable executeOnSetupFinished) {
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@Nonnull BillingResult result) {
                int billingResponseCode = result.getResponseCode();

                Gdx.app.debug(TAG, "Setup finished. Response code: " + billingResponseCode);

                serviceConnected = (billingResponseCode == BillingClient.BillingResponseCode.OK);
                if (serviceConnected) {
                    reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS;
                    hasBillingSetupFinishedSuccessfully = true;
                } else {
                    reconnectWithService();
                }

                if (executeOnSetupFinished != null) {
                    executeOnSetupFinished.run();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                serviceConnected = false;
                reconnectWithService();
            }
        });
    }

    private void reconnectWithService() {
        handler.postDelayed(() -> {
            Runnable executeOnSetupFinished = hasBillingSetupFinishedSuccessfully
                    ? null
                    : PurchaseManagerGoogleBilling.this::handleBillingSetupFinished;
            startServiceConnection(executeOnSetupFinished);
        }, reconnectMilliseconds);
        reconnectMilliseconds = Math.min(reconnectMilliseconds * 2, RECONNECT_TIMER_MAX_TIME_MILLISECONDS);
    }

    private void fetchOfferDetails() {
        Gdx.app.debug(TAG,"Called fetchOfferDetails()");
        productDetailsMap.clear();
        int offerSize = config.getOfferCount();

        List<QueryProductDetailsParams.Product> inAppProducts = new ArrayList<>();
        List<QueryProductDetailsParams.Product> subsProducts = new ArrayList<>();

        for (int z = 0; z < offerSize; z++) {
            Offer offer = config.getOffer(z);
            String productType = mapOfferType(offer.getType());
            QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(offer.getIdentifierForStore(storeName()))
                    .setProductType(productType)
                    .build();

            if (ProductType.SUBS.equals(productType)) {
                subsProducts.add(product);
            } else {
                inAppProducts.add(product);
            }
        }

        if (inAppProducts.isEmpty() && subsProducts.isEmpty()) {
            Gdx.app.debug(TAG, "No products configured");
            setInstalledAndNotifyObserver();
            return;
        }

        // Execute per-product-type queries and only complete install after all succeed.
        int totalBatches = (inAppProducts.isEmpty() ? 0 : 1) + (subsProducts.isEmpty() ? 0 : 1);
        final int[] remainingBatches = new int[] { totalBatches };
        final boolean[] failed = new boolean[] { false };

        Runnable onBatchSuccess = () -> {
            if (remainingBatches[0] > 0) {
                remainingBatches[0] -= 1;
            }
            if (remainingBatches[0] == 0 && !failed[0]) {
                setInstalledAndNotifyObserver();
            }
        };

        java.util.function.Consumer<Exception> onBatchError = (Exception e) -> {
            // Mark failure to prevent later handleInstall() and notify observer once.
            failed[0] = true;
            installationComplete = true; // clear "installing" state to avoid deadlock
            Gdx.app.error(TAG, "Failed to fetch product details", e);
            if (observer != null) {
                observer.handleInstallError(e);
            }
        };

        if (!inAppProducts.isEmpty()) {
            queryProductDetailsForProducts(inAppProducts, ProductType.INAPP, onBatchSuccess, onBatchError);
        }
        if (!subsProducts.isEmpty()) {
            queryProductDetailsForProducts(subsProducts, ProductType.SUBS, onBatchSuccess, onBatchError);
        }
    }

    private void queryProductDetailsForProducts(List<QueryProductDetailsParams.Product> productList,
                                                String productTypeHint,
                                                Runnable onSuccess,
                                                java.util.function.Consumer<Exception> onError) {
        // We avoid mixing types in a single request by design (split beforehand).
        // If productList is empty, treat as success to allow UI to show "Unavailable".
        if (productList == null || productList.isEmpty()) {
            Gdx.app.debug(TAG, "Empty product list for type: " + productTypeHint + " — treating as success");
            onSuccess.run();
            return;
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        Gdx.app.debug(TAG, "QueryProductDetailsParams (type: " + productTypeHint + "): " + params);
        mBillingClient.queryProductDetailsAsync(
                params,
                (billingResult, productDetailsResult) -> {
                    // it might happen that this was already disposed until the response comes back
                    if (observer == null || Gdx.app == null) return;

                    int responseCode = billingResult.getResponseCode();
                    if (responseCode != BillingClient.BillingResponseCode.OK) {
                        onError.accept(new FetchItemInformationException(String.valueOf(responseCode)));
                        return;
                    }

                    List<ProductDetails> productDetailsList = productDetailsResult.getProductDetailsList();

                    Gdx.app.debug(TAG, "Retrieved product count (batch " + productTypeHint + "): " + productDetailsList.size());
                    for (ProductDetails productDetails : productDetailsList) {
                        informationMap.put(productDetails.getProductId(), convertProductDetailsToInformation(productDetails));
                        productDetailsMap.put(productDetails.getProductId(), productDetails);
                    }

                    // Even if empty, we still consider this batch a success to allow the app to show "Unavailable"
                    onSuccess.run();
                }
        );
    }

    private String mapOfferType(OfferType type) {
        switch (type) {
            case CONSUMABLE:
            case ENTITLEMENT:
                return ProductType.INAPP;
            case SUBSCRIPTION:
                return ProductType.SUBS;
        }
        throw new IllegalStateException("Unsupported OfferType: " + type);
    }

    private Information convertProductDetailsToInformation(ProductDetails productDetails) {
        Gdx.app.debug(TAG, "Converting productDetails: \n" + productDetails);

        Information.Builder builder = Information.newBuilder()
                .localName(productDetails.getTitle())
                .localDescription(productDetails.getDescription());

        if (ProductType.SUBS.equals(productDetails.getProductType())) {
            convertSubscriptionProductToInformation(builder, productDetails.getSubscriptionOfferDetails());
        } else {

            ProductDetails.OneTimePurchaseOfferDetails details = productDetails.getOneTimePurchaseOfferDetails();
            if (details != null) {
                convertOneTimeProductToInformation(builder, details);
            }
        }
        return builder.build();
    }

    private void convertSubscriptionProductToInformation(Information.Builder builder,
                                                         @Nullable List<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails) {
        if (subscriptionOfferDetails == null || subscriptionOfferDetails.isEmpty()) {
            Gdx.app.error(TAG, "Empty SubscriptionOfferDetails");
            return;
        }
        ProductDetails.SubscriptionOfferDetails details = getActiveSubscriptionOfferDetails(subscriptionOfferDetails);
        if (details.getPricingPhases().getPricingPhaseList().isEmpty()) {
            Gdx.app.error(TAG, "getPricingPhases()  or empty ");
            return;
        }


        ProductDetails.PricingPhase paidForPricingPhase = getPaidRecurringPricingPhase(details);
        if (paidForPricingPhase == null) {
            Gdx.app.error(TAG, "no paidRecurringPricingPhase found ");
            return;

        }

        builder.localPricing(paidForPricingPhase.getFormattedPrice())
                .priceCurrencyCode(paidForPricingPhase.getPriceCurrencyCode())
                .priceInCents((int) paidForPricingPhase.getPriceAmountMicros() / 10_000)
                .priceAsDouble(paidForPricingPhase.getPriceAmountMicros() / 1_000_000.0)
        ;

        ProductDetails.PricingPhase freeTrialSubscriptionPhase = getFreeTrialSubscriptionPhase(details.getPricingPhases());

        if (freeTrialSubscriptionPhase != null) {
            builder.freeTrialPeriod(convertToFreeTrialPeriod(freeTrialSubscriptionPhase.getBillingPeriod(), freeTrialSubscriptionPhase.getBillingCycleCount()));
        }
    }

    private ProductDetails.SubscriptionOfferDetails getActiveSubscriptionOfferDetails(List<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails) {
        // TODO Google Play supports multiple SubscriptionOfferDetails which we don't support. Enhancement can be added here.
        return subscriptionOfferDetails.get(0);
    }

    @Nullable
    private ProductDetails.PricingPhase getPaidRecurringPricingPhase(ProductDetails.SubscriptionOfferDetails details) {
        for(ProductDetails.PricingPhase phase : details.getPricingPhases().getPricingPhaseList()) {
            if (isPaidForSubscriptionPhase(phase)) {
                return phase;
            }
        }
        return null;
    }

    private static boolean isPaidForSubscriptionPhase(ProductDetails.PricingPhase phase) {
        return phase.getPriceAmountMicros() > 0;
    }

    @Nullable
    private static ProductDetails.PricingPhase getFreeTrialSubscriptionPhase(ProductDetails.PricingPhases phases) {
        for(ProductDetails.PricingPhase phase : phases.getPricingPhaseList()) {
            if (isFreeTrialSubscriptionPhase(phase)) {
                return phase;
            }
        }
        return null;
    }


    /**
     * Free trial periods come in two ways:
     * RecurrenceMode.NON_RECURRING (detected before 2023, android 10)
     * RecurrenceMode.FINITE_RECURRING with billingCycleCount of 1 (detected in january 2023)
     *
     */
    private static boolean isFreeTrialSubscriptionPhase(ProductDetails.PricingPhase phase) {
        return phase.getPriceAmountMicros() == 0L &&
                (phase.getRecurrenceMode() == ProductDetails.RecurrenceMode.NON_RECURRING  || phase.getRecurrenceMode() == ProductDetails.RecurrenceMode.FINITE_RECURRING);
    }

    private static void convertOneTimeProductToInformation(Information.Builder builder, @Nullable ProductDetails.OneTimePurchaseOfferDetails oneTimePurchaseDetails) {

        String priceString = oneTimePurchaseDetails.getFormattedPrice();
        builder
                .localPricing(priceString)
                .priceCurrencyCode(oneTimePurchaseDetails.getPriceCurrencyCode())
                .priceInCents((int) (oneTimePurchaseDetails.getPriceAmountMicros() / 10_000))
                .priceAsDouble(oneTimePurchaseDetails.getPriceAmountMicros() / 1_000_000.0);
    }

    /**
     * @param iso8601Duration in ISO 8601 format.
     * @param billingCycleCount the number of billing cycles. When testing it, we found value 1 for FINITE_RECURRING cycles
     */
    @Nullable
    private FreeTrialPeriod convertToFreeTrialPeriod(@Nullable  String iso8601Duration, int billingCycleCount) {
        if (iso8601Duration == null || iso8601Duration.isEmpty()) {
            return null;
        }

        try {
            FreeTrialPeriod freeTrialPeriod = Iso8601DurationStringToFreeTrialPeriodConverter.convertToFreeTrialPeriod(iso8601Duration);
            if (billingCycleCount > 1) {
                freeTrialPeriod = new FreeTrialPeriod(freeTrialPeriod.getNumberOfUnits() * billingCycleCount, freeTrialPeriod.getUnit());
            }
            return freeTrialPeriod;
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
            Gdx.app.debug(TAG, "disposed observer and config");
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
     * @return The params builder to be used while launching the billing flow.
     */
    protected BillingFlowParams.Builder getBillingFlowParams(ProductDetails productDetails) {
        final List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList;

        if (productDetails.getProductType().equals(ProductType.INAPP)) {
            productDetailsParamsList =
                    Collections.singletonList(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .build()
                    );
        }
        else {
            List<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails = productDetails
                    .getSubscriptionOfferDetails();
            final String offerToken;

            if (subscriptionOfferDetails == null || subscriptionOfferDetails.isEmpty()) {
                Gdx.app.error(TAG, "subscriptionOfferDetails are empty for product: " + productDetails);
                offerToken = null;
            } else {
                offerToken = getActiveSubscriptionOfferDetails(subscriptionOfferDetails) // HOW TO SPECIFY AN ALTERNATE OFFER USING gdx-pay?
                        .getOfferToken();
            }

            productDetailsParamsList =
                    Collections.singletonList(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .setOfferToken(offerToken)
                                    .build()
                    );
        }

        return BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList);
    }




    @Override
    public void purchaseRestore() {
        final String productType;
        if (this.config.hasAnyOfferWithType(OfferType.SUBSCRIPTION)) {
            productType = ProductType.SUBS;
        } else {
            productType = ProductType.INAPP;
        }

        mBillingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(productType).build(),

                (billingResult, list) -> {
                    int responseCode = billingResult.getResponseCode();
                    if (responseCode == BillingClient.BillingResponseCode.OK) {
                        handlePurchase(list, true);
                    } else {
                        Gdx.app.error(TAG, "queryPurchases failed with responseCode " + responseCode);
                        observer.handleRestoreError(new GdxPayException("queryPurchases failed with " +
                                "responseCode " + responseCode));
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
                                                // handlePurchase is done before item is consumed for compatibility with other
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