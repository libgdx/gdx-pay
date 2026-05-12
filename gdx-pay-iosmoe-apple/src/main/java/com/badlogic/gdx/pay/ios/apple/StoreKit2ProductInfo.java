package com.badlogic.gdx.pay.ios.apple;

import com.badlogic.gdx.pay.FreeTrialPeriod;

import java.math.BigDecimal;

final class StoreKit2ProductInfo {
    final String id;
    final String displayName;
    final String description;
    final String displayPrice;
    final String currencyCode;
    final BigDecimal price;
    final FreeTrialPeriod freeTrialPeriod;

    StoreKit2ProductInfo(
            String id,
            String displayName,
            String description,
            String displayPrice,
            String currencyCode,
            BigDecimal price,
            FreeTrialPeriod freeTrialPeriod) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.displayPrice = displayPrice;
        this.currencyCode = currencyCode;
        this.price = price;
        this.freeTrialPeriod = freeTrialPeriod;
    }
}
