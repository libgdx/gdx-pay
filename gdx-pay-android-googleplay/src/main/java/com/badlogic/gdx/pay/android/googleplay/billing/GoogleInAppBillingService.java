package com.badlogic.gdx.pay.android.googleplay.billing;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Transaction;
import com.badlogic.gdx.pay.android.googleplay.GdxPayException;

import java.util.List;
import java.util.Map;

public interface GoogleInAppBillingService {

    void requestConnect(ConnectionListener callback);

    Map<String, Information> getProductsDetails(List<String> productIds);

    void startPurchaseRequest(String productId, PurchaseRequestCallback listener);

    void disconnect();

    boolean isListeningForConnections();

    List<Transaction> getPurchases();

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
