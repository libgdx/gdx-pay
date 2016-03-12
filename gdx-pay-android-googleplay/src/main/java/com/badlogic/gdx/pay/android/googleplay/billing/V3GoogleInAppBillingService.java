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
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidEventListener;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;
import com.badlogic.gdx.pay.android.googleplay.ConsumeException;
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
import static com.badlogic.gdx.pay.android.googleplay.billing.converter.GetSkuDetailsRequestConverter.convertProductIdsToItemIdList;
import static com.badlogic.gdx.pay.android.googleplay.billing.converter.GetSkusDetailsResponseBundleConverter.convertSkuDetailsResponse;

public class V3GoogleInAppBillingService implements GoogleInAppBillingService {

    public static final int BILLING_API_VERSION = 3;

    public static final String PURCHASE_TYPE_IN_APP = "inapp";
    public static final String ERROR_NOT_CONNECTED_TO_GOOGLE_IAB = "Not connected to Google In-app Billing service";
    public static final String ERROR_ON_SERVICE_DISCONNECTED_RECEIVED = "onServiceDisconnected() received.";
    public static final String DEFAULT_DEVELOPER_PAYLOAD = "JustRandomStringTooHardToRememberTralala";

    static final String LOG_TAG = "GdxPay/V3GoogleIABS";
    public static final long RETRY_PURCHASE_DELAY_IN_MS = 3000L;

    private ServiceConnection billingServiceConnection;

    @Nullable
    private IInAppBillingService billingService;

    private final ApplicationProxy androidApplication;

    private int activityRequestCode;
    private PurchaseResponseActivityResultConverter purchaseResponseActivityResultConverter;
    private AsyncExecutor asyncExecutor;

    private final String installerPackageName;
    private final V3GoogleInAppBillingServiceAndroidEventListener androidEventListener = new V3GoogleInAppBillingServiceAndroidEventListener();

    private GdxPayAsyncOperationResultListener asyncOperationResultListener;
    private ConnectionListener connectionListener;

    public V3GoogleInAppBillingService(ApplicationProxy proxy,
                                       int activityRequestCode,
                                       PurchaseResponseActivityResultConverter resultConverter,
                                       AsyncExecutor asyncExecutor) {

        this.androidApplication = proxy;
        this.activityRequestCode = activityRequestCode;
        this.purchaseResponseActivityResultConverter = resultConverter;
        this.asyncExecutor = asyncExecutor;
        this.installerPackageName = proxy.getPackageName();
    }

    public V3GoogleInAppBillingService(Activity activity,
                                       AndroidFragmentApplication application,
                                       int activityRequestCode,
                                       PurchaseResponseActivityResultConverter resultConverter,
                                       AsyncExecutor asyncExecutor) {

        this(new ApplicationProxy.FragmentProxy(activity, application),
            activityRequestCode, resultConverter, asyncExecutor);
    }

    public V3GoogleInAppBillingService(AndroidApplication application, int activityRequestCode, PurchaseResponseActivityResultConverter purchaseResponseActivityResultConverter, AsyncExecutor asyncExecutor) {
        this(new ApplicationProxy.ActivityProxy(application),
            activityRequestCode, purchaseResponseActivityResultConverter, asyncExecutor);
    }

    @Override
    public void requestConnect(ConnectionListener connectionListener) {
        if (this.connectionListener != null) {
            throw new IllegalStateException("Already listening for connections.");
        }

        this.connectionListener = connectionListener;
        billingServiceConnection = new BillingServiceInitializingServiceConnection();

        bindBillingServiceConnectionToActivity();
    }

    protected void bindBillingServiceConnectionToActivity() {
        try {
            if (!androidApplication.bindService(createBindBillingServiceIntent(), billingServiceConnection, Context.BIND_AUTO_CREATE)) {
                this.connectionListener.disconnected(new GdxPayException("bindService() returns false."));
            }
        } catch(GdxPayException e) {
            throw e;
        }
        catch (RuntimeException e) {
            this.connectionListener.disconnected(new GdxPayException("requestConnect() failed.", e));
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
        internalStartPurchaseRequest(productId, listener, true);
    }

    @Override
    public void consumePurchase(final Transaction transaction,
                                final PurchaseObserver observer) {

        new Thread(new PurchaseConsumer(transaction, observer)).start();
    }

    private void internalStartPurchaseRequest(String productId, PurchaseRequestCallback listener, boolean retryOnError) {
        PendingIntent pendingIntent;
        try {
            pendingIntent = getBuyIntent(productId);
        } catch (RemoteException |RuntimeException e) {
            if (retryOnError) {
                reconnectToHandleDeadObjectExceptions();
                schedulePurchaseRetry(productId, listener);
                return;
            }

            listener.purchaseError(new GdxPayException("startPurchaseRequest failed at getBuyIntent() for product: " + productId, e));
            return;
        }
        startPurchaseIntentSenderForResult(productId, pendingIntent, listener);
    }

    private void schedulePurchaseRetry(final String productId, final PurchaseRequestCallback listener) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                internalStartPurchaseRequest(productId, listener, false);
            }
        };

        asyncExecutor.executeAsync(runnable, RETRY_PURCHASE_DELAY_IN_MS);
    }

    private void reconnectToHandleDeadObjectExceptions() {
        unbindBillingServiceAndRemoveAndroidEvenetListener();
        bindBillingServiceConnectionToActivity();
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

        return fetchPendingIntentFromGetBuyIntentResponse(intent);
    }

    private PendingIntent fetchPendingIntentFromGetBuyIntentResponse(Bundle responseData) {
        int code = responseData.getInt(RESPONSE_CODE);

        ResponseCode responseCode = ResponseCode.findByCode(code);

        if (responseCode != ResponseCode.BILLING_RESPONSE_RESULT_OK) {
            // TODO: unit test this.
            throw new GdxPayException("Unexpected getBuyIntent() responseCode: " + responseCode + " with response data: " + responseData);
        }

        PendingIntent pendingIntent = responseData.getParcelable(BUY_INTENT);

        if (pendingIntent == null) {
            throw new GdxPayException("Missing value for key: " + BUY_INTENT + "in getBuyIntent() response: "  + responseData);
        }
        return pendingIntent;
    }

    private Map<String, Information> fetchSkuDetails(List<String> productIds) {
        Bundle skusRequest = convertProductIdsToItemIdList(productIds);

        Bundle skuDetailsResponse = executeGetSkuDetails(skusRequest);

        Map<String, Information> informationMap = new HashMap<>();

        informationMap.putAll(convertSkuDetailsResponse(skuDetailsResponse));

        return informationMap;
    }

    @Override
    public void disconnect() {
        billingService = null;
        unbindBillingServiceAndRemoveAndroidEvenetListener();
        connectionListener = null;
    }

    boolean isConnected() {
        return billingService != null;
    }

    @Override
    public boolean isListeningForConnections() {
        return connectionListener != null;
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

    private void unbindBillingServiceAndRemoveAndroidEvenetListener() {
        if (billingServiceConnection != null) {
            try {
                androidApplication.unbindService(billingServiceConnection);
            } catch(Exception e) {
                // Gdx-Pay uses statics. Android reuses JVM instances sometimes.
                // When com.badlogic.gdx.pay.PurchaseSystem.onAppRestarted() unbinds, with
                // an old activity instance from a previous launch, it will run into this Exception.
                Log.e(LOG_TAG, "Unexpected exception in unbindService()", e);
            }
        }
        androidApplication.removeAndroidEventListener(androidEventListener);
    }

    private Bundle executeGetSkuDetails(Bundle skusRequest) {
        try {
            return billingService().getSkuDetails(BILLING_API_VERSION, installerPackageName,
                    PURCHASE_TYPE_IN_APP, skusRequest);
        } catch (RemoteException e) {
            // TODO: unit test this.
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

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(LOG_TAG, "start onServiceConnected(), isConnected() is: " + isConnected());
            if (isConnected()) {
                return;
            }

            billingService = lookupByStubAsInterface(service);

            connectionListener.connected();

            androidApplication.addAndroidEventListener(androidEventListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            unbindBillingServiceAndRemoveAndroidEvenetListener();
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

    int deltaInSeconds(long startTimeInMs) {
        return deltaInSeconds(System.currentTimeMillis(), startTimeInMs);
    }

    int deltaInSeconds(long endTimeMillis, long startTimeMillis) {
        return (int) ((endTimeMillis - startTimeMillis) / 1000l);
    }

    private class PurchaseConsumer implements Runnable {
        private final Transaction transaction;
        private final PurchaseObserver observer;

        public PurchaseConsumer(Transaction transaction, PurchaseObserver observer) {
            this.transaction = transaction;
            this.observer = observer;
        }

        @Override
        public void run() {
            try {
                final int result = consume(transaction.getTransactionData());
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if (result == 0) {
                            observer.handlePurchase(transaction);
                        } else {
                            ResponseCode responseCode = ResponseCode.findByCode(result);
                            String productId = transaction.getIdentifier();
                            String error = "Consuming " + productId + " failed, " + responseCode;
                            observer.handlePurchaseError(new ConsumeException(error, transaction));
                        }
                    }
                });
            } catch (final RemoteException e) {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        String message = "Failed consuming product: " + transaction.getIdentifier();
                        observer.handlePurchaseError(new ConsumeException(message, transaction, e));
                    }
                });
            }
        }

        private int consume(String token) throws RemoteException {
            return billingService.consumePurchase(BILLING_API_VERSION, installerPackageName, token);
        }
    }
}
