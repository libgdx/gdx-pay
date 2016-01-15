package com.badlogic.gdx.pay.android.googleplay.billing;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
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

import static com.badlogic.gdx.pay.android.googleplay.billing.converter.GetSkuDetailsRequestConverter.convertConfigToItemIdList;
import static com.badlogic.gdx.pay.android.googleplay.billing.converter.GetSkusDetailsResponseBundleToInformationConverter.convertSkuDetailsResponse;

public class V3GoogleInAppBillingService implements GoogleInAppBillingService {

    public static final int BILLING_API_VERSION = 3;

    public static final String PURCHASE_TYPE_IN_APP = "inapp";
    public static final String ERROR_NOT_CONNECTED_TO_GOOGLE_IAB = "Not connected to Google In-app Billing service";
    public static final String ERROR_ON_SERVICE_DISCONNECTED_RECEIVED = "onServiceDisconnected() received.";
    public static final String DEFAULT_DEVELOPER_PAYLOAD = "JustRandomStringTooHardToRememberTralala";

    private ServiceConnection billingServiceConnection;

    @Nullable
    private IInAppBillingService billingService;

    private final Activity activity;
    private int activityResultCode;

    private final String installerPackageName;

    public V3GoogleInAppBillingService(Activity activity, int activityResultCode) {
        this.activity = activity;
        this.activityResultCode = activityResultCode;
        installerPackageName = activity.getPackageName();
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

    @Override
    public void startPurchaseRequest(String productId, PurchaseRequestListener listener) {
        PendingIntent pendingIntent = getBuyIntent(productId);

        System.out.println(pendingIntent);

        startPurchaseIntentSenderForResult(productId, pendingIntent);
    }

    protected void startPurchaseIntentSenderForResult(String productId, PendingIntent pendingIntent) {
        try {
            activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
                    activityResultCode, new Intent(), 0, 0, 0);
        } catch(IntentSender.SendIntentException  e) {
            throw new GdxPayException("startIntentSenderForResult failed for product: " + productId, e);
        }
    }

    protected PendingIntent getBuyIntent(String productId) {
        try {
            Bundle intent = billingService().getBuyIntent(BILLING_API_VERSION, installerPackageName, productId, PURCHASE_TYPE_IN_APP, DEFAULT_DEVELOPER_PAYLOAD);

            return intent.getParcelable("BUY_INTENT");
        } catch (RemoteException e) {
            throw new GdxPayException("Failed to get buy intent for product: " + productId, e);
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
            return billingService().getSkuDetails(BILLING_API_VERSION, installerPackageName,
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
            connectionListener.disconnected(new GdxPayException(ERROR_ON_SERVICE_DISCONNECTED_RECEIVED));
        }
    }
}
