package com.badlogic.gdx.pay.android.googleplay.testdata;

import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.OfferType;

import static com.badlogic.gdx.pay.android.googleplay.testdata.ProductIdentifierObjectMother.PRODUCT_IDENTIFIER_FULL_EDITION;

public class OfferObjectMother {

    public static Offer offerFullEditionEntitlement() {
        Offer offer = new Offer();
        offer.setIdentifier(PRODUCT_IDENTIFIER_FULL_EDITION);
        offer.setType(OfferType.ENTITLEMENT);
        return offer;
    }

    public static Offer offerSubscription() {
        Offer offer = new Offer();
        offer.setIdentifier("com.appname.subscription");
        offer.setType(OfferType.SUBSCRIPTION);
        return offer;
    }

    public static Offer offerConsumable() {
        Offer offer = new Offer();
        offer.setIdentifier("com.appname.consumable.100.coins");
        offer.setType(OfferType.CONSUMABLE);
        return offer;
    }
}
