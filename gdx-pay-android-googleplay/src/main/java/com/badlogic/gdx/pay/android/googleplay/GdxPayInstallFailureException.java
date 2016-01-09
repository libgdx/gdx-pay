package com.badlogic.gdx.pay.android.googleplay;

import com.badlogic.gdx.pay.PurchaseManagerConfig;

public class GdxPayInstallFailureException extends RuntimeException {

    private PurchaseManagerConfig requestConfig;

    public GdxPayInstallFailureException(Exception rootCause, PurchaseManagerConfig requestConfig) {
        super(rootCause);
        this.requestConfig = requestConfig;
    }

    public PurchaseManagerConfig getRequestConfig() {
        return requestConfig;
    }

}
