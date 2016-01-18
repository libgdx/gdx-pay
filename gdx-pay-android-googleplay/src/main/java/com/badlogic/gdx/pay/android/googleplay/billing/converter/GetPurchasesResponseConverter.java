package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import android.os.Bundle;

import com.badlogic.gdx.pay.Transaction;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.INAPP_PURCHASE_DATA_LIST;
import static com.badlogic.gdx.pay.android.googleplay.billing.converter.InAppPurchaseDataToTransactionConverter.convertJSONPurchaseToTransaction;
import static com.badlogic.gdx.pay.android.googleplay.billing.converter.ResponseConverters.assertResponseOk;

public class GetPurchasesResponseConverter {

    public static List<Transaction> convertPurchasesResponseToTransactions(Bundle purchasesResponseBundle) {
        assertResponseOk(purchasesResponseBundle);

        ArrayList<String> jsonPurchases = purchasesResponseBundle.getStringArrayList(INAPP_PURCHASE_DATA_LIST);

        return convertPurchasesToTransactions(jsonPurchases);
    }

    private static List<Transaction> convertPurchasesToTransactions(ArrayList<String> jsonPurchases) {

        List<Transaction> transactions = new ArrayList<>();

        for(String jsonPurchase : jsonPurchases) {
            try {
                transactions.add(convertJSONPurchaseToTransaction(jsonPurchase));
                // Do not add price from Information.java: price might have changes since purchase.
            } catch (JSONException e) {
                throw new IllegalArgumentException("JSON operation failed for json: " + jsonPurchase, e);
            }
        }

        return transactions;
    }

}
