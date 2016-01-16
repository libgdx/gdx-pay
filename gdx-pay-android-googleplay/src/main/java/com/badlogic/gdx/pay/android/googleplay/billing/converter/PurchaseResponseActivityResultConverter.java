package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import android.content.Intent;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.Transaction;
import com.badlogic.gdx.pay.android.googleplay.GdxPayException;
import com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.PRODUCT_ID;

public class PurchaseResponseActivityResultConverter {

    private PurchaseManager purchaseManager;

    public PurchaseResponseActivityResultConverter(PurchaseManager purchaseManager) {
        this.purchaseManager = purchaseManager;
    }


    public Transaction convertToTransaction(Intent responseData) {
        String purchaseDataString = responseData.getStringExtra(GoogleBillingConstants.INAPP_PURCHASE_DATA);
        try {
            Transaction transaction = new Transaction();
            transaction.setStoreName(PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE);

            JSONObject jsonObject =  new JSONObject(purchaseDataString);

            String productId = jsonObject.getString(PRODUCT_ID);
            setInformationFields(transaction, productId);

            transaction.setIdentifier(productId);

            transaction.setPurchaseTime(new Date(jsonObject.getInt(GoogleBillingConstants.PURCHASE_TIME)));

            transaction.setOrderId(jsonObject.getString(GoogleBillingConstants.ORDER_ID));

            return transaction;
        } catch (JSONException e) {
            throw new GdxPayException("JSON Exception while parsing: " + purchaseDataString, e);
        }
    }

    protected void setInformationFields(Transaction transaction, String productId) {
        Information information = purchaseManager.getInformation(productId);

        transaction.setPurchaseCost(information.getPriceInCents());
        transaction.setPurchaseCostCurrency(information.getPriceCurrencyCode());
    }

}
