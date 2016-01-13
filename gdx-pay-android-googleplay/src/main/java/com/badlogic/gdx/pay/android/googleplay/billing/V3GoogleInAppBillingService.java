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

import static com.badlogic.gdx.pay.android.googleplay.GetSkuDetailsRequestConverter.convertConfigToItemIdList;
import static com.badlogic.gdx.pay.android.googleplay.GetSkusDetailsResponseBundleToInformationConverter.convertSkuDetailsResponse;

public class V3GoogleInAppBillingService implements GoogleInAppBillingService {

    public static final int BILLING_API_VERSION = 3;

    public static final String PURCHASE_TYPE_IN_APP = "inapp";

    private ServiceConnection billingServiceConnection;

    private IInAppBillingService billingService;
    private Activity activity;

    public V3GoogleInAppBillingService(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void connect(ConnectResultListener callback) {

        try {

            billingServiceConnection = new BillingServiceInitializingServiceConnection(callback);

            if (!activity.bindService(createBindBillingServiceIntent(), billingServiceConnection, Context.BIND_AUTO_CREATE)) {
                callback.disconnected(new GdxPayException("Failed to bind to service"));
            }
        } catch(Exception e) {
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
        } catch(RuntimeException e) {
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

    protected Bundle executeGetSkuDetails(Bundle skusRequest)  {
        try {
            return billingService.getSkuDetails(BILLING_API_VERSION,
                    activity.getPackageName(), PURCHASE_TYPE_IN_APP,
                    skusRequest);
        } catch (RemoteException e) {
            throw new GdxPayException("getProductSkuDetails failed for bundle:" + skusRequest, e);
        }
    }

    protected IInAppBillingService lookupByStubAsInterface(IBinder service) {
        return IInAppBillingService.Stub.asInterface(service);
    }

    private class BillingServiceInitializingServiceConnection implements ServiceConnection {
        private ConnectResultListener connectResultListener;

        public BillingServiceInitializingServiceConnection(ConnectResultListener connectResultListener) {

            this.connectResultListener = connectResultListener;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            billingService = lookupByStubAsInterface(service);
            connectResultListener.connected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            billingService = null;
            connectResultListener.disconnected(new GdxPayException("onServiceDisconnected() received."));
        }
    }
}
