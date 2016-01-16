package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import android.os.Bundle;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants;
import com.badlogic.gdx.pay.android.googleplay.ResponseCode;

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
import static com.badlogic.gdx.pay.android.googleplay.ResponseCode.BILLING_RESPONSE_RESULT_OK;

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

    protected static void assertSkuListNotEmpty(ArrayList<String> skuDetailsStringList) {
        if (skuDetailsStringList == null || skuDetailsStringList.isEmpty()) {
            throw new IllegalArgumentException("No skus found in response");
        }
    }

    protected static void assertResponseOk(Bundle skuDetailsResponse) {
        int response = skuDetailsResponse.getInt(GoogleBillingConstants.RESPONSE_CODE, -1);

        ResponseCode responseCode = ResponseCode.findByCode(response);

        if (responseCode == null) {
            throw new IllegalArgumentException("Bundle is missing key: " + GoogleBillingConstants.RESPONSE_CODE);
        }

        if (responseCode != BILLING_RESPONSE_RESULT_OK) {
            throw new IllegalArgumentException("Unexpected response code: " + responseCode + ", response: " + skuDetailsResponse);
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
                    .priceInCents((int) object.getLong(PRICE_AMOUNT_MICROS) / 10_000)
                    .priceCurrencyCode(object.getString(PRICE_CURRENCY_CODE))
                    .build());
        }

        return products;
    }
}
