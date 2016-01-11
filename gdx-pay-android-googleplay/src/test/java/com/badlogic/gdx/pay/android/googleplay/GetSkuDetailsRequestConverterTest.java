package com.badlogic.gdx.pay.android.googleplay;

import android.os.Bundle;

import com.badlogic.gdx.pay.PurchaseManagerConfig;

import org.junit.Test;

import java.util.ArrayList;

import static com.badlogic.gdx.pay.android.googleplay.GetSkuDetailsRequestConverter.convertConfigToItemIdList;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.ITEM_ID_LIST;
import static com.badlogic.gdx.pay.android.googleplay.OfferObjectMother.offerFullEditionEntitlement;
import static com.badlogic.gdx.pay.android.googleplay.PurchaseManagerConfigObjectMother.managerConfigGooglePlayOneOfferBuyFullEditionProduct;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class GetSkuDetailsRequestConverterTest {

    @Test
    public void convertsConfigWithOneOffer() throws Exception {
        PurchaseManagerConfig config = managerConfigGooglePlayOneOfferBuyFullEditionProduct();

        Bundle bundle = convertConfigToItemIdList(config);

        assertEquals(1, bundle.size());
        ArrayList<String> actualList = bundle.getStringArrayList(ITEM_ID_LIST);

        ArrayList<String> expectedArrayList = new ArrayList<>(singletonList(offerFullEditionEntitlement().getIdentifier()));
        assertEquals(expectedArrayList, actualList);
    }
}