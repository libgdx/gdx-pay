package com.badlogic.gdx.pay.android.huawei;

public class PurchaseError extends Error {
    private int code;

    public PurchaseError(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
