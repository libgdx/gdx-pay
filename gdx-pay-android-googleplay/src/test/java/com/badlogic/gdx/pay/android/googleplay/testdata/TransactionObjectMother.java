package com.badlogic.gdx.pay.android.googleplay.testdata;

import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.Transaction;

import java.util.Date;

public class TransactionObjectMother {

    public static Transaction transactionFullEditionEuroGooglePlay() {
        Transaction transaction = new Transaction();

        transaction.setPurchaseCostCurrency("EUR");
        transaction.setPurchaseCost(100);
        transaction.setStoreName(PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE);
        transaction.setPurchaseTime(new Date());
        transaction.setIdentifier("com.appname.full.edition.2015");
        transaction.setOrderId("GPA.1234-5678-9012-34567");
        return transaction;
    }
}