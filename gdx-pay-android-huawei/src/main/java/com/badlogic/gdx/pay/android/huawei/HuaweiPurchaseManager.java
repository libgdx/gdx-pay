package com.badlogic.gdx.pay.android.huawei;

import android.app.Activity;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.OfferType;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseReq;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseResult;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.IsEnvReadyResult;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.OwnedPurchasesReq;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.iap.entity.ProductInfo;
import com.huawei.hms.iap.entity.ProductInfoReq;
import com.huawei.hms.iap.entity.ProductInfoResult;
import com.huawei.hms.iap.entity.PurchaseIntentReq;
import com.huawei.hms.iap.entity.PurchaseIntentResult;
import com.huawei.hms.support.api.client.Status;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The purchase manager implementation for Huawei App Gallery (Android) using HMS IAP
 * <p>
 * <a href="https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/iap-service-introduction-v4">Reference docs</a>
 * <p>
 * Created by Francesco Stranieri on 09.05.2020.
 */

public class HuaweiPurchaseManager implements PurchaseManager {

    private final Activity activity;
    private boolean installationComplete;
    private PurchaseObserver observer;
    private IAPListener iapListener;
    private ConcurrentHashMap<String, ProductInfo> productInfoMap = new ConcurrentHashMap<>();

    private boolean hasToFetchEntetliments = false;
    private boolean hasToFetchConsumables = false;
    private boolean hasToFetchSubscriptions = false;

    public HuaweiPurchaseManager(Activity activity, IAPListener iapListener) {
        this.activity = activity;
        this.iapListener = iapListener;
    }

    private void checkIAPStatus(final PurchaseManagerConfig config, final boolean autoFetchInformation) {
        Task<IsEnvReadyResult> task = Iap.getIapClient(activity).isEnvReady();
        task.addOnSuccessListener(new OnSuccessListener<IsEnvReadyResult>() {
            @Override
            public void onSuccess(IsEnvReadyResult result) {
                handleInstall(config, autoFetchInformation);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException) e;
                    Status status = apiException.getStatus();
                    if (status.getStatusCode() == OrderStatusCode.ORDER_HWID_NOT_LOGIN) {
                        // Not logged in.
                        if (status.hasResolution()) {
                            iapListener.onLoginRequired();
                        } else {
                            iapListener.onIAPError(apiException);
                        }
                    } else if (status.getStatusCode() == OrderStatusCode.ORDER_ACCOUNT_AREA_NOT_SUPPORTED) {
                        // The current region does not support HUAWEI IAP.
                        iapListener.onRegionNotSupported();
                    } else {
                        iapListener.onError(e);
                    }
                }
            }
        });
    }

    @Override
    public String storeName() {
        return PurchaseManagerConfig.STORE_NAME_ANDROID_HUAWEI;
    }

    private int getHuaweiPriceType(OfferType offerType) {
        int type = -1;

// priceType: 0: consumable; 1: non-consumable; 2: auto-renewable subscription
        switch (offerType) {
            case CONSUMABLE:
                type = IapClient.PriceType.IN_APP_CONSUMABLE;
                break;
            case ENTITLEMENT:
                type = IapClient.PriceType.IN_APP_NONCONSUMABLE;
                break;
            case SUBSCRIPTION:
                type = IapClient.PriceType.IN_APP_SUBSCRIPTION;
                break;
        }

        return type;
    }

    @Override
    public void install(final PurchaseObserver observer, PurchaseManagerConfig config, boolean autoFetchInformation) {
        this.observer = observer;

        checkIAPStatus(config, autoFetchInformation);
    }

    private void handleInstall(PurchaseManagerConfig config, boolean autoFetchInformation) {
        if (autoFetchInformation) {
            if (config.getOfferCount() > 0) {
                fetchOffers(config);
            } else {
                notifyInstallation();
            }
        } else {
            notifyInstallation();
        }
    }

    private void fetchOffers(PurchaseManagerConfig config) {
        List<String> entitlements = getProductListByType(config, OfferType.ENTITLEMENT);
        List<String> consumables = getProductListByType(config, OfferType.CONSUMABLE);
        List<String> subscriptions = getProductListByType(config, OfferType.SUBSCRIPTION);

        if (!entitlements.isEmpty()) {
            this.hasToFetchEntetliments = true;
            fetchOffersForType(subscriptions, OfferType.ENTITLEMENT);
        }

        if (!consumables.isEmpty()) {
            this.hasToFetchConsumables = true;
            fetchOffersForType(subscriptions, OfferType.CONSUMABLE);
        }

        if (!subscriptions.isEmpty()) {
            this.hasToFetchSubscriptions = true;
            fetchOffersForType(subscriptions, OfferType.SUBSCRIPTION);
        }
    }

    private void fetchOffersForType(List<String> offers, OfferType offerType) {
        ProductInfoReq req = new ProductInfoReq();
        req.setPriceType(getHuaweiPriceType(offerType));
        req.setProductIds(offers);

        fetchOffersByReq(req);
    }

    private void fetchOffersByReq(final ProductInfoReq productInfoReq) {
        Task<ProductInfoResult> task = Iap.getIapClient(activity).obtainProductInfo(productInfoReq);
        task.addOnSuccessListener(new OnSuccessListener<ProductInfoResult>() {
            @Override
            public void onSuccess(ProductInfoResult result) {
                // Obtain the result
                List<ProductInfo> productInfoList = result.getProductInfoList();

                for (int i = 0; i < productInfoList.size(); i++) {
                    ProductInfo productInfo = productInfoList.get(i);
                    productInfoMap.put(productInfo.getProductId(), productInfo);
                }

                notifyInstallationAfterOffersCheck(productInfoReq.getPriceType());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                resetFetchFlagByType(productInfoReq.getPriceType());

                if (e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException) e;
                    iapListener.onIAPError(apiException);
                } else {
                    iapListener.onError(e);
                }

                observer.handleInstallError(e);
            }
        });
    }

    private List<String> getProductListByType(PurchaseManagerConfig config, OfferType offerType) {
        List<String> productIdList = new ArrayList<>();

        for (int i = 0; i < config.getOfferCount(); i++) {
            Offer offer = config.getOffer(i);

            if (offer.getType() == offerType) {
                productIdList.add(offer.getIdentifier());
            }
        }

        return productIdList;
    }

    private void resetFetchFlagByType(int offerType) {
        switch (offerType) {
            case IapClient.PriceType.IN_APP_CONSUMABLE:
                this.hasToFetchConsumables = false;
                break;
            case IapClient.PriceType.IN_APP_NONCONSUMABLE:
                this.hasToFetchEntetliments = false;
                break;
            case IapClient.PriceType.IN_APP_SUBSCRIPTION:
                this.hasToFetchSubscriptions = false;
                break;
        }
    }

    private void notifyInstallationAfterOffersCheck(int offerType) {
        resetFetchFlagByType(offerType);

        if (!this.hasToFetchConsumables && !this.hasToFetchEntetliments && !this.hasToFetchSubscriptions) {
            notifyInstallation();
        }
    }

    private void notifyInstallation() {
        if (!installationComplete) {
            installationComplete = true;
            observer.handleInstall();
        }
    }

    @Override
    public boolean installed() {
        return this.installationComplete;
    }

    @Override
    public void dispose() {
        if (this.observer != null) {
            // remove observer and config as well
            this.observer = null;
        }

        this.installationComplete = false;
    }

    @Override
    public void purchase(String identifier) {
        ProductInfo productInfo = this.productInfoMap.get(identifier);

        if (productInfo != null) {
            Task<PurchaseIntentResult> task = Iap.getIapClient(activity)
                    .createPurchaseIntent(getPurchaseIntentRequest(productInfo));
            task.addOnSuccessListener(new OnSuccessListener<PurchaseIntentResult>() {
                @Override
                public void onSuccess(PurchaseIntentResult result) {
                    // Obtain the payment result.
                    Status status = result.getStatus();
                    if (status.hasResolution()) {
                        iapListener.onPurchaseResult(result);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    if (e instanceof IapApiException) {
                        IapApiException apiException = (IapApiException) e;
                        iapListener.onIAPError(apiException);
                    } else {
                        iapListener.onError(e);
                    }

                    observer.handlePurchaseError(e);
                }
            });
        } else {
            Exception e = new ProductInfoNotFoundException();
            iapListener.onError(e);
            observer.handlePurchaseError(e);
        }
    }

    private PurchaseIntentReq getPurchaseIntentRequest(ProductInfo productInfo) {
        PurchaseIntentReq req = new PurchaseIntentReq();
        req.setProductId(productInfo.getProductId());
        req.setPriceType(productInfo.getPriceType());
        req.setDeveloperPayload("test");

        return req;
    }

    @Override
    public void purchaseRestore() {
        purchaseRestoreByType(IapClient.PriceType.IN_APP_CONSUMABLE);
        purchaseRestoreByType(IapClient.PriceType.IN_APP_NONCONSUMABLE);
        purchaseRestoreByType(IapClient.PriceType.IN_APP_SUBSCRIPTION);
    }

    private void purchaseRestoreByType(int priceType) {
        final OwnedPurchasesReq req = new OwnedPurchasesReq();
        req.setPriceType(priceType);

        Task<OwnedPurchasesResult> task = Iap.getIapClient(activity).obtainOwnedPurchases(req);
        task.addOnSuccessListener(new OnSuccessListener<OwnedPurchasesResult>() {
            @Override
            public void onSuccess(OwnedPurchasesResult result) {
                handleRestoreTransactions(result.getInAppPurchaseDataList());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException) e;
                    iapListener.onIAPError(apiException);
                } else {
                    iapListener.onError(e);
                }

                observer.handleRestoreError(e);
            }
        });
    }

    private void handleRestoreTransactions(List<String> ownedItems) {
        Transaction[] transactions = new Transaction[ownedItems.size()];

        for (int i = 0; i < ownedItems.size(); i++) {
            try {
                String originalData = ownedItems.get(i);
                InAppPurchaseData inAppPurchaseData = new InAppPurchaseData(originalData);
                transactions[i] = getTransactionFromPurchaseData(inAppPurchaseData, originalData);
            } catch (JSONException ex) {
            }

        }

        observer.handleRestore(transactions);
    }

    private Transaction getTransactionFromPurchaseData(InAppPurchaseData inAppPurchaseData, String originalData) {
        Transaction transaction = null;

        if (inAppPurchaseData != null) {
            transaction.setIdentifier(inAppPurchaseData.getProductId());
            transaction.setStoreName(storeName());
            transaction.setPurchaseText("Purchased: " + inAppPurchaseData.getProductId());
            transaction.setOrderId(inAppPurchaseData.getOrderID());
            transaction.setRequestId(inAppPurchaseData.getPurchaseToken());
            transaction.setPurchaseTime(new Date(inAppPurchaseData.getPurchaseTime()));
            transaction.setTransactionData(originalData);
            transaction.setReversalTime(null);
            transaction.setReversalText(null);
        }

        return transaction;
    }

    @Override
    public Information getInformation(String identifier) {
        ProductInfo productInfo = this.productInfoMap.get(identifier);

        if (productInfo != null) {
            return buildInformation(productInfo);
        }

        return null;
    }

    private Information buildInformation(ProductInfo productInfo) {
        String priceString = productInfo.getPrice();
        return Information.newBuilder()
                .localName(productInfo.getProductName())
                .localDescription(productInfo.getProductDesc())
                .localPricing(priceString)
                .priceCurrencyCode(productInfo.getCurrency())
                .priceInCents((int) (productInfo.getMicrosPrice() / 10000))
                .build();
    }

    public void consumeProduct(String inAppPurchaseData) {
        IapClient mClient = Iap.getIapClient(this.activity);
        Task<ConsumeOwnedPurchaseResult> task = mClient.consumeOwnedPurchase(createConsumeOwnedPurchaseReq(inAppPurchaseData));
        task.addOnSuccessListener(new OnSuccessListener<ConsumeOwnedPurchaseResult>() {
            @Override
            public void onSuccess(ConsumeOwnedPurchaseResult result) {
                iapListener.onConsumedResult(result);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException) e;
                    iapListener.onIAPError(apiException);
                } else {
                    iapListener.onError(e);
                }
            }
        });
    }

    private ConsumeOwnedPurchaseReq createConsumeOwnedPurchaseReq(String purchaseData) {
        ConsumeOwnedPurchaseReq req = new ConsumeOwnedPurchaseReq();
        // Parse purchaseToken from InAppPurchaseData in JSON format.

        try {
            InAppPurchaseData inAppPurchaseData = new InAppPurchaseData(purchaseData);
            req.setPurchaseToken(inAppPurchaseData.getPurchaseToken());
        } catch (JSONException e) {

        }

        return req;
    }

    interface IAPListener {
        void onRegionNotSupported();

        void onLoginRequired();

        void onIAPError(IapApiException exception);

        void onError(Exception exception);

        void onPurchaseResult(PurchaseIntentResult result);

        void onConsumedResult(ConsumeOwnedPurchaseResult result);
    }

    class ProductInfoNotFoundException extends Exception {
    }
}

