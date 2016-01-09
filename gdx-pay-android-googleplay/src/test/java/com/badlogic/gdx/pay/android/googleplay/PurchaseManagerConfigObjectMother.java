package com.badlogic.gdx.pay.android.googleplay;

import com.badlogic.gdx.pay.PurchaseManagerConfig;

import static com.badlogic.gdx.pay.PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE;
import static com.badlogic.gdx.pay.android.googleplay.OfferObjectMother.offerFullEditionEntitlement;

public class PurchaseManagerConfigObjectMother {


    public static PurchaseManagerConfig managerConfigGooglePlayOneOfferBuyFullEditionProduct() {
        PurchaseManagerConfig config = new PurchaseManagerConfig();

        config.addStoreParam(STORE_NAME_ANDROID_GOOGLE, "kbiosdfjoifjkldsfjowei8rfjiwfklmujwemflksdfjmsdklfj/sdifjsdlfkjsdfksd");
        config.addOffer(offerFullEditionEntitlement());

        return config;

    }
}
