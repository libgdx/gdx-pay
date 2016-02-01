package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.pay.android.googleplay.GoogleBillingConstants.ITEM_ID_LIST;

public class GetSkuDetailsRequestConverter {

    public static Bundle convertProductIdsToItemIdList(List<String> productIds) {
        ArrayList<String> skuList = new ArrayList<>();

        skuList.addAll(productIds);

        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList(ITEM_ID_LIST, skuList);
        return querySkus;
    }
}
