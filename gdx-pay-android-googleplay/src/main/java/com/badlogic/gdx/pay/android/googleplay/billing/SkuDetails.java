package com.badlogic.gdx.pay.android.googleplay.billing;

public class SkuDetails {

    private String productId;

    private long priceAmountCents;

    private String priceCurrencyCode;

    private SkuDetails(Builder builder) {
        productId = builder.productId;
        priceAmountCents = builder.priceAmountCents;
        priceCurrencyCode = builder.priceCurrencyCode;
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public String getProductId() {
        return productId;
    }

    public long getPriceAmountCents() {
        return priceAmountCents;
    }

    public String getPriceCurrencyCode() {
        return priceCurrencyCode;
    }


    public static final class Builder {
        private String productId;
        private long priceAmountCents;
        private String priceCurrencyCode;

        private Builder() {
        }

        public Builder productId(String val) {
            productId = val;
            return this;
        }

        public Builder priceAmountCents(long val) {
            priceAmountCents = val;
            return this;
        }

        public Builder priceCurrencyCode(String val) {
            priceCurrencyCode = val;
            return this;
        }

        public SkuDetails build() {
            return new SkuDetails(this);
        }
    }
}
