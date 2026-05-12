/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.pay.ios.apple;

import com.badlogic.gdx.pay.FetchItemInformationException;
import com.badlogic.gdx.pay.GdxPayException;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The purchase manager implementation for Apple's iOS IAP system using StoreKit 2 through
 * a Swift bridge that is exposed to MOE/NatJ.
 */
public class PurchaseManageriOSApple implements PurchaseManager {
    private static final String TAG = "GdxPay/AppleIOS/MOE";
    private static final boolean LOGDEBUG =
            Boolean.parseBoolean(System.getProperty("gdx.pay.ios.apple.logdebug", "false"));
    private static final int LOGTYPELOG = 0;
    private static final int LOGTYPEERROR = 1;

    private final StoreKit2Bridge storeKit2Bridge;
    private final Map<String, StoreKit2ProductInfo> productsByStoreIdentifier = new HashMap<>();

    private PurchaseObserver observer;
    private PurchaseManagerConfig config;
    private boolean installed;

    public PurchaseManageriOSApple() {
        this(new NatJStoreKit2Bridge());
    }

    PurchaseManageriOSApple(StoreKit2Bridge storeKit2Bridge) {
        this.storeKit2Bridge = storeKit2Bridge;
    }

    @Override
    public String storeName() {
        return PurchaseManagerConfig.STORE_NAME_IOS_APPLE;
    }

    @Override
    public void install(PurchaseObserver observer, PurchaseManagerConfig config, boolean autoFetchInformation) {
        this.observer = observer;
        this.config = config;

        log(LOGTYPELOG, "Installing StoreKit 2 purchase manager...");

        if (!storeKit2Bridge.canMakePayments()) {
            observer.handleInstallError(new GdxPayException(
                    "Error installing purchase observer: Device not configured for purchases!"));
            return;
        }

        Collection<String> productIdentifiers = configuredStoreIdentifiers(config);
        if (productIdentifiers.isEmpty()) {
            installed = true;
            observer.handleInstall();
            return;
        }

        storeKit2Bridge.fetchProducts(productIdentifiers, new StoreKit2Bridge.ProductsCallback() {
            @Override
            public void onResult(List<StoreKit2ProductInfo> products, Throwable error) {
                if (error != null) {
                    String message = "Error requesting products: " + error.getMessage();
                    log(LOGTYPEERROR, message, error);
                    PurchaseManageriOSApple.this.observer.handleInstallError(
                            new FetchItemInformationException(message));
                    return;
                }

                cacheProducts(products);
                installed = true;
                observeTransactionUpdates();
                restoreCurrentEntitlementsOnStartup();
                PurchaseManageriOSApple.this.observer.handleInstall();
            }
        });
    }

    @Override
    public boolean installed() {
        return installed;
    }

    @Override
    public void dispose() {
        storeKit2Bridge.stopObservingTransactions();
        productsByStoreIdentifier.clear();
        observer = null;
        config = null;
        installed = false;
        log(LOGTYPELOG, "Disposed purchase manager!");
    }

    @Override
    public void purchase(final String identifier) {
        if (config == null || observer == null) return;

        Offer offer = config.getOffer(identifier);
        if (offer == null) {
            observer.handlePurchaseError(new GdxPayException("Unknown offer: " + identifier));
            return;
        }

        final String identifierForStore = offer.getIdentifierForStore(PurchaseManagerConfig.STORE_NAME_IOS_APPLE);
        StoreKit2ProductInfo product = productsByStoreIdentifier.get(identifierForStore);
        if (product == null) {
            log(LOGTYPELOG, "Requesting product info for " + identifierForStore);
            storeKit2Bridge.fetchProducts(singleton(identifierForStore), new StoreKit2Bridge.ProductsCallback() {
                @Override
                public void onResult(List<StoreKit2ProductInfo> products, Throwable error) {
                    if (error != null) {
                        observer.handlePurchaseError(new GdxPayException(
                                "Error requesting product info to later purchase: " + error.getMessage()));
                        return;
                    }

                    cacheProducts(products);
                    startPurchase(identifierForStore);
                }
            });
            return;
        }

        startPurchase(identifierForStore);
    }

    @Override
    public void purchaseRestore() {
        log(LOGTYPELOG, "Restoring purchases...");
        storeKit2Bridge.restorePurchases(new StoreKit2Bridge.TransactionsCallback() {
            @Override
            public void onResult(List<StoreKit2TransactionInfo> transactions, Throwable error) {
                if (error != null) {
                    observer.handleRestoreError(new GdxPayException(
                            "Restoring of purchases failed: " + error.getMessage()));
                    return;
                }

                observer.handleRestore(toGdxTransactions(transactions));
            }
        });
    }

    @Override
    public Information getInformation(String identifier) {
        StoreKit2ProductInfo product = productsByStoreIdentifier.get(identifier);
        if (product == null && config != null && config.getOffer(identifier) != null) {
            String storeIdentifier = config.getOffer(identifier)
                    .getIdentifierForStore(PurchaseManagerConfig.STORE_NAME_IOS_APPLE);
            product = productsByStoreIdentifier.get(storeIdentifier);
        }
        if (product == null) return Information.UNAVAILABLE;

        return Information.newBuilder()
                .localName(product.displayName)
                .localDescription(product.description)
                .localPricing(product.displayPrice)
                .priceInCents(priceInCents(product.price))
                .priceAsDouble(product.price != null ? product.price.doubleValue() : null)
                .priceCurrencyCode(product.currencyCode)
                .freeTrialPeriod(product.freeTrialPeriod)
                .build();
    }

    @Override
    public String toString() {
        return PurchaseManagerConfig.STORE_NAME_IOS_APPLE;
    }

    void log(final int type, final String message) {
        log(type, message, null);
    }

    void log(final int type, final String message, Throwable e) {
        if (LOGDEBUG) {
            if (type == LOGTYPELOG) System.out.println('[' + TAG + "] " + message);
            if (type == LOGTYPEERROR) System.err.println('[' + TAG + "] " + message);
            if (e != null) e.printStackTrace(System.err);
        }
    }

    private void startPurchase(final String identifierForStore) {
        log(LOGTYPELOG, "Purchasing product " + identifierForStore + " ...");
        storeKit2Bridge.purchase(identifierForStore, new StoreKit2Bridge.TransactionCallback() {
            @Override
            public void onResult(StoreKit2TransactionInfo transaction, Throwable error) {
                if (error != null) {
                    observer.handlePurchaseError(new GdxPayException(
                            "Purchasing product " + identifierForStore + " failed: " + error.getMessage()));
                    return;
                }

                if (transaction == null) {
                    observer.handlePurchaseError(new GdxPayException(
                            "Purchasing product " + identifierForStore + " returned no transaction"));
                    return;
                }

                if (transaction.cancelled) {
                    observer.handlePurchaseCanceled();
                    return;
                }

                if (transaction.pending) {
                    log(LOGTYPELOG, "Purchase is pending for " + identifierForStore);
                    return;
                }

                Transaction gdxTransaction = transaction(transaction);
                if (gdxTransaction != null) observer.handlePurchase(gdxTransaction);
            }
        });
    }

    private void observeTransactionUpdates() {
        storeKit2Bridge.startObservingTransactions(new StoreKit2Bridge.TransactionCallback() {
            @Override
            public void onResult(StoreKit2TransactionInfo transaction, Throwable error) {
                if (error != null) {
                    observer.handlePurchaseError(new GdxPayException(
                            "Transaction update failed: " + error.getMessage()));
                    return;
                }

                Transaction gdxTransaction = transaction(transaction);
                if (gdxTransaction != null) observer.handlePurchase(gdxTransaction);
            }
        });
    }

    private void restoreCurrentEntitlementsOnStartup() {
        storeKit2Bridge.fetchCurrentEntitlements(new StoreKit2Bridge.TransactionsCallback() {
            @Override
            public void onResult(List<StoreKit2TransactionInfo> transactions, Throwable error) {
                if (error != null) {
                    log(LOGTYPEERROR, "Startup entitlement restore failed: " + error.getMessage(), error);
                    return;
                }

                Transaction[] restoredTransactions = toGdxTransactions(transactions);
                if (restoredTransactions.length > 0) observer.handleRestore(restoredTransactions);
            }
        });
    }

    private Transaction[] toGdxTransactions(List<StoreKit2TransactionInfo> transactions) {
        List<Transaction> restored = new ArrayList<>();
        for (StoreKit2TransactionInfo transaction : transactions) {
            Transaction gdxTransaction = transaction(transaction);
            if (gdxTransaction != null) restored.add(gdxTransaction);
        }
        return restored.toArray(new Transaction[0]);
    }

    private Transaction transaction(StoreKit2TransactionInfo info) {
        if (info == null) return null;

        Offer offerForStore = config.getOfferForStore(PurchaseManagerConfig.STORE_NAME_IOS_APPLE, info.productId);
        if (offerForStore == null) {
            log(LOGTYPEERROR, "Product not configured in PurchaseManagerConfig: " + info.productId);
            return null;
        }

        StoreKit2ProductInfo product = productsByStoreIdentifier.get(info.productId);

        Transaction transaction = new Transaction();
        transaction.setIdentifier(offerForStore.getIdentifier());
        transaction.setStoreName(PurchaseManagerConfig.STORE_NAME_IOS_APPLE);
        transaction.setOrderId(info.originalId);
        transaction.setPurchaseTime(info.purchaseDate);
        transaction.setPurchaseText("Purchased: " + (product != null ? product.displayName : info.productId));
        transaction.setPurchaseCost(product != null ? priceInCents(product.price) : 0);
        transaction.setPurchaseCostCurrency(product != null ? product.currencyCode : null);
        transaction.setReversalTime(null);
        transaction.setReversalText(null);
        transaction.setTransactionData(null);
        transaction.setTransactionDataSignature(info.jsonRepresentation);
        return transaction;
    }

    private void cacheProducts(List<StoreKit2ProductInfo> products) {
        for (StoreKit2ProductInfo product : products) {
            productsByStoreIdentifier.put(product.id, product);
        }
    }

    private Collection<String> configuredStoreIdentifiers(PurchaseManagerConfig config) {
        int size = config.getOfferCount();
        Set<String> productIdentifiers = new HashSet<String>(size);
        for (int i = 0; i < size; i++) {
            productIdentifiers.add(config.getOffer(i)
                    .getIdentifierForStore(PurchaseManagerConfig.STORE_NAME_IOS_APPLE));
        }
        return productIdentifiers;
    }

    private Collection<String> singleton(String value) {
        Set<String> values = new HashSet<String>(1);
        values.add(value);
        return values;
    }

    private int priceInCents(BigDecimal price) {
        if (price == null) return 0;
        return price.multiply(BigDecimal.valueOf(100D)).setScale(0, RoundingMode.CEILING).intValue();
    }
}
