package com.badlogic.gdx.pay.android.huawei;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.OfferType;
import com.badlogic.gdx.pay.Transaction;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseReq;
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

    static Transaction getTransactionFromPurchaseData(InAppPurchaseData inAppPurchaseData,
                                                      String originalData,
                                                      String storeName) {
        Transaction transaction = null;

        if (inAppPurchaseData != null) {
            transaction = new Transaction();
            transaction.setIdentifier(inAppPurchaseData.getProductId());
            transaction.setStoreName(storeName);
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

    static Information buildInformation(ProductInfo productInfo) {
        String priceString = productInfo.getPrice();
        return Information.newBuilder()
                .localName(productInfo.getProductName())
                .localDescription(productInfo.getProductDesc())
                .localPricing(priceString)
                .priceCurrencyCode(productInfo.getCurrency())
                .priceInCents((int) (productInfo.getMicrosPrice() / 10000))
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
