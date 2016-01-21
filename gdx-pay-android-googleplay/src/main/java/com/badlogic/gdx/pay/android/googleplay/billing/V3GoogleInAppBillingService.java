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
import com.badlogic.gdx.pay.android.googleplay.ResponseCode;
import com.badlogic.gdx.pay.android.googleplay.billing.converter.PurchaseResponseActivityResultConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.BUY_INTENT;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.RESPONSE_CODE;
import static com.badlogic.gdx.pay.android.googleplay.billing.converter.GetPurchasesResponseConverter.convertPurchasesResponseToTransactions;
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
                callback.disconnected(new GdxPayException("bindService() returns false."));
            }
        } catch(GdxPayException e) {
            throw e;
        }
        catch (RuntimeException e) {
            callback.disconnected(new GdxPayException("requestConnect() failed.", e));
        }
    }

    private Intent createBindBillingServiceIntent() {
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        return serviceIntent;
    }

    @Override
    public Map<String, Information> getProductsDetails(List<String> productIds) {
        long startTimeInMs = System.currentTimeMillis();
        try {
            return fetchSkuDetails(productIds);
        } catch (RuntimeException e) {
            throw new GdxPayException("getProductsDetails(" + productIds + " failed) after " + deltaInSeconds(startTimeInMs) + " seconds", e);
        }
    }

    @Override
    public void startPurchaseRequest(String productId, PurchaseRequestCallback listener) {
        PendingIntent pendingIntent;
        try {
            pendingIntent = getBuyIntent(productId);
        } catch (RemoteException|RuntimeException e) {
            listener.purchaseError(new GdxPayException("startPurchaseRequest failed at getBuyIntent() for product: " + productId, e));
            return;
        }
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

    private PendingIntent getBuyIntent(String productId) throws RemoteException {
        Bundle intent = billingService().getBuyIntent(BILLING_API_VERSION, installerPackageName, productId, PURCHASE_TYPE_IN_APP, DEFAULT_DEVELOPER_PAYLOAD);

        // TODO unit test this.
        return fetchPendingIntentFromGetBuyIntentResponse(intent);
    }

    private PendingIntent fetchPendingIntentFromGetBuyIntentResponse(Bundle responseData) {
        // TODO: unit test this.
        int code = responseData.getInt(RESPONSE_CODE);

        ResponseCode responseCode = ResponseCode.findByCode(code);

        if (responseCode != ResponseCode.BILLING_RESPONSE_RESULT_OK) {
            throw new GdxPayException("Unexpected getBuyIntent() responseCode: " + responseCode + " with response data: " + responseData);
        }

        PendingIntent pendingIntent = responseData.getParcelable(BUY_INTENT);

        if (pendingIntent == null) {
            throw new GdxPayException("Missing key (or has object) " + BUY_INTENT + "in getBuyIntent() response: "  + responseData);
        }
        return pendingIntent;
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
    public List<Transaction> getPurchases() {
        try {
            Bundle purchases = billingService().getPurchases(BILLING_API_VERSION, installerPackageName, V3GoogleInAppBillingService.PURCHASE_TYPE_IN_APP, null);

            return convertPurchasesResponseToTransactions(purchases);

        } catch (RemoteException | RuntimeException e) { // TODO: unit test RuntimeException scenario, e.g. :  java.lang.IllegalArgumentException: Unexpected response code: ResponseCode{code=3, message='Billing API version is not supported for the type requested'}, response: Bundle[{RESPONSE_CODE=3}]

            throw new GdxPayException("Unexpected exception in getPurchases()", e);
        }
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
            if (isConnected()) {
                return;
            }

            billingService = lookupByStubAsInterface(service);

            connectionListener.connected();

            androidApplication.addAndroidEventListener(androidEventListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            disconnectFromActivity();
            billingService = null;
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

    private long deltaInSeconds(long startTimeInMs) {
        return startTimeInMs - System.currentTimeMillis() / 1000l;
    }

    int deltaInSeconds(long endTimeMillis, long startTimeMillis) {
        return (int) ((endTimeMillis - startTimeMillis) / 1000l);
    }
}
