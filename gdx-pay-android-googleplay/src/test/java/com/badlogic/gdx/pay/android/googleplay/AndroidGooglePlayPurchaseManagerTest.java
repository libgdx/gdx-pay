package com.badlogic.gdx.pay.android.googleplay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;

import com.android.vending.billing.IInAppBillingService;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.utils.Logger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.badlogic.gdx.pay.android.googleplay.GetSkuDetailsResponseBundleObjectMother.skuDetailsResponseResultNetworkError;
import static com.badlogic.gdx.pay.android.googleplay.GetSkuDetailsResponseBundleObjectMother.skuDetailsResponseResultOkProductFullEditionEntitlement;
import static com.badlogic.gdx.pay.android.googleplay.AndroidGooglePlayPurchaseManager.PURCHASE_TYPE_IN_APP;
import static com.badlogic.gdx.pay.android.googleplay.InformationObjectMother.informationFullEditionEntitlement;
import static com.badlogic.gdx.pay.android.googleplay.OfferObjectMother.offerFullEditionEntitlement;
import static com.badlogic.gdx.pay.android.googleplay.PurchaseManagerConfigObjectMother.managerConfigGooglePlayOneOfferBuyFullEditionProduct;
import static com.badlogic.gdx.pay.android.googleplay.ResponseCode.BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
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
    ArgumentCaptor<Throwable> throwableArgumentCaptor;

    @Mock
    IInAppBillingService inAppBillingService;


    private AndroidGooglePlayPurchaseManager purchaseManager;

    boolean runAsyncCalled;

    @Mock
    private PackageManager packageManager;

    @Before
    public void setUp() throws Exception {

        runAsyncCalled = false;

        purchaseManager = new AndroidGooglePlayPurchaseManager(activity, 1032) {
            @Override
            protected IInAppBillingService lookupByStubAsInterface(IBinder binder) {
                return inAppBillingService;
            }

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
    public void installShouldStartActivityIntent() throws Exception {

        whenActivityBindReturn(true);

        requestPurchaseMangerInstallWithFullEditionOffer();

        verify(activity).bindService(isA(Intent.class), isA(ServiceConnection.class), eq(Context.BIND_AUTO_CREATE));
    }

    @Test
    public void shouldCallObserverInstallErrorOnActivityBindFailure() throws Exception {
        whenActivityBindThrow(new SecurityException("Not allowed to bind to this service"));

        requestPurchaseMangerInstallWithFullEditionOffer();

        verify(purchaseObserver).handleInstallError(isA(GdxPayInstallFailureException.class));
    }

    @Test
    public void shouldCallObserverInstallErrorWhenActivityBindReturnsFalse() throws Exception {
        whenActivityBindReturn(false);

        requestPurchaseMangerInstallWithFullEditionOffer();

        verify(purchaseObserver).handleInstallError(isA(GdxPayInstallFailureException.class));
    }

    @Test
    public void shouldInstallWhenConnectAndGetSkuDetailsSucceeds() throws Exception {
        bindFetchNewConnectionAndInstallPurchaseSystem();

        assertRunAsyncCalledAndReset();

        verify(purchaseObserver).handleInstall();

        assertTrue(purchaseManager.installed());
    }

    @Test
    public void shouldCallHandleInstallErrorWhenGetSkuDetailsResponseResultIsNetworkError() throws Exception {
        ServiceConnection connection = bindAndFetchNewConnection();

        whenBillingServiceGetSkuDetailsReturn(skuDetailsResponseResultNetworkError());

        connection.onServiceConnected(null, null);

        verifyBillingGetSkuDetailsCalled();

        assertRunAsyncCalledAndReset();

        verify(purchaseObserver).handleInstallError(throwableArgumentCaptor.capture());


        Throwable throwable = throwableArgumentCaptor.getValue();

        assertThat(throwable).isInstanceOf(GdxPayInstallFailureException.class)
                .hasMessageContaining(BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE.getMessage());

        assertFalse(purchaseManager.installed());
    }

    @Test
    public void disposeShouldMarkServiceUninstalled() throws Exception {
        bindFetchNewConnectionAndInstallPurchaseSystem();

        purchaseManager.dispose();

        assertFalse(purchaseManager.installed());
    }

    @Test
    public void getInformationForExistingSkuShouldReturnIt() throws Exception {
        bindFetchNewConnectionAndInstallPurchaseSystem();
        String identifier = offerFullEditionEntitlement().getIdentifier();
        Information expectedInformation = informationFullEditionEntitlement();

        Information actualInformation = purchaseManager.getInformation(identifier);

        assertEquals(expectedInformation, actualInformation);
    }

    @Test
    public void getInformationForNonExistingProductShouldReturnInformationUnavailable() throws Exception {
        bindFetchNewConnectionAndInstallPurchaseSystem();

        Information information = purchaseManager.getInformation("nonExistingIdentifier");

        assertSame(Information.UNAVAILABLE, information);
    }

    private void whenGetInstallerPackageNameReturn(String installerPackageName) {
        when(activity.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getInstallerPackageName(isA(String.class))).thenReturn(installerPackageName);
    }

    private void bindFetchNewConnectionAndInstallPurchaseSystem() throws android.os.RemoteException {
        ServiceConnection connection = bindAndFetchNewConnection();

        whenBillingServiceGetSkuDetailsReturn(skuDetailsResponseResultOkProductFullEditionEntitlement());

        connection.onServiceConnected(null, null);

        verifyBillingGetSkuDetailsCalled();
    }

    private void assertRunAsyncNotCalled() {
        assertFalse("runAsync should not have been called.", runAsyncCalled);
    }

    private ServiceConnection bindAndFetchNewConnection() {
        whenActivityBindReturn(true);

        requestPurchaseMangerInstallWithFullEditionOffer();

        verify(activity).bindService(isA(Intent.class), serviceConnectionArgumentCaptor.capture(), eq(Context.BIND_AUTO_CREATE));

        assertRunAsyncNotCalled();

        return serviceConnectionArgumentCaptor.getValue();
    }

    private void whenBillingServiceGetSkuDetailsReturn(Bundle skuDetailsResponse) throws android.os.RemoteException {
        when(inAppBillingService.getSkuDetails(
                        eq(AndroidGooglePlayPurchaseManager.BILLING_API_VERSION),
                        isA(String.class),
                        eq(PURCHASE_TYPE_IN_APP),
                        isA(Bundle.class))
        ).thenReturn(skuDetailsResponse);
    }

    private void verifyBillingGetSkuDetailsCalled() throws android.os.RemoteException {
        verify(inAppBillingService).getSkuDetails(
                eq(AndroidGooglePlayPurchaseManager.BILLING_API_VERSION),
                isA(String.class),
                eq(PURCHASE_TYPE_IN_APP),
                isA(Bundle.class));
    }

    private void whenActivityBindThrow(SecurityException exception) {
        when(activity.bindService(isA(Intent.class), isA(ServiceConnection.class),
                eq(Context.BIND_AUTO_CREATE)))
                .thenThrow(exception);
    }

    private void whenActivityBindReturn(boolean returnValue) {
        when(activity.bindService(isA(Intent.class), isA(ServiceConnection.class), eq(Context.BIND_AUTO_CREATE))).thenReturn(returnValue);
    }

    private void requestPurchaseMangerInstallWithFullEditionOffer() {
        purchaseManager.install(purchaseObserver, managerConfigGooglePlayOneOfferBuyFullEditionProduct(), false);
    }

    private void assertRunAsyncCalledAndReset() {
        assertTrue("Expected runAsync() to be called", runAsyncCalled);
        runAsyncCalled = false;
    }
}