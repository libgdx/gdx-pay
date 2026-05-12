package com.badlogic.gdx.pay.ios.apple;

import apple.foundation.NSArray;
import apple.foundation.NSDictionary;
import apple.foundation.NSError;
import apple.foundation.NSNumber;

import com.badlogic.gdx.pay.FreeTrialPeriod;
import com.badlogic.gdx.pay.ios.apple.bindings.GDXStoreKit2Bridge;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

final class NatJStoreKit2Bridge implements StoreKit2Bridge {
    private final GDXStoreKit2Bridge bridge;

    NatJStoreKit2Bridge() {
        this(GDXStoreKit2Bridge.shared());
    }

    NatJStoreKit2Bridge(GDXStoreKit2Bridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public boolean canMakePayments() {
        return bridge.canMakePayments();
    }

    @Override
    public void fetchProducts(Collection<String> identifiers, final ProductsCallback callback) {
        bridge.fetchProductsWithIdentifiersCompletion(toNSArray(identifiers),
                new GDXStoreKit2Bridge.Block_fetchProductsWithIdentifiersCompletion() {
                    @Override
                    public void call_fetchProductsWithIdentifiersCompletion(
                            NSArray<NSDictionary<String, Object>> products, NSError error) {
                        callback.onResult(mapProducts(products), toThrowable(error));
                    }
                });
    }

    @Override
    public void purchase(String identifier, final TransactionCallback callback) {
        bridge.purchaseWithIdentifierCompletion(identifier,
                new GDXStoreKit2Bridge.Block_purchaseWithIdentifierCompletion() {
                    @Override
                    public void call_purchaseWithIdentifierCompletion(NSDictionary<String, Object> transaction, NSError error) {
                        callback.onResult(mapTransaction(transaction), toThrowable(error));
                    }
                });
    }

    @Override
    public void fetchCurrentEntitlements(final TransactionsCallback callback) {
        bridge.fetchCurrentEntitlementsWithCompletion(
                new GDXStoreKit2Bridge.Block_fetchCurrentEntitlementsWithCompletion() {
                    @Override
                    public void call_fetchCurrentEntitlementsWithCompletion(
                            NSArray<NSDictionary<String, Object>> transactions, NSError error) {
                        callback.onResult(mapTransactions(transactions), toThrowable(error));
                    }
                });
    }

    @Override
    public void restorePurchases(final TransactionsCallback callback) {
        bridge.restorePurchasesWithCompletion(new GDXStoreKit2Bridge.Block_restorePurchasesWithCompletion() {
            @Override
            public void call_restorePurchasesWithCompletion(
                    NSArray<NSDictionary<String, Object>> transactions, NSError error) {
                callback.onResult(mapTransactions(transactions), toThrowable(error));
            }
        });
    }

    @Override
    public void startObservingTransactions(final TransactionCallback callback) {
        bridge.startObservingTransactionsWithCompletion(
                new GDXStoreKit2Bridge.Block_startObservingTransactionsWithCompletion() {
                    @Override
                    public void call_startObservingTransactionsWithCompletion(
                            NSDictionary<String, Object> transaction, NSError error) {
                        callback.onResult(mapTransaction(transaction), toThrowable(error));
                    }
                });
    }

    @Override
    public void stopObservingTransactions() {
        bridge.stopObservingTransactions();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private NSArray<String> toNSArray(Collection<String> values) {
        if (values.isEmpty()) return (NSArray<String>) NSArray.array();

        Object[] strings = values.toArray(new Object[0]);
        Object first = strings[0];
        Object[] rest = new Object[strings.length - 1];
        System.arraycopy(strings, 1, rest, 0, rest.length);
        return (NSArray<String>) NSArray.arrayWithObjects(first, rest);
    }

    private List<StoreKit2ProductInfo> mapProducts(NSArray<NSDictionary<String, Object>> products) {
        List<StoreKit2ProductInfo> result = new ArrayList<>();
        if (products == null) return result;

        for (NSDictionary<String, Object> product : products) {
            result.add(mapProduct(product));
        }
        return result;
    }

    private StoreKit2ProductInfo mapProduct(NSDictionary<String, Object> product) {
        return new StoreKit2ProductInfo(
                string(product, "id"),
                string(product, "displayName"),
                string(product, "description"),
                string(product, "displayPrice"),
                string(product, "currencyCode"),
                decimal(product, "price"),
                freeTrialPeriod(product));
    }

    private List<StoreKit2TransactionInfo> mapTransactions(NSArray<NSDictionary<String, Object>> transactions) {
        List<StoreKit2TransactionInfo> result = new ArrayList<>();
        if (transactions == null) return result;

        for (NSDictionary<String, Object> transaction : transactions) {
            result.add(mapTransaction(transaction));
        }
        return result;
    }

    private StoreKit2TransactionInfo mapTransaction(NSDictionary<String, Object> transaction) {
        if (transaction == null) return null;

        return new StoreKit2TransactionInfo(
                string(transaction, "productID"),
                string(transaction, "originalID"),
                date(transaction, "purchaseDate"),
                firstNonEmpty(string(transaction, "jsonRepresentationBase64"), string(transaction, "jsonRepresentation")),
                bool(transaction, "cancelled"),
                bool(transaction, "pending"));
    }

    private FreeTrialPeriod freeTrialPeriod(NSDictionary<String, Object> product) {
        if (!bool(product, "eligibleForIntroOffer")) return null;

        BigDecimal introPrice = decimal(product, "introPrice");
        if (introPrice != null && introPrice.compareTo(BigDecimal.ZERO) > 0) return null;

        Number value = number(product, "introPeriodValue");
        String unit = string(product, "introPeriodUnit");
        if (value == null || unit == null) return null;

        FreeTrialPeriod.PeriodUnit periodUnit;
        if ("day".equals(unit)) periodUnit = FreeTrialPeriod.PeriodUnit.DAY;
        else if ("week".equals(unit)) periodUnit = FreeTrialPeriod.PeriodUnit.WEEK;
        else if ("month".equals(unit)) periodUnit = FreeTrialPeriod.PeriodUnit.MONTH;
        else if ("year".equals(unit)) periodUnit = FreeTrialPeriod.PeriodUnit.YEAR;
        else return null;

        return new FreeTrialPeriod(value.intValue(), periodUnit);
    }

    private Throwable toThrowable(NSError error) {
        if (error == null) return null;
        return new RuntimeException(error.localizedDescription());
    }

    private String firstNonEmpty(String first, String second) {
        return first != null && first.length() > 0 ? first : second;
    }

    private String string(NSDictionary<String, Object> dictionary, String key) {
        Object value = value(dictionary, key);
        return value != null ? value.toString() : null;
    }

    private boolean bool(NSDictionary<String, Object> dictionary, String key) {
        Object value = value(dictionary, key);
        if (value instanceof NSNumber) return ((NSNumber) value).boolValue();
        if (value instanceof Boolean) return (Boolean) value;
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private Date date(NSDictionary<String, Object> dictionary, String key) {
        Number value = number(dictionary, key);
        return value != null ? new Date((long) (value.doubleValue() * 1000D)) : new Date();
    }

    private BigDecimal decimal(NSDictionary<String, Object> dictionary, String key) {
        Number number = number(dictionary, key);
        return number != null ? BigDecimal.valueOf(number.doubleValue()) : null;
    }

    private Number number(NSDictionary<String, Object> dictionary, String key) {
        Object value = value(dictionary, key);
        if (value instanceof NSNumber) return ((NSNumber) value).doubleValue();
        if (value instanceof Number) return (Number) value;
        if (value == null) return null;
        return Double.valueOf(value.toString());
    }

    private Object value(NSDictionary<String, Object> dictionary, String key) {
        return dictionary != null ? dictionary.objectForKey(key) : null;
    }
}
