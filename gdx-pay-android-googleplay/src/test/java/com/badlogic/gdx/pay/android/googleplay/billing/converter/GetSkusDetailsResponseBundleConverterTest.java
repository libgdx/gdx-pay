package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import android.os.Bundle;

import com.badlogic.gdx.pay.Information;

import org.junit.Test;

import java.util.Map;

import static com.badlogic.gdx.pay.android.googleplay.billing.converter.GetSkusDetailsResponseBundleConverter.convertSkuDetailsResponse;
import static com.badlogic.gdx.pay.android.googleplay.testdata.GetSkuDetailsResponseBundleObjectMother.skuDetailsResponseResultOkNoPriceAmountMicrosInDetailList;
import static com.badlogic.gdx.pay.android.googleplay.testdata.GetSkuDetailsResponseBundleObjectMother.skuDetailsResponseResultOkProductFullEditionEntitlement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class GetSkusDetailsResponseBundleConverterTest {

    @Test
    public void shouldConvertIfPriceAmountInMicrosIsMissing() throws Exception {
        Bundle skuDetailsResponse = skuDetailsResponseResultOkNoPriceAmountMicrosInDetailList();
        Map<String, Information> informations = convertSkuDetailsResponse(skuDetailsResponse);

        assertFalse(informations.isEmpty());

        assertNull(informations.values().iterator().next().getPriceInCents());
    }

    @Test
    public void shouldSetPriceAmountInMicrosWhenConvertingOne() throws Exception {
        Bundle skuDetailsResponse = skuDetailsResponseResultOkProductFullEditionEntitlement();
        Map<String, Information> informations = convertSkuDetailsResponse(skuDetailsResponse);
        assertFalse(informations.isEmpty());

        Information information = informations.values().iterator().next();

        assertEquals(new Integer(100), information.getPriceInCents());
    }
}