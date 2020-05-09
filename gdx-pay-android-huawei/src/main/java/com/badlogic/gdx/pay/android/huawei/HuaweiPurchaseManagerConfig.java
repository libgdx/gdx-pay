package com.badlogic.gdx.pay.android.huawei;

import com.badlogic.gdx.pay.PurchaseObserver;
import com.huawei.hms.iap.entity.ProductInfo;

import java.util.concurrent.ConcurrentHashMap;

class HuaweiPurchaseManagerConfig {
    PurchaseObserver observer;
    IAPListener iapListener;
    ConcurrentHashMap<String, ProductInfo> productInfoMap = new ConcurrentHashMap<>();
    boolean installationComplete;
}
