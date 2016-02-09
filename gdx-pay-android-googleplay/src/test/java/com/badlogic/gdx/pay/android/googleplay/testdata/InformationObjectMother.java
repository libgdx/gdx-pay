package com.badlogic.gdx.pay.android.googleplay.testdata;

import com.badlogic.gdx.pay.Information;

// TODO: return Builder instances instead of inmutable objects.
public class InformationObjectMother {


    public static Information informationFullEditionEntitlement() {
        return Information.newBuilder()
                .localName("Buy full edition")
                .localDescription("Access to all levels")
                .localPricing( "€ 1.00")
                .priceCurrencyCode("EUR")
                .priceInCents(100)
                .build();
    }

    public static Information informationFullEditionEntitlementNoPriceInCents() {
        return Information.newBuilder()
                .localName("Buy full edition")
                .localDescription("Access to all levels")
                .localPricing("€ 1.00")
                .priceCurrencyCode("EUR")
                .build();
    }
}
