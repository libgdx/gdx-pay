package com.badlogic.gdx.pay.android.googleplay.billing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.android.vending.billing.IInAppBillingService;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.android.googleplay.GdxPayException;
import com.badlogic.gdx.pay.android.googleplay.OfferObjectMother;
import com.badlogic.gdx.pay.android.googleplay.billing.GoogleInAppBillingService.ConnectionListener;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static com.badlogic.gdx.pay.android.googleplay.AndroidGooglePlayPurchaseManager.PURCHASE_TYPE_IN_APP;
import static com.badlogic.gdx.pay.android.googleplay.GetSkuDetailsResponseBundleObjectMother.skuDetailsResponseResultNetworkError;
import static com.badlogic.gdx.pay.android.googleplay.GetSkuDetailsResponseBundleObjectMother.skuDetailsResponseResultOkProductFullEditionEntitlement;
import static com.badlogic.gdx.pay.android.googleplay.InformationObjectMother.informationFullEditionEntitlement;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class V3GoogleInAppBillingServiceTest {

    @Mock
    Activity activity;

    @Captor
    ArgumentCaptor<ServiceConnection> serviceConnectionArgumentCaptor;

    @Mock
    IInAppBillingService inAppBillingService;

    @Mock
    ConnectionListener connectionListener;

    private V3GoogleInAppBillingService billingService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        billingService = new V3GoogleInAppBillingService(activity) {
            @Override
            protected IInAppBillingService lookupByStubAsInterface(IBinder binder) {
                return inAppBillingService;
            }
        };

        when(activity.getPackageName()).thenReturn("com.gdx.pay.dummy.activity");
    }

    @Test
    public void installShouldStartActivityIntent() throws Exception {

        whenActivityBindReturn(true);

        requestConnect();

        verify(activity).bindService(isA(Intent.class), isA(ServiceConnection.class), eq(Context.BIND_AUTO_CREATE));
    }

    @Test
    public void shouldCallObserverInstallErrorOnActivityBindFailure() throws Exception {
        whenActivityBindThrow(new SecurityException("Not allowed to bind to this service"));

        requestConnect();

        verify(connectionListener).disconnected(isA(GdxPayException.class));
    }

    @Test
    public void shouldCallConnectionListenerFailureWhenActivityBindReturnsFalse() throws Exception {
        whenActivityBindReturn(false);

        requestConnect();

        verify(connectionListener).disconnected(isA(GdxPayException.class));
    }

    @Test
    public void shouldCallConnectSuccessWhenConnectSucceeds() throws Exception {
        activityBindAndConnect();

        verify(connectionListener).connected();
    }

    @Test
    public void shouldReturnSkusWhenResponseIsOk() throws Exception {

        whenBillingServiceGetSkuDetailsReturn(skuDetailsResponseResultOkProductFullEditionEntitlement());

        activityBindAndConnect();

        Offer offer = OfferObjectMother.offerFullEditionEntitlement();

        Map<String, Information> details = billingService.getProductSkuDetails(singletonList(offer.getIdentifier()));

        assertEquals(details, Collections.singletonMap(offer.getIdentifier(), informationFullEditionEntitlement()));
    }

    @Test
    public void shouldThrowExceptionWhenGetSkuDetailsResponseResultIsNetworkError() throws Exception {
        whenBillingServiceGetSkuDetailsReturn(skuDetailsResponseResultNetworkError());

        activityBindAndConnect();

        thrown.expect(GdxPayException.class);

        billingService.getProductSkuDetails(singletonList("TEST"));
    }

    @Test
    public void shouldThrowExceptionOnGetSkuDetailsWhenDisconnected() throws Exception {
        thrown.expect(GdxPayException.class);

        billingService.getProductSkuDetails(singletonList("TEST"));
    }

    private void activityBindAndConnect() {
        ServiceConnection connection = bindAndFetchNewConnection();

        connection.onServiceConnected(null, null);
    }

    private void whenBillingServiceGetSkuDetailsReturn(Bundle skuDetailsResponse) throws android.os.RemoteException {
        when(inAppBillingService.getSkuDetails(
                        eq(V3GoogleInAppBillingService.BILLING_API_VERSION),
                        isA(String.class),
                        eq(PURCHASE_TYPE_IN_APP),
                        isA(Bundle.class))
        ).thenReturn(skuDetailsResponse);
    }

    private ServiceConnection bindAndFetchNewConnection() {
        whenActivityBindReturn(true);

        requestConnect();

        verify(activity).bindService(isA(Intent.class), serviceConnectionArgumentCaptor.capture(), eq(Context.BIND_AUTO_CREATE));

        return serviceConnectionArgumentCaptor.getValue();
    }

    private void whenActivityBindThrow(SecurityException exception) {
        when(activity.bindService(isA(Intent.class), isA(ServiceConnection.class),
                eq(Context.BIND_AUTO_CREATE)))
                .thenThrow(exception);
    }


    private void whenActivityBindReturn(boolean returnValue) {
        when(activity.bindService(isA(Intent.class), isA(ServiceConnection.class), eq(Context.BIND_AUTO_CREATE))).thenReturn(returnValue);
    }

    private void requestConnect() {
        billingService.connect(connectionListener);
    }
}