package com.badlogic.gdx.pay.android.googleplay.billing;

// TODO: only needed because Information does not provide enough details.
public interface SkuDetailsFinder {

    SkuDetails getSkuDetails(String productId);
}
