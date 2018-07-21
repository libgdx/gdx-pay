package com.badlogic.gdx.pay;

/**
 * This exception is given to {@link PurchaseObserver#handleInstallError(Throwable)} when fetching item information
 * failed.
 * <p>
 * Please note: It is up to the underlying service to return an error. Some services may just return an empty item
 * list without reporting a failure.
 */

public class FetchItemInformationException extends GdxPayException {
    public FetchItemInformationException() {
        super("Failed to fetch item list - check your connection");
    }

    public FetchItemInformationException(String message) {
        super("Failed to fetch item list - check your connection (" + message + ")");
    }
}
