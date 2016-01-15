package com.badlogic.gdx.pay.android.googleplay.testdata;

import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.OfferType;

public class OfferObjectMother {


    public static Offer offerFullEditionEntitlement() {
        Offer offer = new Offer();
        offer.setIdentifier("com.appname.full.edition.2015");
        offer.setType(OfferType.ENTITLEMENT);
        return offer;
    }
}
