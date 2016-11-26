package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import com.badlogic.gdx.pay.OfferType;

import static com.badlogic.gdx.pay.android.googleplay.billing.V3GoogleInAppBillingService.PURCHASE_TYPE_IN_APP;
import static com.badlogic.gdx.pay.android.googleplay.billing.V3GoogleInAppBillingService.PURCHASE_TYPE_SUBSCRIPTION;

public class OfferToInAppPurchaseConverter {

    static public  String convertOfferType(OfferType offerType ) {
        if (offerType.equals(OfferType.SUBSCRIPTION))
            return PURCHASE_TYPE_SUBSCRIPTION;

        return PURCHASE_TYPE_IN_APP;
    }
}
