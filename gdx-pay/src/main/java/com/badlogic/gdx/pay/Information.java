package com.badlogic.gdx.pay;

/**
 * Information about a product that can be purchased provided by a purchase manager. Some methods
 * will return 'null' if requested information is not available.
 *
 * @author noblemaster
 */
public final class Information {

    /**
     * The information returned if a purchase manager does not support information.
     */
    public static final Information UNAVAILABLE = new Information(null, null, null);

    private final String localName;
    private final String localDescription;
    private final String localPricing;

    public Information(String localName, String localDescription, String localPricing) {
        this.localName = localName;
        this.localDescription = localDescription;
        this.localPricing = localPricing;
    }

    /**
     * Returns the localized product name or null if not available (PurchaseManager-dependent).
     */
    public String getLocalName() {
        return localName;
    }

    /**
     * Returns the localized product description or null if not available (PurchaseManager-dependent).
     */
    public String getLocalDescription() {
        return localDescription;
    }

    /**
     * Returns the localized product price or null if not available (PurchaseManager-dependent).
     */
    public String getLocalPricing() {
        return localPricing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Information that = (Information) o;

        if (localName != null ? !localName.equals(that.localName) : that.localName != null)
            return false;
        if (localDescription != null ? !localDescription.equals(that.localDescription) : that.localDescription != null)
            return false;
        return !(localPricing != null ? !localPricing.equals(that.localPricing) : that.localPricing != null);

    }

    @Override
    public int hashCode() {
        int result = localName != null ? localName.hashCode() : 0;
        result = 31 * result + (localDescription != null ? localDescription.hashCode() : 0);
        result = 31 * result + (localPricing != null ? localPricing.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Information{" +
                "localName='" + localName + '\'' +
                ", localDescription='" + localDescription + '\'' +
                ", localPricing='" + localPricing + '\'' +
                '}';
    }
}
