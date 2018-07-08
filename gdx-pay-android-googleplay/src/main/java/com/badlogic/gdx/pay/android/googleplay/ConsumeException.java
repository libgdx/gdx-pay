package com.badlogic.gdx.pay.android.googleplay;

import com.badlogic.gdx.pay.GdxPayException;
import com.badlogic.gdx.pay.Transaction;

public class ConsumeException extends GdxPayException {

    public final Transaction transaction;

    public ConsumeException(String message, Transaction transaction, Exception rootCause) {
        super(message, rootCause);
        this.transaction = transaction;
    }

    public ConsumeException(String message, Transaction transaction) {
        this(message, transaction, null);
    }
}
