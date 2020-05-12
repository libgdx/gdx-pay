package com.badlogic.gdx.pay.android.huawei;

import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseResult;
import com.huawei.hms.iap.entity.PurchaseIntentResult;

public interface IAPListener {
        void onRegionNotSupported();

        void onLoginRequired();

        void onIAPError(IapApiException exception);

        void onError(Exception exception);

        void onPurchaseResult(PurchaseIntentResult result);

        void onConsumedResult(ConsumeOwnedPurchaseResult result);
    }