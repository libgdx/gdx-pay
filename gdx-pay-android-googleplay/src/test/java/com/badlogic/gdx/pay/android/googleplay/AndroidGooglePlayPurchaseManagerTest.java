package com.badlogic.gdx.pay.android.googleplay;

import android.app.Activity;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;
import com.badlogic.gdx.pay.android.googleplay.billing.GoogleInAppBillingService;
import com.badlogic.gdx.pay.android.googleplay.billing.GoogleInAppBillingService.ConnectionListener;
import com.badlogic.gdx.pay.android.googleplay.billing.GoogleInAppBillingService.PurchaseRequestCallback;
import com.badlogic.gdx.utils.Logger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static com.badlogic.gdx.pay.android.googleplay.testdata.InformationObjectMother.informationFullEditionEntitlement;
import static com.badlogic.gdx.pay.android.googleplay.testdata.OfferObjectMother.offerFullEditionEntitlement;
import static com.badlogic.gdx.pay.android.googleplay.testdata.PurchaseManagerConfigObjectMother.managerConfigGooglePlayOneOfferBuyFullEditionProduct;
import static com.badlogic.gdx.pay.android.googleplay.testdata.TransactionObjectMother.transactionFullEditionEuroGooglePlay;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AndroidGooglePlayPurchaseManagerTest {

    @Mock
    Activity activity;

    @Mock
    Logger logger;

    @Mock
    PurchaseObserver purchaseObserver;

    @Captor
    ArgumentCaptor<ServiceConnection> serviceConnectionArgumentCaptor;

    @Captor
    ArgumentCaptor<ConnectionListener> connectResultListenerArgumentCaptor;

    @Captor
    ArgumentCaptor<Throwable> throwableArgumentCaptor;

    @Captor
    ArgumentCaptor<PurchaseRequestCallback> purchaseRequestListenerArgumentCaptor;

    @Mock
    GoogleInAppBillingService googleInAppBillingService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private AndroidGooglePlayPurchaseManager purchaseManager;


    boolean runAsyncCalled;

    @Mock
    private PackageManager packageManager;

    @Before
    public void setUp() throws Exception {

        runAsyncCalled = false;

        purchaseManager = new AndroidGooglePlayPurchaseManager(this.googleInAppBillingService) {
            @Override
            protected void runAsync(Runnable runnable) {
                runnable.run();
                runAsyncCalled = true;
            }
        };
        purchaseManager.logger = logger;

        when(activity.getPackageName()).thenReturn("com.gdx.pay.dummy.activity");
    }

    @Test
    public void runningOnGooglePlayShouldReturnFalseWhenInstalledViaAmazon() throws Exception {

        whenGetInstallerPackageNameReturn("com.amazon.venezia");

        assertFalse(AndroidGooglePlayPurchaseManager.isRunningViaGooglePlay(activity));
    }

    @Test
    public void runningOnGooglePlayShouldReturnTrueWhenInstalledViaGooglePlay() throws Exception {
        whenGetInstallerPackageNameReturn("com.android.vending");
        assertTrue(AndroidGooglePlayPurchaseManager.isRunningViaGooglePlay(activity));
    }

    @Test
    public void shouldNotBeInstalledAfterInstantiation() throws Exception {
        assertFalse(purchaseManager.installed());
    }


    @Test
    public void shouldCallObserverInstallErrorOnConnectFailure() throws Exception {
        requestPurchaseMangerInstallWithFullEditionOffer();

        verify(googleInAppBillingService).requestConnect(connectResultListenerArgumentCaptor.capture());

        connectResultListenerArgumentCaptor.getValue()
                .disconnected(new GdxPayException("Disconnected", new SecurityException("Test")));


        verify(purchaseObserver).handleInstallError(isA(GdxPayException.class));
    }

    @Test
    public void shouldInstallWhenConnectAndGetSkuDetailsSucceeds() throws Exception {
        bindFetchNewConnectionAndInstallPurchaseSystem();

        assertRunAsyncCalledAndReset();

        verify(purchaseObserver).handleInstall();

        assertTrue(purchaseManager.installed());
    }

    @Test
    public void shouldInstallEvenIfPreloadingInformationFails() throws Exception {

        connectToBillingService();

        verifyBillingGetSkuDetailsCalled();

        assertRunAsyncCalledAndReset();

        verify(purchaseObserver).handleInstall();

        assertTrue(purchaseManager.installed());
    }

    @Test
    public void disposeShouldMarkServiceUninstalled() throws Exception {
        bindFetchNewConnectionAndInstallPurchaseSystem();

        purchaseManager.dispose();

        verify(googleInAppBillingService).disconnect();

        when(googleInAppBillingService.isConnected()).thenReturn(false);

        assertFalse(purchaseManager.installed());
    }

    @Test
    public void getInformationForExistingSkuShouldReturnIt() throws Exception {
        String identifier = offerFullEditionEntitlement().getIdentifier();
        Information expectedInformation = informationFullEditionEntitlement();

        whenGetProductsDetailsReturn(identifier, expectedInformation);

        bindFetchNewConnectionAndInstallPurchaseSystem();

        Information actualInformation = purchaseManager.getInformation(identifier);

        verify(googleInAppBillingService).getProductsDetails(singletonList(identifier));
        assertEquals(expectedInformation, actualInformation);
    }

    @Test
    public void getInformationForNonExistingProductShouldReturnInformationUnavailable() throws Exception {
        bindFetchNewConnectionAndInstallPurchaseSystem();

        Information information = purchaseManager.getInformation("nonExistingIdentifier");

        assertSame(Information.UNAVAILABLE, information);
    }

    @Test
    public void purchaseProductWhenNotInstalledThrowsException() {
        thrown.expect(GdxPayException.class);
        purchaseManager.purchase("PRODUCT");
    }

    @Test
    public void purchaseSuccessShouldDelegateResultSuccessToObserver() throws Exception {
        PurchaseRequestCallback callback = connectBindAndPurchaseRequestForFullEditionEntitlement();

        Transaction transaction = transactionFullEditionEuroGooglePlay();

        callback.purchaseSuccess(transaction);

        verify(purchaseObserver).handlePurchase(transaction);
    }

    @Test
    public void purchaseCanceledShouldDelegateResultToObserver() throws Exception {
        PurchaseRequestCallback callback = connectBindAndPurchaseRequestForFullEditionEntitlement();

        callback.purchaseCanceled();

        verify(purchaseObserver).handlePurchaseCanceled();
    }

    @Test
    public void purchaseErrorShouldDelegateResultErrorToObserver() throws Exception {
        PurchaseRequestCallback callback = connectBindAndPurchaseRequestForFullEditionEntitlement();

        GdxPayException exception = new GdxPayException("Network error");
        callback.purchaseError(exception);

        verify(purchaseObserver).handlePurchaseError(exception);
    }

    @Test
    public void restoreSuccessShouldDelegateResultToObserver() throws Exception {
        connectBindAndForFullEditionEntitlement();

        Transaction transaction = transactionFullEditionEuroGooglePlay();
        Transaction[] transactions = new Transaction[]{transaction};

        when(googleInAppBillingService.getPurchases()).thenReturn(singletonList(transaction));

        purchaseManager.purchaseRestore();

        verify(purchaseObserver).handleRestore(transactions);
    }

    @Test
    public void restoreErrorShouldDelegateResultToObserver() throws Exception {
        connectBindAndForFullEditionEntitlement();

        GdxPayException exception = new GdxPayException("Network error");

        when(this.googleInAppBillingService.getPurchases()).thenThrow(exception);

        purchaseManager.purchaseRestore();

        verify(googleInAppBillingService).getPurchases();

        verify(purchaseObserver).handleRestoreError(exception);
    }


    private void connectBindAndForFullEditionEntitlement() throws android.os.RemoteException {
        Offer offer = offerFullEditionEntitlement();
        Information information = informationFullEditionEntitlement();

        whenGetProductsDetailsReturn(offer.getIdentifier(), information);

        bindFetchNewConnectionAndInstallPurchaseSystem();
    }


    private PurchaseRequestCallback connectBindAndPurchaseRequestForFullEditionEntitlement() throws android.os.RemoteException {
        Offer offer = offerFullEditionEntitlement();
        Information information = informationFullEditionEntitlement();

        whenGetProductsDetailsReturn(offer.getIdentifier(), information);

        bindFetchNewConnectionAndInstallPurchaseSystem();

        String productIdentifier = offerFullEditionEntitlement().getIdentifier();
        purchaseManager.purchase(productIdentifier);

        verify(googleInAppBillingService).startPurchaseRequest(Mockito.eq(productIdentifier), purchaseRequestListenerArgumentCaptor.capture());

        return purchaseRequestListenerArgumentCaptor.getValue();
    }

    private void whenGetProductsDetailsReturn(String identifier, Information expectedInformation) {
        when(googleInAppBillingService.getProductsDetails(singletonList(identifier))).
                thenReturn(singletonMap(identifier, expectedInformation));
    }

    private void whenGetInstallerPackageNameReturn(String installerPackageName) {
        when(activity.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getInstallerPackageName(isA(String.class))).thenReturn(installerPackageName);
    }

    private void bindFetchNewConnectionAndInstallPurchaseSystem() throws android.os.RemoteException {
        connectToBillingService();

        verifyBillingGetSkuDetailsCalled();
    }

    private void assertRunAsyncNotCalled() {
        assertFalse("runAsync should not have been called.", runAsyncCalled);
    }

    private void connectToBillingService() {
        requestPurchaseMangerInstallWithFullEditionOffer();

        verify(googleInAppBillingService).requestConnect(connectResultListenerArgumentCaptor.capture());

        assertRunAsyncNotCalled();

        connectResultListenerArgumentCaptor.getValue().connected();

        when(googleInAppBillingService.isConnected()).thenReturn(true);
    }

    private void verifyBillingGetSkuDetailsCalled() throws android.os.RemoteException {
        verify(googleInAppBillingService).getProductsDetails(singletonList(offerFullEditionEntitlement().getIdentifier()));
    }

    private void requestPurchaseMangerInstallWithFullEditionOffer() {
        purchaseManager.install(purchaseObserver, managerConfigGooglePlayOneOfferBuyFullEditionProduct(), false);
    }

    private void assertRunAsyncCalledAndReset() {
        assertTrue("Expected runAsync() to be called", runAsyncCalled);
        runAsyncCalled = false;
    }
}