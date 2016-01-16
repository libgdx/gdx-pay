package com.badlogic.gdx.pay.android.googleplay.testdata;

import android.os.Bundle;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants;

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
        Information information = InformationObjectMother.informationFullEditionEntitlement();

        Bundle bundle = new Bundle(3);

        bundle.putInt(RESPONSE_CODE, BILLING_RESPONSE_RESULT_OK.getCode());
        bundle.putStringArrayList(ITEM_ID_LIST, itemIdList(offer));
        bundle.putStringArrayList(DETAILS_LIST, bundleDetailList(offer, information));

        return bundle;
    }

    public static Bundle skuDetailsResponseResultOkIncompleteDetailList() {
        Offer offer = OfferObjectMother.offerFullEditionEntitlement();
        Information information = InformationObjectMother.informationFullEditionEntitlement();

        Bundle bundle = new Bundle(3);

        bundle.putInt(RESPONSE_CODE, BILLING_RESPONSE_RESULT_OK.getCode());
        bundle.putStringArrayList(ITEM_ID_LIST, itemIdList(offer));
        bundle.putStringArrayList(DETAILS_LIST, bundleDetailList(offer, information));

        return bundle;
    }

    private static ArrayList<String> itemIdList(Offer offer) {
        ArrayList<String> skuList = new ArrayList<>();
        skuList.add(offer.getIdentifier());
        return skuList;
    }

    public static Bundle skuDetailsResponseResultNetworkError() {
        Bundle bundle = new Bundle(1);
        bundle.putInt(RESPONSE_CODE, BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE.getCode());

        return bundle;
    }

    private static ArrayList<String> incompleBundleDetailList(Offer offer, Information information) {
        JSONObject object = new JSONObject();

        try {
            // include all details, but not title.

            object.put(GoogleBillingConstants.SKU_DESCRIPTION, information.getLocalDescription());
            object.put(GoogleBillingConstants.SKU_PRICE, information.getLocalPricing());
            object.put(GoogleBillingConstants.PRODUCT_ID, offer.getIdentifier());
            object.put(GoogleBillingConstants.PRICE_AMOUNT_MICROS, information.getPriceInCents() * 10_000);
            object.put(GoogleBillingConstants.PRICE_CURRENCY_CODE, information.getPriceCurrencyCode());
        } catch(JSONException e) {
            throw new IllegalStateException("Failed to create json object", e);
        }

        ArrayList<String> list = new ArrayList<>();
        list.add(object.toString());
        return list;
    }


    private static ArrayList<String> bundleDetailList(Offer offer, Information information) {
        JSONObject object = new JSONObject();

        try {
            object.put(GoogleBillingConstants.SKU_TITLE, information.getLocalName());
            object.put(GoogleBillingConstants.SKU_DESCRIPTION, information.getLocalDescription());
            object.put(GoogleBillingConstants.SKU_PRICE, information.getLocalPricing());
            object.put(GoogleBillingConstants.PRODUCT_ID, offer.getIdentifier());
            object.put(GoogleBillingConstants.PRICE_AMOUNT_MICROS, information.getPriceInCents() * 10_000);
            object.put(GoogleBillingConstants.PRICE_CURRENCY_CODE, information.getPriceCurrencyCode());
        } catch(JSONException e) {
            throw new IllegalStateException("Failed to create json object", e);
        }

        ArrayList<String> list = new ArrayList<>();
        list.add(object.toString());
        return list;
    }
}
