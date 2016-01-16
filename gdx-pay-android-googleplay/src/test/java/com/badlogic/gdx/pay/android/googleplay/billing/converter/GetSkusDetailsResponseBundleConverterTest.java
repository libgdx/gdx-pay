package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import android.os.Bundle;

import com.badlogic.gdx.pay.android.googleplay.billing.SkuDetails;

import org.junit.Test;

import java.util.List;

import static com.badlogic.gdx.pay.android.googleplay.billing.converter.GetSkusDetailsResponseBundleConverter.convertToSkuDetailsList;
import static com.badlogic.gdx.pay.android.googleplay.testdata.GetSkuDetailsResponseBundleObjectMother.skuDetailsResponseResultOkProductFullEditionEntitlement;
import static org.junit.Assert.assertEquals;

public class GetSkusDetailsResponseBundleConverterTest {

    @Test
    public void convertsToSkuDetails() throws Exception {

        Bundle bundle = skuDetailsResponseResultOkProductFullEditionEntitlement();

        List<SkuDetails> skuDetailses = convertToSkuDetailsList(bundle);

        assertEquals(1, skuDetailses.size());

        SkuDetails skuDetails = skuDetailses.get(0);

        assertEquals(299, skuDetails.getPriceAmountCents());
        assertEquals("EUR", skuDetails.getPriceCurrencyCode());
    }
}