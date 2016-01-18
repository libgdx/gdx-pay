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
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidEventListener;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Transaction;
import com.badlogic.gdx.pay.android.googleplay.GdxPayException;
import com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants;
import com.badlogic.gdx.pay.android.googleplay.billing.converter.PurchaseResponseActivityResultConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import static com.badlogic.gdx.pay.android.googleplay.billing.converter.GetSkuDetailsRequestConverter.convertConfigToItemIdList;
import static com.badlogic.gdx.pay.android.googleplay.billing.converter.GetSkusDetailsResponseBundleConverter.convertSkuDetailsResponse;

public class V3GoogleInAppBillingService implements GoogleInAppBillingService {

    public static final int BILLING_API_VERSION = 3;

    public static final String PURCHASE_TYPE_IN_APP = "inapp";
    public static final String ERROR_NOT_CONNECTED_TO_GOOGLE_IAB = "Not connected to Google In-app Billing service";
    public static final String ERROR_ON_SERVICE_DISCONNECTED_RECEIVED = "onServiceDisconnected() received.";
    public static final String DEFAULT_DEVELOPER_PAYLOAD = "JustRandomStringTooHardToRememberTralala";

    private ServiceConnection billingServiceConnection;

    @Nullable
    private IInAppBillingService billingService;


    private final AndroidApplication androidApplication;

    private int activityRequestCode;
    private PurchaseResponseActivityResultConverter purchaseResponseActivityResultConverter;

    private final String installerPackageName;
    private final V3GoogleInAppBillingServiceAndroidEventListener androidEventListener = new V3GoogleInAppBillingServiceAndroidEventListener();

    private GdxPayAsyncOperationResultListener asyncOperationResultListener;


    public V3GoogleInAppBillingService(AndroidApplication application, int activityRequestCode, PurchaseResponseActivityResultConverter purchaseResponseActivityResultConverter) {
        this.androidApplication = application;
        this.activityRequestCode = activityRequestCode;
        this.purchaseResponseActivityResultConverter = purchaseResponseActivityResultConverter;
        installerPackageName = application.getPackageName();
    }

    // TODO: implement handling of reconnects.
    @Override
    public void requestConnect(ConnectionListener callback) {
        try {
            billingServiceConnection = new BillingServiceInitializingServiceConnection(callback);

            if (!androidApplication.bindService(createBindBillingServiceIntent(), billingServiceConnection, Context.BIND_AUTO_CREATE)) {
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
    public Map<String, Information> getProductsDetails(List<String> productIds) {
        try {
            return fetchSkuDetails(productIds);
        } catch (RuntimeException e) {
            throw new GdxPayException("getProductsDetails(" + productIds + " failed)", e);
        }
    }

    @Override
    public void startPurchaseRequest(String productId, PurchaseRequestCallback listener) {
        PendingIntent pendingIntent = getBuyIntent(productId);
        startPurchaseIntentSenderForResult(productId, pendingIntent, listener);
    }

    private void startPurchaseIntentSenderForResult(String productId, PendingIntent pendingIntent, final PurchaseRequestCallback listener) {
        try {
            androidApplication.startIntentSenderForResult(pendingIntent.getIntentSender(),
                    activityRequestCode, new Intent(), 0, 0, 0);

            listenForAppBillingActivityEventOnce(new GdxPayAsyncOperationResultListener() {
                @Override
                public void onEvent(int resultCode, Intent data) {

                    if (resultCode == Activity.RESULT_OK) {
                        handleResultOk(data);
                        return;
                    }

                    if (resultCode == Activity.RESULT_CANCELED) {
                        listener.purchaseCanceled();
                        return;
                    }

                    listener.purchaseError(new GdxPayException("Unexpected resultCode:" + resultCode + "with data:" + data));
                }

                protected void handleResultOk(Intent data) {
                    final Transaction transaction;
                    try {
                        transaction = convertPurchaseResponseDataToTransaction(data);
                    } catch (GdxPayException e) {
                        listener.purchaseError(new GdxPayException("Error converting purchase success response: " + data, e));
                        return;
                    }

                    listener.purchaseSuccess(transaction);
                }
            });
        } catch (IntentSender.SendIntentException e) {
            listener.purchaseError(new GdxPayException("startIntentSenderForResult failed for product: " + productId, e));
        }
    }

    private Transaction convertPurchaseResponseDataToTransaction(Intent responseIntentData) {
        return purchaseResponseActivityResultConverter.convertToTransaction(responseIntentData);
    }

    private void listenForAppBillingActivityEventOnce(GdxPayAsyncOperationResultListener gdxPayAsyncListener) {
        asyncOperationResultListener = gdxPayAsyncListener;
    }

    private PendingIntent getBuyIntent(String productId) {
        try {
            Bundle intent = billingService().getBuyIntent(BILLING_API_VERSION, installerPackageName, productId, PURCHASE_TYPE_IN_APP, DEFAULT_DEVELOPER_PAYLOAD);

            return intent.getParcelable(GoogleBillingConstants.BUY_INTENT);
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
        disconnectFromActivity();
    }

    @Override
    public boolean isConnected() {
        return billingService != null;
    }

    @Override
    public void startPurchaseRestoreRequest(PurchaseRestoreRequestCallback callback) {
        throw new UnsupportedOperationException();
    }

    private void disconnectFromActivity() {
        if (billingServiceConnection != null) {
            androidApplication.unbindService(billingServiceConnection);
        }
        androidApplication.removeAndroidEventListener(androidEventListener);
    }

    private Bundle executeGetSkuDetails(Bundle skusRequest) {
        try {
            return billingService().getSkuDetails(BILLING_API_VERSION, installerPackageName,
                    PURCHASE_TYPE_IN_APP, skusRequest);
        } catch (RemoteException e) {
            throw new GdxPayException("getProductsDetails failed for bundle:" + skusRequest, e);
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

            androidApplication.addAndroidEventListener(androidEventListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            billingService = null;
            androidApplication.removeAndroidEventListener(androidEventListener);
            connectionListener.disconnected(new GdxPayException(ERROR_ON_SERVICE_DISCONNECTED_RECEIVED));
        }
    }

    private void onGdxPayActivityEvent(int resultCode, Intent data) {
        if (this.asyncOperationResultListener != null) {
            asyncOperationResultListener.onEvent(resultCode, data);
            asyncOperationResultListener = null;
        }
    }

    private final class V3GoogleInAppBillingServiceAndroidEventListener implements AndroidEventListener {

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (activityRequestCode == requestCode) {
                onGdxPayActivityEvent(resultCode, data);
            }
        }
    }

    private interface GdxPayAsyncOperationResultListener {
        void onEvent(int resultCode, Intent data);
    }
}
