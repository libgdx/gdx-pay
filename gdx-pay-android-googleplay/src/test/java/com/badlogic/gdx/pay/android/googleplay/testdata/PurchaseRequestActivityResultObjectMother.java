package com.badlogic.gdx.pay.android.googleplay.testdata;

import android.content.Intent;

import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants;

import org.json.JSONException;
import org.json.JSONObject;

import static com.badlogic.gdx.pay.android.googleplay.testdata.OfferObjectMother.offerFullEditionEntitlement;

public class PurchaseRequestActivityResultObjectMother {

    public static final String INAPP_DATA_SIGNATURE_ACTIVITY_RESULT_SUCCESS = "NowIdeaHowSuchAValueLooksTODOFindRealValue";

    public static Intent activityResultPurchaseFullEditionSuccess() {
        Intent intent = new Intent();
        intent.putExtra(GoogleBillingConstants.INAPP_PURCHASE_DATA, makeInAppPurchaseJsonData());
        intent.putExtra(GoogleBillingConstants.INAPP_DATA_SIGNATURE, INAPP_DATA_SIGNATURE_ACTIVITY_RESULT_SUCCESS);
        return intent;
    }

    private static String makeInAppPurchaseJsonData() {
        Offer offer = offerFullEditionEntitlement();

        try {
            return makeJsonObjectForOffer(offer);
        } catch(JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static String makeJsonObjectForOffer(Offer offer) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(GoogleBillingConstants.PRODUCT_ID, offer.getIdentifier());
        jsonObject.put(GoogleBillingConstants.PURCHASE_TIME, System.currentTimeMillis());
        jsonObject.put(GoogleBillingConstants.ORDER_ID, "GPA.1234-5678-9012-34567");

        return jsonObject.toString();
    }
}
