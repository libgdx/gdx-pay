package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import android.os.Bundle;

import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.PurchaseManagerConfig;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.ITEM_ID_LIST;

public class GetSkuDetailsRequestConverter {

    public static Bundle convertConfigToItemIdList(PurchaseManagerConfig purchaseManagerConfig) {
        ArrayList<String> skuList = new ArrayList<>();

        for (int i = 0; i < purchaseManagerConfig.getOfferCount(); i++) {
            Offer offer = purchaseManagerConfig.getOffer(i);

            skuList.add(offer.getIdentifier());
        }

        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList(ITEM_ID_LIST, skuList);
        return querySkus;
    }

    public static Bundle convertConfigToItemIdList(List<String> productIds) {
        ArrayList<String> skuList = new ArrayList<>();

        skuList.addAll(productIds);

        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList(ITEM_ID_LIST, skuList);
        return querySkus;
    }
}
