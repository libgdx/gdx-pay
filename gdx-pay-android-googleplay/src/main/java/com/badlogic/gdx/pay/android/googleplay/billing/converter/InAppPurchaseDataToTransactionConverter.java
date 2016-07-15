package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.Transaction;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.ORDER_ID;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.PRODUCT_ID;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.PURCHASE_STATE;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.PURCHASE_TIME;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.PURCHASE_TOKEN;
import static com.badlogic.gdx.pay.Transaction.REVERSAL_TEXT_CANCELLED;
import static com.badlogic.gdx.pay.Transaction.REVERSAL_TEXT_REFUNDED;

public class InAppPurchaseDataToTransactionConverter {


    private static final int PURCHASE_STATE_CANCELLED = 1;
    private static final int PURCHASE_STATE_REFUNDED = 2;

    // See http://developer.android.com/google/play/billing/billing_reference.html#purchase-data-table
    public static Transaction convertJSONPurchaseToTransaction(String jsonPurchase) throws JSONException {
        JSONObject object = new JSONObject(jsonPurchase);

        Transaction transaction = new Transaction();
        transaction.setStoreName(PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE);

        if (object.has(PURCHASE_TOKEN)) {
            transaction.setTransactionData(object.getString(PURCHASE_TOKEN));
        }

        if (object.has(ORDER_ID)) {
            transaction.setOrderId(object.getString(ORDER_ID));
        }

        transaction.setIdentifier(object.getString(PRODUCT_ID));
        transaction.setPurchaseTime(new Date(object.getLong(PURCHASE_TIME)));

        if (object.has(PURCHASE_STATE)) {
            fillTransactionForPurchaseState(transaction, object.getInt(PURCHASE_STATE));
        }

        return transaction;
    }

    private static void fillTransactionForPurchaseState(Transaction transaction, int purchaseState) {
        switch (purchaseState) {
            case PURCHASE_STATE_CANCELLED:
                transaction.setReversalTime(new Date());
                transaction.setReversalText(REVERSAL_TEXT_CANCELLED);
                break;
            case PURCHASE_STATE_REFUNDED:
                transaction.setReversalTime(new Date());
                transaction.setReversalText(REVERSAL_TEXT_REFUNDED);
        }
    }
}
