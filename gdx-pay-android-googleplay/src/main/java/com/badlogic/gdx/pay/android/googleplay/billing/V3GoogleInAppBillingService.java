package com.badlogic.gdx.pay.android.googleplay.billing;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.android.googleplay.GdxPayException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import static com.badlogic.gdx.pay.android.googleplay.GetSkuDetailsRequestConverter.convertConfigToItemIdList;
import static com.badlogic.gdx.pay.android.googleplay.GetSkusDetailsResponseBundleToInformationConverter.convertSkuDetailsResponse;

public class V3GoogleInAppBillingService implements GoogleInAppBillingService {

    public static final int BILLING_API_VERSION = 3;

    public static final String PURCHASE_TYPE_IN_APP = "inapp";
    public static final String ERROR_NOT_CONNECTED_TO_GOOGLE_IAB = "Not connected to Google In-app Billing service";

    private ServiceConnection billingServiceConnection;

    @Nullable
    private IInAppBillingService billingService;

    private Activity activity;

    public V3GoogleInAppBillingService(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void connect(ConnectionListener callback) {
        try {
            billingServiceConnection = new BillingServiceInitializingServiceConnection(callback);

            if (!activity.bindService(createBindBillingServiceIntent(), billingServiceConnection, Context.BIND_AUTO_CREATE)) {
                callback.disconnected(new GdxPayException("Failed to bind to service"));
            }
        } catch (Exception e) {
            callback.disconnected(new GdxPayException("Failed to connect", e));
        }
    }

    private Intent createBindBillingServiceIntent() {
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        return serviceIntent;
    }

    @Override
    public Map<String, Information> getProductSkuDetails(List<String> productIds) {
        try {
            return fetchSkuDetails(productIds);
        }
        catch (RuntimeException e) {
            throw new GdxPayException("getProductSkuDetails(" + productIds + " failed)", e);
        }
    }

    private Map<String, Information> fetchSkuDetails(List<String> productIds) {
        Bundle skusRequest = convertConfigToItemIdList(productIds);

        Bundle skuDetailsResponse = executeGetSkuDetails(skusRequest);

        Map<String, Information> informationMap = new HashMap<>();

        informationMap.clear();
        informationMap.putAll(convertSkuDetailsResponse(skuDetailsResponse));

        return informationMap;
    }

    @Override
    public void disconnect() {
        billingService = null;
        unbindIfBound();
    }

    @Override
    public boolean isConnected() {
        return billingService != null;
    }

    private void unbindIfBound() {
        if (billingServiceConnection != null) {
            activity.unbindService(billingServiceConnection);
        }
    }

    private Bundle executeGetSkuDetails(Bundle skusRequest) {
        try {
            String packageName = activity.getPackageName();
            return billingService().getSkuDetails(BILLING_API_VERSION, packageName,
                    PURCHASE_TYPE_IN_APP, skusRequest);
        } catch (RemoteException e) {
            throw new GdxPayException("getProductSkuDetails failed for bundle:" + skusRequest, e);
        }
    }

    private IInAppBillingService billingService() {
        if (!isConnected()) {
            throw new GdxPayException(ERROR_NOT_CONNECTED_TO_GOOGLE_IAB);
        }
        return billingService;
    }

    protected IInAppBillingService lookupByStubAsInterface(IBinder service) {
        return IInAppBillingService.Stub.asInterface(service);
    }

    private class BillingServiceInitializingServiceConnection implements ServiceConnection {
        private ConnectionListener connectionListener;

        public BillingServiceInitializingServiceConnection(ConnectionListener connectionListener) {

            this.connectionListener = connectionListener;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            billingService = lookupByStubAsInterface(service);
            connectionListener.connected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            billingService = null;
            connectionListener.disconnected(new GdxPayException("onServiceDisconnected() received."));
        }
    }
}
