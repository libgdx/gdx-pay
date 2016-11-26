package com.badlogic.gdx.pay.android.googleplay.billing;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;
import com.badlogic.gdx.pay.android.googleplay.GdxPayException;

import java.util.List;
import java.util.Map;

public interface GoogleInAppBillingService {

    void requestConnect(ConnectionListener callback);

    Map<String, Information> getProductsDetails(List<String> productIds, String productType);

    void startPurchaseRequest(String productId, String type, PurchaseRequestCallback listener);
    void consumePurchase(Transaction transaction, PurchaseObserver observer);

    void cancelTestPurchases();

    void disconnect();

    boolean isListeningForConnections();

    List<Transaction> getPurchases();

    void dispose();

    interface ConnectionListener {
        void connected();

        void disconnected(GdxPayException exception);
    }

    interface PurchaseRequestCallback {
        void purchaseSuccess(Transaction transaction);

        void purchaseError(GdxPayException exception);

        void purchaseCanceled();
    }

}
