package com.badlogic.gdx.pay.ios.apple;

import java.util.Collection;
import java.util.List;

interface StoreKit2Bridge {
    boolean canMakePayments();

    void fetchProducts(Collection<String> identifiers, ProductsCallback callback);

    void purchase(String identifier, TransactionCallback callback);

    void fetchCurrentEntitlements(TransactionsCallback callback);

    void restorePurchases(TransactionsCallback callback);

    void startObservingTransactions(TransactionCallback callback);

    void stopObservingTransactions();

    interface ProductsCallback {
        void onResult(List<StoreKit2ProductInfo> products, Throwable error);
    }

    interface TransactionsCallback {
        void onResult(List<StoreKit2TransactionInfo> transactions, Throwable error);
    }

    interface TransactionCallback {
        void onResult(StoreKit2TransactionInfo transaction, Throwable error);
    }
}
