package com.badlogic.gdx.pay.android.googlebilling;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;

/**
 * The purchase manager implementation for Google Play (Android) using Google Billing Library.
 * <p>
 * Created by Benjamin Schulte on 07.07.2018.
 */

public class PurchaseManagerGoogleBilling implements PurchaseManager {
    @Override
    public Information getInformation(String identifier) {
        return null;
    }

    @Override
    public String storeName() {
        return null;
    }

    @Override
    public void install(PurchaseObserver observer, PurchaseManagerConfig config, boolean autoFetchInformation) {

    }

    @Override
    public boolean installed() {
        return false;
    }

    @Override
    public void dispose() {

    }

    @Override
    public void purchase(String identifier) {

    }

    @Override
    public void purchaseRestore() {

    }
}
