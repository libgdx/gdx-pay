package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import android.content.Intent;

import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.Transaction;
import com.badlogic.gdx.pay.android.googleplay.GdxPayException;
import com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants;
import com.badlogic.gdx.pay.android.googleplay.billing.SkuDetails;
import com.badlogic.gdx.pay.android.googleplay.billing.SkuDetailsFinder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class PurchaseResponseActivityResultConverter {


    public static final String RESPONSE_DATA_PRODUCT_ID = "productId";
    public static final String RESPONSE_DATA_ORDER_ID = "orderId";
    public static final String RESPONSE_DATA_PURCHASE_TIME = "purchaseTime";

    public static Transaction convertToTransaction(Intent responseData, SkuDetailsFinder skuDetailsFinder) {
        String purchaseDataString = responseData.getStringExtra(GoogleBillingConstants.INAPP_PURCHASE_DATA);
        try {
            Transaction transaction = new Transaction();
            transaction.setStoreName(PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE);

            JSONObject jsonObject =  new JSONObject(purchaseDataString);

            String productId = jsonObject.getString(RESPONSE_DATA_PRODUCT_ID);
            setSkuDetailsFields(skuDetailsFinder, transaction, productId);

            transaction.setIdentifier(productId);

            transaction.setPurchaseTime(new Date(jsonObject.getInt(RESPONSE_DATA_PURCHASE_TIME)));

            transaction.setOrderId(jsonObject.getString(RESPONSE_DATA_ORDER_ID));

            return transaction;
        } catch (JSONException e) {
            throw new GdxPayException("JSON Exception while parsing: " + purchaseDataString);
        }
    }

    protected static void setSkuDetailsFields(SkuDetailsFinder skuDetailsFinder, Transaction transaction, String productId) {
        SkuDetails skuDetails = skuDetailsFinder.getSkuDetails(productId);
        transaction.setPurchaseCost((int) skuDetails.getPriceAmountCents());
        transaction.setPurchaseCostCurrency(skuDetails.getPriceCurrencyCode());
    }

}
