package com.badlogic.gdx.pay.android.googleplay;

import android.os.Bundle;

import com.badlogic.gdx.pay.Offer;

import org.junit.Test;

import java.util.ArrayList;

import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.ITEM_ID_LIST;
import static com.badlogic.gdx.pay.android.googleplay.billing.converter.GetSkuDetailsRequestConverter.convertProductIdsToItemIdList;
import static com.badlogic.gdx.pay.android.googleplay.testdata.OfferObjectMother.offerFullEditionEntitlement;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class GetSkuDetailsRequestConverterTest {

    @Test
    public void convertsConfigWithOneOffer() throws Exception {
        Offer offer = offerFullEditionEntitlement();

        Bundle bundle = convertProductIdsToItemIdList(singletonList(offer.getIdentifier()));

        assertEquals(1, bundle.size());
        ArrayList<String> actualList = bundle.getStringArrayList(ITEM_ID_LIST);

        ArrayList<String> expectedArrayList = new ArrayList<>(singletonList(offerFullEditionEntitlement().getIdentifier()));
        assertEquals(expectedArrayList, actualList);
    }
}