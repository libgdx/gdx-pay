package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import android.content.Intent;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.Transaction;
import com.badlogic.gdx.pay.android.googleplay.GdxPayException;
import com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants;

import org.json.JSONException;

import static com.badlogic.gdx.pay.android.googleplay.billing.converter.InAppPurchaseDataToTransactionConverter.convertJSONPurchaseToTransaction;

public class PurchaseResponseActivityResultConverter {

    private PurchaseManager purchaseManager;

    public PurchaseResponseActivityResultConverter(PurchaseManager purchaseManager) {
        this.purchaseManager = purchaseManager;
    }

    // TODO: throw GdxPayException only from service boundaries.
    public Transaction convertToTransaction(Intent responseData) {
        String purchaseDataString = responseData.getStringExtra(GoogleBillingConstants.INAPP_PURCHASE_DATA);
        try {
            Transaction transaction = convertJSONPurchaseToTransaction(purchaseDataString);

            String productId = transaction.getIdentifier();

            setInformationFields(transaction, productId);

            return transaction;
        } catch (JSONException e) {
            throw new GdxPayException("JSON Exception while parsing: " + purchaseDataString, e);
        }
    }

    protected void setInformationFields(Transaction transaction, String productId) {
        Information information = purchaseManager.getInformation(productId);

        Integer priceInCents = information.getPriceInCents();
        transaction.setPurchaseCost(priceInCents == null ? null : priceInCents);
        transaction.setPurchaseCostCurrency(information.getPriceCurrencyCode());
    }

}
