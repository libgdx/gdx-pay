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
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseResult;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.IsEnvReadyResult;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.OwnedPurchasesReq;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.iap.entity.ProductInfo;
import com.huawei.hms.iap.entity.ProductInfoReq;
import com.huawei.hms.iap.entity.ProductInfoResult;
import com.huawei.hms.iap.entity.PurchaseIntentResult;
import com.huawei.hms.support.api.client.Status;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * The purchase manager implementation for Huawei App Gallery (Android) using HMS IAP
 * <p>
 * <a href="https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/iap-service-introduction-v4">Reference docs</a>
 * <p>
 * Created by Francesco Stranieri on 09.05.2020.
 */

public class HuaweiPurchaseManager implements PurchaseManager {

    private final Activity activity;
    private final HuaweiPurchaseManagerConfig huaweiPurchaseManagerConfig = new HuaweiPurchaseManagerConfig();
    private final HuaweiPurchaseManagerFlagWrapper huaweiPurchaseManagerFlagWrapper = new HuaweiPurchaseManagerFlagWrapper();

    public HuaweiPurchaseManager(Activity activity, IAPListener iapListener) {
        this.activity = activity;
        this.huaweiPurchaseManagerConfig.iapListener = iapListener;
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
                            huaweiPurchaseManagerConfig.iapListener.onLoginRequired();
                        } else {
                            huaweiPurchaseManagerConfig.iapListener.onIAPError(apiException);
                        }
                    } else if (status.getStatusCode() == OrderStatusCode.ORDER_ACCOUNT_AREA_NOT_SUPPORTED) {
                        // The current region does not support HUAWEI IAP.
                        huaweiPurchaseManagerConfig.iapListener.onRegionNotSupported();
                    } else {
                        huaweiPurchaseManagerConfig.iapListener.onError(e);
                    }
                }
            }
        });
    }

    @Override
    public String storeName() {
        return PurchaseManagerConfig.STORE_NAME_ANDROID_HUAWEI;
    }

    @Override
    public void install(final PurchaseObserver observer, PurchaseManagerConfig config, boolean autoFetchInformation) {
        this.huaweiPurchaseManagerConfig.observer = observer;

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
            this.huaweiPurchaseManagerFlagWrapper.hasToFetchEntetliments = true;
            fetchOffersForType(subscriptions, OfferType.ENTITLEMENT);
        }

        if (!consumables.isEmpty()) {
            this.huaweiPurchaseManagerFlagWrapper.hasToFetchConsumables = true;
            fetchOffersForType(subscriptions, OfferType.CONSUMABLE);
        }

        if (!subscriptions.isEmpty()) {
            this.huaweiPurchaseManagerFlagWrapper.hasToFetchSubscriptions = true;
            fetchOffersForType(subscriptions, OfferType.SUBSCRIPTION);
        }
    }

    private void fetchOffersForType(List<String> offers, OfferType offerType) {
        ProductInfoReq req = new ProductInfoReq();
        req.setPriceType(HuaweiPurchaseManagerUtils.getHuaweiPriceType(offerType));
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
                    huaweiPurchaseManagerConfig.productInfoMap.put(productInfo.getProductId(), productInfo);
                }

                notifyInstallationAfterOffersCheck(productInfoReq.getPriceType());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                huaweiPurchaseManagerFlagWrapper.resetFetchFlagByType(productInfoReq.getPriceType());

                if (e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException) e;
                    huaweiPurchaseManagerConfig.iapListener.onIAPError(apiException);
                } else {
                    huaweiPurchaseManagerConfig.iapListener.onError(e);
                }

                huaweiPurchaseManagerConfig.observer.handleInstallError(e);
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

    private void notifyInstallationAfterOffersCheck(int offerType) {
        this.huaweiPurchaseManagerFlagWrapper.resetFetchFlagByType(offerType);

        if (this.huaweiPurchaseManagerFlagWrapper.hasFetchedAllProducts()) {
            notifyInstallation();
        }
    }

    private void notifyInstallation() {
        if (!this.huaweiPurchaseManagerConfig.installationComplete) {
            this.huaweiPurchaseManagerConfig.installationComplete = true;
            this.huaweiPurchaseManagerConfig.observer.handleInstall();
        }
    }

    @Override
    public boolean installed() {
        return this.huaweiPurchaseManagerConfig.installationComplete;
    }

    @Override
    public void dispose() {
        if (this.huaweiPurchaseManagerConfig.observer != null) {
            // remove observer and config as well
            this.huaweiPurchaseManagerConfig.observer = null;
        }

        this.huaweiPurchaseManagerConfig.installationComplete = false;
    }

    @Override
    public void purchase(String identifier) {
        ProductInfo productInfo = this.huaweiPurchaseManagerConfig.productInfoMap.get(identifier);

        if (productInfo != null) {
            Task<PurchaseIntentResult> task = Iap.getIapClient(activity)
                    .createPurchaseIntent(HuaweiPurchaseManagerUtils.getPurchaseIntentRequest(productInfo));
            task.addOnSuccessListener(new OnSuccessListener<PurchaseIntentResult>() {
                @Override
                public void onSuccess(PurchaseIntentResult result) {
                    // Obtain the payment result.
                    Status status = result.getStatus();
                    if (status.hasResolution()) {
                        huaweiPurchaseManagerConfig.iapListener.onPurchaseResult(result);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    if (e instanceof IapApiException) {
                        IapApiException apiException = (IapApiException) e;
                        huaweiPurchaseManagerConfig.iapListener.onIAPError(apiException);
                    } else {
                        huaweiPurchaseManagerConfig.iapListener.onError(e);
                    }

                    huaweiPurchaseManagerConfig.observer.handlePurchaseError(e);
                }
            });
        } else {
            Exception e = new ProductInfoNotFoundException();
            huaweiPurchaseManagerConfig.iapListener.onError(e);
            huaweiPurchaseManagerConfig.observer.handlePurchaseError(e);
        }
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
                    huaweiPurchaseManagerConfig.iapListener.onIAPError(apiException);
                } else {
                    huaweiPurchaseManagerConfig.iapListener.onError(e);
                }

                huaweiPurchaseManagerConfig.observer.handleRestoreError(e);
            }
        });
    }

    private void handleRestoreTransactions(List<String> ownedItems) {
        Transaction[] transactions = new Transaction[ownedItems.size()];

        for (int i = 0; i < ownedItems.size(); i++) {
            try {
                String originalData = ownedItems.get(i);
                InAppPurchaseData inAppPurchaseData = new InAppPurchaseData(originalData);
                transactions[i] = HuaweiPurchaseManagerUtils.getTransactionFromPurchaseData(inAppPurchaseData, originalData, storeName());
            } catch (JSONException ex) {
            }

        }

        huaweiPurchaseManagerConfig.observer.handleRestore(transactions);
    }

    @Override
    public Information getInformation(String identifier) {
        ProductInfo productInfo = this.huaweiPurchaseManagerConfig.productInfoMap.get(identifier);

        if (productInfo != null) {
            return HuaweiPurchaseManagerUtils.buildInformation(productInfo);
        }

        return null;
    }

    public void consumeProduct(String inAppPurchaseData) {
        IapClient mClient = Iap.getIapClient(this.activity);
        Task<ConsumeOwnedPurchaseResult> task = mClient.consumeOwnedPurchase(HuaweiPurchaseManagerUtils.createConsumeOwnedPurchaseReq(inAppPurchaseData));
        task.addOnSuccessListener(new OnSuccessListener<ConsumeOwnedPurchaseResult>() {
            @Override
            public void onSuccess(ConsumeOwnedPurchaseResult result) {
                huaweiPurchaseManagerConfig.iapListener.onConsumedResult(result);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException) e;
                    huaweiPurchaseManagerConfig.iapListener.onIAPError(apiException);
                } else {
                    huaweiPurchaseManagerConfig.iapListener.onError(e);
                }
            }
        });
    }
}

