package com.badlogic.gdx.pay;

/**
 * To be implemented by {@link PurchaseManager} instances that have the ability to cancel test purchases.
 *
 * <p>Currently only supported by android-googleplay implementation.</p>
 */
public interface PurchaseManagerTestSupport {

    void cancelTestPurchases();
}
