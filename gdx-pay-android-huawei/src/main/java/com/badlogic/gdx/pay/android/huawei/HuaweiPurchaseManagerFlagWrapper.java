package com.badlogic.gdx.pay.android.huawei;

import com.huawei.hms.iap.IapClient;

class HuaweiPurchaseManagerFlagWrapper {
    boolean hasToFetchEntetliments = false;
    boolean hasToFetchConsumables = false;
    boolean hasToFetchSubscriptions = false;

    boolean hasFetchedAllProducts() {
        return !this.hasToFetchConsumables &&
                !this.hasToFetchEntetliments &&
                !this.hasToFetchSubscriptions;
    }

    void resetFetchFlagByType(int offerType) {
        switch (offerType) {
            case IapClient.PriceType.IN_APP_CONSUMABLE:
                this.hasToFetchConsumables = false;
                break;
            case IapClient.PriceType.IN_APP_NONCONSUMABLE:
                this.hasToFetchEntetliments = false;
                break;
            case IapClient.PriceType.IN_APP_SUBSCRIPTION:
                this.hasToFetchSubscriptions = false;
                break;
        }
    }
}
