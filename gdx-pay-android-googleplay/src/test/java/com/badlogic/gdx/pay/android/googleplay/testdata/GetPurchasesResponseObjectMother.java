package com.badlogic.gdx.pay.android.googleplay.testdata;

import android.os.Bundle;

import com.badlogic.gdx.pay.Transaction;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.INAPP_PURCHASE_DATA_LIST;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.ORDER_ID;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.PACKAGE_NAME;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.PRODUCT_ID;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.PURCHASE_TIME;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.PURCHASE_TOKEN;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.RESPONSE_CODE;
import static com.badlogic.gdx.pay.android.googleplay.ResponseCode.BILLING_RESPONSE_RESULT_OK;
import static com.badlogic.gdx.pay.android.googleplay.testdata.TestConstants.PACKAGE_NAME_GOOD;
import static com.badlogic.gdx.pay.android.googleplay.testdata.TransactionObjectMother.transactionFullEditionEuroGooglePlay;
import static com.badlogic.gdx.pay.android.googleplay.testdata.TransactionObjectMother.transactionFullEditionEuroGooglePlaySandbox;

public class GetPurchasesResponseObjectMother {

    public static Bundle purchasesResponseOneTransactionFullEdition() {
        Bundle bundle = new Bundle();

        bundle.putInt(RESPONSE_CODE, BILLING_RESPONSE_RESULT_OK.getCode());

        bundle.putStringArrayList(INAPP_PURCHASE_DATA_LIST, makeStringArrayListForTransaction(transactionFullEditionEuroGooglePlay()));

        return bundle;
    }

    public static Bundle purchasesResponseOneTransactionFullEditionSandboxOrder() {
        Bundle bundle = new Bundle();

        bundle.putInt(RESPONSE_CODE, BILLING_RESPONSE_RESULT_OK.getCode());

        bundle.putStringArrayList(INAPP_PURCHASE_DATA_LIST, makeStringArrayListForTransaction(transactionFullEditionEuroGooglePlaySandbox()));

        return bundle;
    }

    private static ArrayList<String> makeStringArrayListForTransaction(Transaction transaction) {
        ArrayList<String> list = new ArrayList<>();

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put(ORDER_ID, transaction.getOrderId());
            jsonObject.put(PACKAGE_NAME, PACKAGE_NAME_GOOD);
            jsonObject.put(ORDER_ID, transaction.getOrderId());
            jsonObject.put(PRODUCT_ID, transaction.getIdentifier());
            jsonObject.put(PURCHASE_TIME, System.currentTimeMillis());
            jsonObject.put(PURCHASE_TOKEN, transaction.getTransactionData());

            list.add(jsonObject.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    public static Bundle purchasesResponseEmptyResponse() {
        Bundle bundle = new Bundle();

        bundle.putInt(RESPONSE_CODE, BILLING_RESPONSE_RESULT_OK.getCode());

        bundle.putStringArrayList(INAPP_PURCHASE_DATA_LIST, new ArrayList<String>());

        return bundle;
    }
}
