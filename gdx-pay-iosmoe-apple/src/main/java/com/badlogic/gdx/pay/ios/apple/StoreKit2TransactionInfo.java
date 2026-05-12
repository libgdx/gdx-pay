package com.badlogic.gdx.pay.ios.apple;

import java.util.Date;

final class StoreKit2TransactionInfo {
    final String productId;
    final String originalId;
    final Date purchaseDate;
    final String jsonRepresentation;
    final boolean cancelled;
    final boolean pending;

    StoreKit2TransactionInfo(
            String productId,
            String originalId,
            Date purchaseDate,
            String jsonRepresentation,
            boolean cancelled,
            boolean pending) {
        this.productId = productId;
        this.originalId = originalId;
        this.purchaseDate = purchaseDate;
        this.jsonRepresentation = jsonRepresentation;
        this.cancelled = cancelled;
        this.pending = pending;
    }
}
