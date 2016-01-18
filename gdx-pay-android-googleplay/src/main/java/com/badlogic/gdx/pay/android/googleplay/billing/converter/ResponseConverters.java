package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import android.os.Bundle;

import com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants;
import com.badlogic.gdx.pay.android.googleplay.ResponseCode;

import static com.badlogic.gdx.pay.android.googleplay.ResponseCode.BILLING_RESPONSE_RESULT_OK;

class ResponseConverters {

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

}
