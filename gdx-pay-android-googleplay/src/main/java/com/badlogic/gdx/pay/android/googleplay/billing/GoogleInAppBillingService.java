package com.badlogic.gdx.pay.android.googleplay.billing;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.android.googleplay.GdxPayException;

import java.util.List;
import java.util.Map;

public interface GoogleInAppBillingService {

    void connect(ConnectionListener callback);

    Map<String, Information> getProductSkuDetails(List<String> productIds);

    void disconnect();

    boolean isConnected();

    interface ConnectionListener {
        void connected();

        void disconnected(GdxPayException exception);
    }
}
