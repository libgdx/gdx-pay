package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import android.os.Bundle;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.DETAILS_LIST;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.PRICE_AMOUNT_MICROS;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.PRICE_CURRENCY_CODE;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.PRODUCT_ID;
import static com.badlogic.gdx.pay.android.googleplay.billing.converter.ResponseConverters.assertResponseOk;

public class GetSkusDetailsResponseBundleConverter {

    public static Map<String,Information> convertSkuDetailsResponse(Bundle skuDetailsResponse) {
        assertResponseOk(skuDetailsResponse);

        ArrayList<String> skuDetailsStringList = skuDetailsResponse.getStringArrayList(DETAILS_LIST);

        assertSkuListNotEmpty(skuDetailsStringList);


        try {
            return convertSkuDetailsToInformationMap(skuDetailsStringList);
        } catch(JSONException e) {
            throw new IllegalArgumentException("Failed to parse : " + skuDetailsResponse, e);
        }
    }

    private static void assertSkuListNotEmpty(ArrayList<String> skuDetailsStringList) {
        if (skuDetailsStringList == null || skuDetailsStringList.isEmpty()) {
            throw new IllegalArgumentException("No skus found in response");
        }
    }

    private static Map<String, Information> convertSkuDetailsToInformationMap(List<String> skuDetailsStringList) throws JSONException {
        Map<String, Information> products = new HashMap<>();

        for (String thisResponse : skuDetailsStringList) {
            JSONObject object = new JSONObject(thisResponse);
            String sku = object.getString(PRODUCT_ID);
            String price = object.getString(GoogleBillingConstants.SKU_PRICE);
            String title = object.getString(GoogleBillingConstants.SKU_TITLE);
            String description = object.getString(GoogleBillingConstants.SKU_DESCRIPTION);
            products.put(sku, Information.newBuilder()
                    .localName(title)
                    .localDescription(description)
                    .localPricing(price)
                    .priceInCents(priceInCents(object))
                    .priceCurrencyCode(object.getString(PRICE_CURRENCY_CODE))
                    .build());
        }

        return products;
    }

    private static Integer priceInCents(JSONObject object) throws JSONException {
        if (object.has(PRICE_AMOUNT_MICROS)) {
            return (int) object.getLong(PRICE_AMOUNT_MICROS) / 10_000;
        }
        return null;
    }
}
