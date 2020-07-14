package com.badlogic.gdx.pay.android.huawei;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.OfferType;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.Transaction;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseReq;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseResult;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.ProductInfo;
import com.huawei.hms.iap.entity.PurchaseIntentReq;

import org.json.JSONException;

import java.util.Date;

class HuaweiPurchaseManagerUtils {

    static PurchaseIntentReq getPurchaseIntentRequest(ProductInfo productInfo) {
        PurchaseIntentReq req = new PurchaseIntentReq();
        req.setProductId(productInfo.getProductId());
        req.setPriceType(productInfo.getPriceType());
        req.setDeveloperPayload("test");

        return req;
    }

    static Transaction getTransactionFromConsumableResult(ConsumeOwnedPurchaseResult result) {
        return getTransactionFromPurchaseData(result.getConsumePurchaseData(), result.getDataSignature());
    }

    static Transaction getTransactionFromPurchaseData(String inAppData, String dataSignature) {
        Transaction transaction = new Transaction();
        transaction.setStoreName(PurchaseManagerConfig.STORE_NAME_ANDROID_HUAWEI);

        try {
            InAppPurchaseData inAppPurchaseData = new InAppPurchaseData(inAppData);
            transaction.setIdentifier(inAppPurchaseData.getProductId());
            transaction.setOrderId(inAppPurchaseData.getOrderID());
            transaction.setRequestId(inAppPurchaseData.getLastOrderId());
            transaction.setPurchaseTime(new Date(inAppPurchaseData.getPurchaseTime()));
            transaction.setPurchaseText("Purchased: " + inAppPurchaseData.getProductName());
            transaction.setPurchaseCost((int) inAppPurchaseData.getPrice());
            transaction.setPurchaseCostCurrency(inAppPurchaseData.getCurrency());

            if (inAppPurchaseData.getPurchaseState() == InAppPurchaseData.PurchaseState.CANCELED) {
                transaction.setReversalTime(new Date(inAppPurchaseData.getCancellationTime()));
            } else {
                transaction.setReversalTime(null);
                transaction.setReversalText(null);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        transaction.setTransactionData(inAppData);
        transaction.setTransactionDataSignature(dataSignature);

        return transaction;
    }

    static Information buildInformation(ProductInfo productInfo) {
        String priceString = productInfo.getPrice();
        return Information.newBuilder()
                .localName(productInfo.getProductName())
                .localDescription(productInfo.getProductDesc())
                .localPricing(priceString)
                .priceCurrencyCode(productInfo.getCurrency())
                .priceAsDouble(productInfo.getMicrosPrice() / 1_000_000.0)
                .build();
    }

    static ConsumeOwnedPurchaseReq createConsumeOwnedPurchaseReq(String purchaseData) {
        ConsumeOwnedPurchaseReq req = new ConsumeOwnedPurchaseReq();
        // Parse purchaseToken from InAppPurchaseData in JSON format.

        try {
            InAppPurchaseData inAppPurchaseData = new InAppPurchaseData(purchaseData);
            req.setPurchaseToken(inAppPurchaseData.getPurchaseToken());
        } catch (JSONException e) {

        }

        return req;
    }

    static int getHuaweiPriceType(OfferType offerType) {
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
}
