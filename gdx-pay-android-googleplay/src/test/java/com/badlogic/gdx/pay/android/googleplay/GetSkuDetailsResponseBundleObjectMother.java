package com.badlogic.gdx.pay.android.googleplay;

import android.os.Bundle;

import com.badlogic.gdx.pay.Offer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.DETAILS_LIST;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.ITEM_ID_LIST;
import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.RESPONSE_CODE;
import static com.badlogic.gdx.pay.android.googleplay.ResponseCode.BILLING_RESPONSE_RESULT_OK;
import static com.badlogic.gdx.pay.android.googleplay.ResponseCode.BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE;

public class GetSkuDetailsResponseBundleObjectMother {

    public static Bundle skuDetailsResponseResultOkProductFullEditionEntitlement() {
        Offer offer = OfferObjectMother.offerFullEditionEntitlement();

        Bundle bundle = new Bundle(3);

        bundle.putInt(RESPONSE_CODE, BILLING_RESPONSE_RESULT_OK.getCode());
        bundle.putStringArrayList(ITEM_ID_LIST, itemIdList(offer));
        bundle.putStringArrayList(DETAILS_LIST, detailListPrice1Euro(offer));

        return bundle;
    }

    protected static ArrayList<String> itemIdList(Offer offer) {
        ArrayList<String> skuList = new ArrayList<String>();
        skuList.add(offer.getIdentifier());
        return skuList;
    }

    public static Bundle skuDetailsResponseResultNetworkError() {
        Bundle bundle = new Bundle(1);
        bundle.putInt(RESPONSE_CODE, BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE.getCode());

        return bundle;
    }

    private static ArrayList<String> detailListPrice1Euro(Offer offer) {
        JSONObject object = new JSONObject();

        try {
            object.put(GoogleBillingConstants.SKU_TITLE, "Buy full edition");
            object.put(GoogleBillingConstants.SKU_DESCRIPTION, "Access to all levels");
            object.put(GoogleBillingConstants.SKU_PRICE, "€ 1.00");
            object.put(GoogleBillingConstants.SKU_PRODUCT_ID, offer.getIdentifier());
        } catch(JSONException e) {
            throw new IllegalStateException("Failed to create json object", e);
        }


        ArrayList<String> list = new ArrayList<String>();
        list.add(object.toString());
        return list;
    }
}
