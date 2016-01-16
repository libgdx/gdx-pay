package com.badlogic.gdx.pay;

public interface InformationFinder {
    /**
     * Returns information about a product provided by the purchase manager.
     *
     * @return the information for given identifier, or {@link Information#UNAVAILABLE} if no product for given <code>identifier</code> is loaded.
     */
    Information getInformation(String identifier);
}
