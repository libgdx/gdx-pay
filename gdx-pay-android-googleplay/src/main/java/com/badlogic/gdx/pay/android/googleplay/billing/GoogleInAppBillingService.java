package com.badlogic.gdx.pay.android.googleplay.billing;

import com.badlogic.gdx.pay.Information;

import java.util.List;
import java.util.Map;

public interface GoogleInAppBillingService {

    void connect(ConnectResultListener callback);

    Map<String, Information> getProductSkuDetails(List<String> productIds);

    void disconnect();

    boolean isConnected();

    interface ConnectResultListener {
        void connected();

        void disconnected(Exception exception);
    }
}
