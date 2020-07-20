package com.badlogic.gdx.pay.android.huawei;

import com.badlogic.gdx.pay.GdxPayException;

public class PurchaseError extends GdxPayException {
    private int code;

    public PurchaseError(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
