package com.badlogic.gdx.pay;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Currency;

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

    /**
     * @deprecated Not all currencies use cents. Currencies with no or more than 2 fractional
     * digits exist. Use {@link #priceAsDouble} instead.
     */
    @Deprecated
    private Integer priceInCents;
    private Double priceAsDouble;
    private BigDecimal priceAsBigDecimal;

    private String priceCurrencyCode;

    @Nullable
    private FreeTrialPeriod freeTrialPeriod;

    public Information(String localName, String localDescription, String localPricing) {
        this.localName = localName;
        this.localDescription = localDescription;
        this.localPricing = localPricing;
    }

    private Information(Builder builder) {
        localName = builder.localName;
        localDescription = builder.localDescription;
        localPricing = builder.localPricing;
        priceInCents = builder.priceInCents;
        priceAsDouble = builder.priceAsDouble;
        priceAsBigDecimal = builder.priceAsBigDecimal;
        priceCurrencyCode = builder.priceCurrencyCode;
        freeTrialPeriod = builder.freeTrialPeriod;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Price in cents.
     * <p>Caution: this field could be null, information is not always available! </p>
     *
     * @deprecated Not all currencies use cents. Currencies with no or more than 2 fractional
     * digits exist. Use {@link #getPriceAsDouble()} instead.
     */
    @Deprecated
    @Nullable
    public Integer getPriceInCents() {
        return priceInCents;
    }

    /**
     *
     * @return null if there is no free trial or the implementation does not support free trials.
     */
    @Nullable
    public FreeTrialPeriod getFreeTrialPeriod() {
        return freeTrialPeriod;
    }

    /**
     * Price (as a double).
     * <p>Caution: this field could be null, information is not always available! </p>
     * <p>Use {@link Currency#getDefaultFractionDigits()} to format the price with the correct
     * number of fraction digits for its currency:</p>
     * <pre>
     * NumberFormat priceFormat = NumberFormat.getCurrencyInstance(yourUsersLocale);
     * Currency currency = Currency.getInstance(information.getCurrencyCode());
     * priceFormat.setCurrency(currency);
     * priceFormat.setMaximumFractionDigits(currency.getDefaultFractionDigits());
     * priceFormat.setMinimumFractionDigits(currency.getDefaultFractionDigits());
     * priceFormat.format(information.getPriceAsDouble());
     * </pre>
     * <p>Note that this will not always output the currency symbol (e.g. €), but use the
     * currency code (e.g. EUR) instead.</p>
     */
    @Nullable
    public Double getPriceAsDouble() {
        return priceAsDouble;
    }

    /**
     * Price (as a BigDecimal).
     * <p>Caution: this field could be null, information is not always available! </p>
     */
    @Nullable
    public BigDecimal getPriceAsBigDecimal() {
        return priceAsBigDecimal;
    }

    /**
     * Price currency code.
     * <p>Caution:Note that not all PurchaseManagers set this field!</p>
     * @return
     */
    public String getPriceCurrencyCode() {
        return priceCurrencyCode;
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

    public static final class Builder {
        private String localName;
        private String localDescription;
        private String localPricing;
        /**
         * @deprecated Not all currencies use cents. Currencies with no or more than 2 fractional
         * digits exist. Use {@link #priceAsDouble} instead.
         */
        @Deprecated
        private Integer priceInCents;
        private Double priceAsDouble;
        private BigDecimal priceAsBigDecimal;
        private String priceCurrencyCode;
        private FreeTrialPeriod freeTrialPeriod;

        private Builder() {
        }

        public Builder localName(String val) {
            localName = val;
            return this;
        }

        public Builder localDescription(String val) {
            localDescription = val;
            return this;
        }

        public Builder freeTrialPeriod(FreeTrialPeriod val) {
            freeTrialPeriod = val;
            return this;
        }

        public Builder localPricing(String val) {
            localPricing = val;
            return this;
        }

        /**
         * @deprecated Not all currencies use cents. Currencies with no or more than 2 fractional
         * digits exist. Use {@link #priceAsDouble(Double)} instead.
         */
        @Deprecated
        public Builder priceInCents(Integer val) {
            priceInCents = val;
            return this;
        }

        public Builder priceAsDouble(Double val) {
            priceAsDouble = val;
            return this;
        }

        public Builder priceAsBigDecimal(BigDecimal val) {
            priceAsBigDecimal = val;
            return this;
        }

        public Builder priceCurrencyCode(String val) {
            priceCurrencyCode = val;
            return this;
        }

        public Information build() {
            return new Information(this);
        }
    }
}
