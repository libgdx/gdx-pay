package com.badlogic.gdx.pay.android.huawei;

import android.content.Intent;
import android.content.IntentSender;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidEventListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.pay.FetchItemInformationException;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.LoginRequiredException;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.OfferType;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.RegionNotSupportedException;
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
import com.huawei.hms.iap.entity.PurchaseResultInfo;
import com.huawei.hms.support.api.client.Status;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * The purchase manager implementation for Huawei App Gallery (Android) using HMS IAP
 * <p>
 * <a href="https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/iap-service-introduction-v4">Reference docs</a>
 * <p>
 * Created by Francesco Stranieri on 09.05.2020.
 */

public class HuaweiPurchaseManager implements PurchaseManager, AndroidEventListener {
    private final String TAG = "HuaweiPurchaseManager";

    private final int PURCHASE_STATUS_RESULT_CODE = 7265;
    private final int NOT_LOGGED_IN_STATUS_RESULT_CODE = 7264;

    private final AndroidApplication activity;
    private final HuaweiPurchaseManagerConfig huaweiPurchaseManagerConfig = new HuaweiPurchaseManagerConfig();
    private final HuaweiPurchaseManagerFlagWrapper huaweiPurchaseManagerFlagWrapper = new HuaweiPurchaseManagerFlagWrapper();

    public HuaweiPurchaseManager(AndroidApplication activity) {
        this.activity = activity;
        this.activity.addAndroidEventListener(this);
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
                            try {
                                status.startResolutionForResult(activity, NOT_LOGGED_IN_STATUS_RESULT_CODE);
                            } catch (IntentSender.SendIntentException ex) {
                                huaweiPurchaseManagerConfig.observer.handleInstallError(ex);
                            }
                        } else {
                            huaweiPurchaseManagerConfig.observer.handleInstallError(new LoginRequiredException());
                        }
                    } else if (status.getStatusCode() == OrderStatusCode.ORDER_ACCOUNT_AREA_NOT_SUPPORTED) {
                        // The current region does not support HUAWEI IAP.
                        huaweiPurchaseManagerConfig.observer.handleInstallError(new RegionNotSupportedException());
                    } else {
                        huaweiPurchaseManagerConfig.observer.handleInstallError(e);
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
            fetchOffersForType(entitlements, OfferType.ENTITLEMENT);
        }

        if (!consumables.isEmpty()) {
            this.huaweiPurchaseManagerFlagWrapper.hasToFetchConsumables = true;
            fetchOffersForType(consumables, OfferType.CONSUMABLE);
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
                        try {
                            result.getStatus().startResolutionForResult(activity, PURCHASE_STATUS_RESULT_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            huaweiPurchaseManagerConfig.observer.handlePurchaseError(e);
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    huaweiPurchaseManagerConfig.observer.handlePurchaseError(e);
                }
            });
        } else {
            huaweiPurchaseManagerConfig.observer.handlePurchaseError(new FetchItemInformationException());
        }
    }

    private void handlePurchase(PurchaseResultInfo purchaseResultInfo) {
        Transaction transaction = HuaweiPurchaseManagerUtils.
                getTransactionFromPurchaseData(purchaseResultInfo.getInAppPurchaseData(), purchaseResultInfo.getInAppDataSignature());
        this.huaweiPurchaseManagerConfig.observer.handlePurchase(transaction);
    }

    private void handlePurchaseError(String message, int code) {
        this.huaweiPurchaseManagerConfig.observer.handlePurchaseError(new PurchaseError(message, code));
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
                handleRestoreTransactions(result.getInAppPurchaseDataList(), result.getInAppSignature());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                huaweiPurchaseManagerConfig.observer.handleRestoreError(e);
            }
        });
    }

    private void handleRestoreTransactions(List<String> ownedItems, List<String> signatures) {
        Transaction[] transactions = new Transaction[ownedItems.size()];

        for (int i = 0; i < ownedItems.size(); i++) {
            String originalData = ownedItems.get(i);
            transactions[i] = HuaweiPurchaseManagerUtils.getTransactionFromPurchaseData(originalData, signatures.get(i));
            if (isConsumable(originalData))
                consumeProduct(originalData);
        }

        huaweiPurchaseManagerConfig.observer.handleRestore(transactions);
    }

    private boolean isConsumable(String inAppPurchaseDataString) {
        try {
            InAppPurchaseData inAppPurchaseData = new InAppPurchaseData(inAppPurchaseDataString);
            if (inAppPurchaseData.getKind() == IapClient.PriceType.IN_APP_CONSUMABLE)
                return true;
        } catch( JSONException e ) {
            Gdx.app.log(TAG, "isConsumable - cannot get InAppPurchaseData from JSON", e);
        }
        return false;
    }

    @Override
    public Information getInformation(String identifier) {
        ProductInfo productInfo = this.huaweiPurchaseManagerConfig.productInfoMap.get(identifier);

        if (productInfo != null) {
            return HuaweiPurchaseManagerUtils.buildInformation(productInfo);
        }

        return null;
    }

    private Task<ConsumeOwnedPurchaseResult> consumeProduct(String inAppPurchaseData) {
        IapClient mClient = Iap.getIapClient(this.activity);
        return mClient.consumeOwnedPurchase(HuaweiPurchaseManagerUtils.createConsumeOwnedPurchaseReq(inAppPurchaseData));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PURCHASE_STATUS_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                if (data == null) {
                    Gdx.app.log(TAG, "onActivityResult - data is null");
                    return;
                }

                PurchaseResultInfo purchaseResultInfo = Iap.getIapClient(activity).parsePurchaseResultInfoFromIntent(data);
                switch (purchaseResultInfo.getReturnCode()) {
                    case OrderStatusCode.ORDER_STATE_CANCEL:
                        // User cancel payment.
                        huaweiPurchaseManagerConfig.observer.handlePurchaseCanceled();
                        break;
                    case OrderStatusCode.ORDER_STATE_FAILED:
                    case OrderStatusCode.ORDER_PRODUCT_OWNED:
                        // to check if there exists undelivered products.
                        handlePurchaseError(purchaseResultInfo.getErrMsg(), purchaseResultInfo.getReturnCode());
                        break;
                    case OrderStatusCode.ORDER_STATE_SUCCESS:
                        // pay success.
                        handlePurchase(purchaseResultInfo);

                        String inAppPurchaseDataString = purchaseResultInfo.getInAppPurchaseData();
                        if (isConsumable(inAppPurchaseDataString)) {
                            Task<ConsumeOwnedPurchaseResult> task = consumeProduct(inAppPurchaseDataString);
                            task.addOnSuccessListener(new OnSuccessListener<ConsumeOwnedPurchaseResult>()
                            {
                                @Override
                                public void onSuccess(ConsumeOwnedPurchaseResult result) {
                                    // handlepurchase is done before item is consumed for compatibility with other
                                    // gdx-pay implementations
                                    //TODO what to do if it did not return OK?
                                }
                            } ).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(Exception e) {
                                    huaweiPurchaseManagerConfig.observer.handlePurchaseError(e);
                                }
                            } );
                        }
                        break;
                }
            }
        }
    }
}

