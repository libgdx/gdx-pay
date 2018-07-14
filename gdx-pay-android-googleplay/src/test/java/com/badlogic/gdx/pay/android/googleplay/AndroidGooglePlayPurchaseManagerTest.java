package com.badlogic.gdx.pay.android.googleplay;

import android.app.Activity;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.pay.GdxPayException;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;
import com.badlogic.gdx.pay.android.googleplay.billing.GoogleInAppBillingService;
import com.badlogic.gdx.pay.android.googleplay.billing.GoogleInAppBillingService.ConnectionListener;
import com.badlogic.gdx.pay.android.googleplay.billing.GoogleInAppBillingService.PurchaseRequestCallback;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Constructor;
import java.util.List;

import static com.badlogic.gdx.pay.android.googleplay.AndroidGooglePlayPurchaseManager.GOOGLE_MARKET_NAME;
import static com.badlogic.gdx.pay.android.googleplay.AndroidGooglePlayPurchaseManager.GOOGLE_PLAY_STORE_NAME;
import static com.badlogic.gdx.pay.android.googleplay.billing.V3GoogleInAppBillingService.PURCHASE_TYPE_IN_APP;
import static com.badlogic.gdx.pay.android.googleplay.testdata.InformationObjectMother.informationFullEditionEntitlement;
import static com.badlogic.gdx.pay.android.googleplay.testdata.OfferObjectMother.offerFullEditionEntitlement;
import static com.badlogic.gdx.pay.android.googleplay.testdata.PurchaseManagerConfigObjectMother.managerConfigGooglePlayOneOfferBuyFullEditionProduct;
import static com.badlogic.gdx.pay.android.googleplay.testdata.PurchaseManagerConfigObjectMother.managerConfigGooglePlayOneOfferSubscriptionProduct;
import static com.badlogic.gdx.pay.android.googleplay.testdata.TestConstants.PACKAGE_NAME_GOOD;
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
    AndroidApplication application;

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
        when(application.getPackageName()).thenReturn(PACKAGE_NAME_GOOD);
    }

    @Test
    public void cancelTestPurchasesDelegatesToBillingService() throws Exception {

        purchaseManager.cancelTestPurchases();

        verify(googleInAppBillingService).cancelTestPurchases();
    }

    @Test
    public void runningOnGooglePlayShouldReturnFalseWhenInstalledViaAmazon() throws Exception {

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = "com.amazon.venezia";

        whenGetInstallerPackageNameReturn(singletonList(packageInfo));

        assertFalse(AndroidGooglePlayPurchaseManager.isRunningViaGooglePlay(application));
    }

    @Test
    public void runningOnGooglePlayShouldReturnTrueWhenInstalledViaGoogleMarket() throws Exception {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = GOOGLE_PLAY_STORE_NAME;

        whenGetInstallerPackageNameReturn(singletonList(packageInfo));

        assertTrue(AndroidGooglePlayPurchaseManager.isRunningViaGooglePlay(application));
    }

    @Test
    public void runningOnGooglePlayShouldReturnTrueWhenInstalledViaGooglePlay() throws Exception {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = GOOGLE_MARKET_NAME;

        whenGetInstallerPackageNameReturn(singletonList(packageInfo));

        assertTrue(AndroidGooglePlayPurchaseManager.isRunningViaGooglePlay(application));
    }

    @Test
    public void shouldNotBeInstalledAfterInstantiation() throws Exception {
        assertFalse(purchaseManager.installed());
    }

    @Test
    public void shouldSupportConstructingViaIap() throws Exception {
        Constructor<AndroidGooglePlayPurchaseManager> constructor =
                AndroidGooglePlayPurchaseManager.class.getConstructor(Activity.class, int.class);
        AndroidGooglePlayPurchaseManager manager = constructor.newInstance(application, 1002);

        assertFalse(manager.installed());
    }

    @Test
    public void requestPurchaseMangerInstallWithSubscriptionProduct() throws Exception {

        purchaseManager.install(purchaseObserver, managerConfigGooglePlayOneOfferSubscriptionProduct(), true);
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
    public void shouldInstallEvenIfPreloadInformationFails() throws Exception {

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

        verify(googleInAppBillingService).dispose();

        when(googleInAppBillingService.isListeningForConnections()).thenReturn(false);

        assertFalse(purchaseManager.installed());
    }

    @Test
    public void getInformationForExistingSkuShouldReturnIt() throws Exception {
        String identifier = offerFullEditionEntitlement().getIdentifier();
        Information expectedInformation = informationFullEditionEntitlement();

        whenGetProductsDetailsReturn(identifier, expectedInformation);

        bindFetchNewConnectionAndInstallPurchaseSystem();

        Information actualInformation = purchaseManager.getInformation(identifier);

        verify(googleInAppBillingService).getProductsDetails(singletonList(identifier), PURCHASE_TYPE_IN_APP);
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

        verify(googleInAppBillingService).startPurchaseRequest(Mockito.eq(productIdentifier), Mockito.eq(PURCHASE_TYPE_IN_APP), purchaseRequestListenerArgumentCaptor.capture());

        return purchaseRequestListenerArgumentCaptor.getValue();
    }

    private void whenGetProductsDetailsReturn(String identifier, Information expectedInformation) {
        when(googleInAppBillingService.getProductsDetails(singletonList(identifier), PURCHASE_TYPE_IN_APP)).
                thenReturn(singletonMap(identifier, expectedInformation));
    }

    private void whenGetInstallerPackageNameReturn(List<PackageInfo> packages) {
        when(application.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getInstalledPackages(Mockito.anyInt())).thenReturn(packages);
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

        when(googleInAppBillingService.isListeningForConnections()).thenReturn(true);
    }

    private void verifyBillingGetSkuDetailsCalled() throws android.os.RemoteException {
        verify(googleInAppBillingService).getProductsDetails(singletonList(offerFullEditionEntitlement().getIdentifier()), PURCHASE_TYPE_IN_APP);
    }

    private void requestPurchaseMangerInstallWithFullEditionOffer() {
        purchaseManager.install(purchaseObserver, managerConfigGooglePlayOneOfferBuyFullEditionProduct(), true);
    }

    private void assertRunAsyncCalledAndReset() {
        assertTrue("Expected runAsync() to be called", runAsyncCalled);
        runAsyncCalled = false;
    }
}