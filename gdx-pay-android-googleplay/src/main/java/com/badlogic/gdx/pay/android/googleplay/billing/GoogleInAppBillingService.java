package com.badlogic.gdx.pay.android.googleplay.billing;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Transaction;
import com.badlogic.gdx.pay.android.googleplay.GdxPayException;

import java.util.List;
import java.util.Map;

public interface GoogleInAppBillingService {

    void requestConnect(ConnectionListener callback);

    Map<String, Information> getProductSkuDetails(List<String> productIds);

    void startPurchaseRequest(String productId, PurchaseRequestListener listener);

    void disconnect();

    boolean isConnected();

    interface ConnectionListener {
        void connected();

        void disconnected(GdxPayException exception);
    }

    interface PurchaseRequestListener {
        void purchaseSuccess(Transaction transaction);

        void purchaseError(GdxPayException exception);

        void purchaseCancelled();
    }
}
