package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.Transaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.badlogic.gdx.pay.android.googleplay.testdata.InformationObjectMother.informationFullEditionEntitlementNoPriceInCents;
import static com.badlogic.gdx.pay.android.googleplay.testdata.ProductIdentifierObjectMother.PRODUCT_IDENTIFIER_FULL_EDITION;
import static com.badlogic.gdx.pay.android.googleplay.testdata.PurchaseRequestActivityResultObjectMother.INAPP_DATA_SIGNATURE_ACTIVITY_RESULT_SUCCESS;
import static com.badlogic.gdx.pay.android.googleplay.testdata.PurchaseRequestActivityResultObjectMother.activityResultPurchaseFullEditionSuccess;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PurchaseResponseActivityResultConverterTest {

    @Mock
    private PurchaseManager purchaseManager;
    private PurchaseResponseActivityResultConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = new PurchaseResponseActivityResultConverter(purchaseManager);
    }

    @Test
    public void shouldCreateTransactionForInformationWithoutPriceInCents() throws Exception {
        Information information = informationFullEditionEntitlementNoPriceInCents();

        when(purchaseManager.getInformation(PRODUCT_IDENTIFIER_FULL_EDITION)).thenReturn(information);

        Transaction transaction = converter.convertToTransaction(activityResultPurchaseFullEditionSuccess());

        assertEquals(0, transaction.getPurchaseCost());
        assertEquals(INAPP_DATA_SIGNATURE_ACTIVITY_RESULT_SUCCESS, transaction.getTransactionDataSignature());
    }
}