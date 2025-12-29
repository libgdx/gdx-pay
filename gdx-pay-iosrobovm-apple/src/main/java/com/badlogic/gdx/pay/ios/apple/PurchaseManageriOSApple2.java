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

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.pay.*;
import org.robovm.apple.foundation.*;
import org.robovm.objc.block.VoidBlock1;
import org.robovm.objc.block.VoidBlock2;
import org.robovm.objc.block.VoidBooleanBlock;
import org.robovm.pods.cocoatouch.storekitrvm.*;
import org.robovm.pods.cocoatouch.storekitrvm.AsyncSequence.AsyncIterator;
import org.robovm.pods.cocoatouch.storekitrvm.Transaction;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The purchase manager implementation for Apple's iOS IAP system using StoreKit2 (RoboVM).
 *
 * @author HD_92 (BlueRiverInteractive)
 * @author noblemaster
 * @author alex-dorokhov
 * @author dkimitsa
 */
public class PurchaseManageriOSApple2 implements PurchaseManager {
    private static final String TAG = "GdxPay/AppleIOS";
    private static final boolean LOGDEBUG =
            Boolean.parseBoolean(System.getProperty("gdx.pay.ios.apple.logdebug", "false"));
    private static final int LOGTYPELOG = 0;
    private static final int LOGTYPEERROR = 1;

    private static NSNumberFormatter numberFormatter;

    private PurchaseObserver observer;
    private PurchaseManagerConfig config;

    private CancelableTask startupTransactionRestorer;
    private CancelableTask transactionUpdateObserver;
    private CancelableTask promotionTransactionObserver;
    private CancelableTask productsRequestAndInstall;
    private CancelableTask productsRequestAndPurchase;
    private NSArray<Product> products;
    private Set<String> productIdsEligibleForIntroOffer;

    private final List<com.badlogic.gdx.pay.Transaction> restoredTransactions = new ArrayList<>();

    @Override
    public String storeName() {
        return PurchaseManagerConfig.STORE_NAME_IOS_APPLE;
    }

    /**
     * @param autoFetchInformation is not used, because without product information on ios it's not possible to fill
     *                             {@link Transaction} object on successful purchase
     **/
    @Override
    public void install(PurchaseObserver observer, PurchaseManagerConfig config, boolean autoFetchInformation) {
        this.observer = observer;
        this.config = config;

        log(LOGTYPELOG, "Installing purchase observer...");

        // Check if the device is configured for purchases.
        if (AppStore.canMakePayments()) {
            // Create string set from offer identifiers.
            int size = config.getOfferCount();
            Set<String> productIdentifiers = new HashSet<String>(size);
            for (int i = 0; i < size; i++) {
                productIdentifiers.add(config.getOffer(i).getIdentifierForStore(PurchaseManagerConfig.STORE_NAME_IOS_APPLE));
            }

            // Request configured offers/products.
            log(LOGTYPELOG, "Requesting products...");
            productsRequestAndInstall = getProducts(productIdentifiers, new FetchProductsAndInstallDelegate(), true);
        } else {
            log(LOGTYPEERROR, "Error setting up in-app-billing: Device not configured for purchases!");
            observer.handleInstallError(new GdxPayException(
                    "Error installing purchase observer: Device not configured for purchases!"));
        }
    }

    @Override
    public boolean installed() {
        return promotionTransactionObserver != null;
    }

    @Override
    public void dispose() {
        if (startupTransactionRestorer != null)
            startupTransactionRestorer.cancel();
        if (transactionUpdateObserver != null)
            transactionUpdateObserver.cancel();
        if (promotionTransactionObserver != null)
            promotionTransactionObserver.cancel();
        if (productsRequestAndInstall != null)
            productsRequestAndInstall.cancel();
        if (productsRequestAndPurchase != null)
            productsRequestAndPurchase.cancel();

        startupTransactionRestorer = null;
        transactionUpdateObserver = null;
        promotionTransactionObserver = null;
        productsRequestAndInstall = null;
        productsRequestAndPurchase = null;
        products = null;
        restoredTransactions.clear();
        if (productIdsEligibleForIntroOffer != null) {
            productIdsEligibleForIntroOffer.clear();
            productIdsEligibleForIntroOffer = null;
        }

        observer = null;
        config = null;

        log(LOGTYPELOG, "Disposed purchase manager!");
    }

    @Override
    public void purchase(final String identifier) {
        // Find the SKProduct for this identifier.
        String identifierForStore = config.getOffer(identifier).getIdentifierForStore(PurchaseManagerConfig.STORE_NAME_IOS_APPLE);
        Product product = getProductByStoreIdentifier(identifierForStore);
        if (product == null) {
            // Product with this identifier not found: load product info first and try to purchase again
            log(LOGTYPELOG, "Requesting product info for " + identifierForStore);

            if (productsRequestAndPurchase != null) productsRequestAndPurchase.cancel();
            productsRequestAndPurchase = getProducts(Collections.singleton(identifierForStore), new FetchProductAndPurchaseDelegate(), false);
        } else {
            // Create a SKPayment from the product and start purchase flow
            log(LOGTYPELOG, "Purchasing product " + identifier + " ...");
            product.purchase(new NSSet<Product.PurchaseOption>(), new VoidBlock2<Product.PurchaseResult, NSError>() {
                @Override
                public void invoke(Product.PurchaseResult purchaseResult, NSError nsError) {
                    if (purchaseResult != null) {
                        log(LOGTYPELOG, "Purchasing product " + identifier + " complete " + purchaseResult);

                        if (purchaseResult instanceof Product.PurchaseResult.success) {
                            Product.PurchaseResult.success success = (Product.PurchaseResult.success) purchaseResult;
                            // Product was successfully purchased.
                            final Transaction transaction = success.getTransaction().getUnsafePayloadValue();
                            // Parse transaction data.
                            final com.badlogic.gdx.pay.Transaction t = transaction(transaction);
                            if (t == null)
                                observer.handlePurchaseError(new GdxPayException("Failed to create GdxPay transaction"));
                            else
                                observer.handlePurchase(t);
                        } else if (purchaseResult == Product.PurchaseResult.userCancelled()) {
                            observer.handlePurchaseCanceled();
                        } else {
                            // should not happen
                            observer.handlePurchaseError(new GdxPayException("Unexpected purchase result " + purchaseResult));
                        }
                    } else {
                        String message = "Purchasing product " + identifier + " failed with error: " + nsError;
                        log(LOGTYPEERROR, message);
                        observer.handlePurchaseError(new GdxPayException(message));
                    }
                }
            });
        }
    }

    @Override
    public void purchaseRestore() {
        log(LOGTYPELOG, "Restoring purchases...");

        // Clear previously restored transactions.
        restoredTransactions.clear();
        // Start the restore flow.
        AppStore.sync(new VoidBlock1<NSError>() {
            @Override
            public void invoke(NSError nsError) {
                if (nsError != null) {
                    // Decide if user cancelled or transaction failed.
                    if (nsError.getCode() == StoreKitError.UserCancelled.value()) {
                        log(LOGTYPEERROR, "Restoring of transactions was cancelled by user!");
                        observer.handleRestoreError(new GdxPayException("Restoring of purchases was cancelled by user!"));
                    } else {
                        String message = "Restoring of transactions failed: " + nsError.toString();
                        log(LOGTYPEERROR, message);
                        observer.handleRestoreError(new GdxPayException(message));
                    }
                }
            }
        });
    }

    /**
     * Override this method in an own subclass if you need to change the default behaviour for promotional
     * App Store payments. The default behaviour adds the store payment to the payment queue and processes
     * it as soon as the product information is available.
     */
    @SuppressWarnings("WeakerAccess")
    protected boolean shouldProcessPromotionalStorePayment(String productId) {
        return true;
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
            if (e != null) System.err.println('[' + TAG + "] " + e);
        }
    }

    //
    // ---------------  Utilities  ---------------
    //

    /**
     * @return StoreKit2 Product object by its id (if already loaded)
     */
    private Product getProductByStoreIdentifier(String identifierForStore) {
        if (products == null) return null;
        for (Product product : products) {
            if (product.getId().equals(identifierForStore)) {
                return product;
            }
        }
        return null;
    }

    /**
     * @return the original and unique transaction ID of a purchase.
     */
    private String getOriginalTxID(Transaction transaction) {
        if (transaction != null) {
            return String.valueOf(transaction.getOriginalID());
        } else {
            // transaction object was 'null': we shouldn't generally get here
            return null;
        }
    }

    /**
     * Converts a purchase to our transaction object.
     */
    @Nullable
    com.badlogic.gdx.pay.Transaction transaction(Transaction t) {
        String productIdentifier = t.getProductID();
        Product product = getProductByStoreIdentifier(productIdentifier);
        if (product == null) {
            // if we didn't request product information -OR- it's not in iTunes, it will be null
            System.err.println("gdx-pay: product not registered/loaded: " + productIdentifier);
        }

        // Build the transaction from the payment transaction object.
        com.badlogic.gdx.pay.Transaction transaction = new com.badlogic.gdx.pay.Transaction();

        Offer offerForStore = config.getOfferForStore(PurchaseManagerConfig.STORE_NAME_IOS_APPLE, productIdentifier);
        if (offerForStore == null) {
            System.err.println("Product not configured in PurchaseManagerConfig: " + productIdentifier + ", skipping transaction.");
            return null;
        }

        transaction.setIdentifier(offerForStore.getIdentifier());

        transaction.setStoreName(PurchaseManagerConfig.STORE_NAME_IOS_APPLE);
        transaction.setOrderId(getOriginalTxID(t));

        transaction.setPurchaseTime(t.getPurchaseDate() != null ? t.getPurchaseDate().toDate() : new Date());
        if (product != null) {
            // if we didn't load product information, product will be 'null' (we only set if available)
            transaction.setPurchaseText("Purchased: " + product.getDisplayName());
            transaction.setPurchaseCost((int) Math.round(product.getPrice().doubleValue() * 100));
            transaction.setPurchaseCostCurrency(product.getPriceFormatStyle().getCurrencyCode());
        } else {
            // product information was empty (not loaded or product didn't exist)
            transaction.setPurchaseText("Purchased: " + productIdentifier);
            transaction.setPurchaseCost(0);
            transaction.setPurchaseCostCurrency(null);
        }

        transaction.setReversalTime(null);  // no refunds for iOS!
        transaction.setReversalText(null);

        // there is no SKPaymentTransaction.payment.requestData alternative in StoreKit2
        transaction.setTransactionData(null);

        // NOTE: although deprecated as of iOS 7, "transactionReceipt" is still available as of iOS 9 & hopefully long there after :)
        String transactionDataSignature;
        try {
            NSData transactionReceipt = t.getJsonRepresentation();
            transactionDataSignature = transactionReceipt.toBase64EncodedString(NSDataBase64EncodingOptions.None);
        } catch (Throwable e) {
            log(LOGTYPELOG, "Transaction.jsonRepresentation appears broken", e);
            transactionDataSignature = null;
        }
        transaction.setTransactionDataSignature(transactionDataSignature);

        // return the transaction
        return transaction;
    }

    @Override
    public Information getInformation(String identifier) {
        if (products != null) {
            for (Product p : products) {
                if (p.getId().equals(identifier)) {
                    if (numberFormatter == null) {
                        numberFormatter = new NSNumberFormatter();
                        numberFormatter.setFormatterBehavior(NSNumberFormatterBehavior._10_4);
                        numberFormatter.setNumberStyle(NSNumberFormatterStyle.Currency);
                    }
                    numberFormatter.setLocale(p.getPriceFormatStyle().getLocale());
                    return Information.newBuilder()
                            .localName(p.getDisplayName())
                            .localDescription(p.getProductDescription())
                            .localPricing(numberFormatter.format(p.getPrice()))
                            .priceCurrencyCode(p.getPriceFormatStyle().getLocale().getCurrencyCode())
                            .priceInCents(MathUtils.ceilPositive(p.getPrice().floatValue() * 100))
                            .priceAsDouble(p.getPrice().doubleValue())
                            .freeTrialPeriod(convertToFreeTrialPeriod(p))
                            .build();
                }
            }
        }
        return Information.UNAVAILABLE;
    }

    private FreeTrialPeriod convertToFreeTrialPeriod(Product product) {
        // fill only for eligible products
        if (productIdsEligibleForIntroOffer == null || !productIdsEligibleForIntroOffer.contains(product.getId()))
            return null;

        if (product.getSubscription() == null) {
            return null;
        }

        final Product.SubscriptionOffer introductoryOffer = product.getSubscription().getIntroductoryOffer();
        if (introductoryOffer == null || introductoryOffer.getPeriod() == null || introductoryOffer.getPeriodCount() == 0) {
            return null;
        }

        if (introductoryOffer.getPrice() != null && introductoryOffer.getPrice().doubleValue() > 0D) {
            // in that case, it is not a free trial. We do not yet support reduced price introductory offers.
            return null;
        }

        final Product.SubscriptionPeriod subscriptionPeriod = introductoryOffer.getPeriod();
        // convert period unit
        FreeTrialPeriod.PeriodUnit periodUnit;
        switch (subscriptionPeriod.getUnit()) {
            case Day:
                periodUnit = FreeTrialPeriod.PeriodUnit.DAY;
                break;
            case Week:
                periodUnit = FreeTrialPeriod.PeriodUnit.WEEK;
                break;
            case Month:
                periodUnit = FreeTrialPeriod.PeriodUnit.MONTH;
                break;
            case Year:
                periodUnit = FreeTrialPeriod.PeriodUnit.YEAR;
                break;
            default:
                periodUnit = null;
                break;
        }
        if (periodUnit == null) {
            return null;
        }

        return new FreeTrialPeriod((int) introductoryOffer.getPeriodCount(), periodUnit);
    }


    //
    // ---------------  Delegates  ---------------
    //

    /// Fetch product delegate -- save received products and install observers
    /// considers manager ready to go and notifies observer by calling observer.handleInstall();
    private class FetchProductsAndInstallDelegate implements FetchProductsDelegate {
        @Override
        public void didReceiveResponse(NSArray<Product> products, Set<String> freeTrialEligible) {
            PurchaseManageriOSApple2.this.products = products;
            if (freeTrialEligible != null && !freeTrialEligible.isEmpty()) {
                if (PurchaseManageriOSApple2.this.productIdsEligibleForIntroOffer == null)
                    PurchaseManageriOSApple2.this.productIdsEligibleForIntroOffer = new HashSet<>();
                PurchaseManageriOSApple2.this.productIdsEligibleForIntroOffer.addAll(freeTrialEligible);
            }

            // Received the registered products from AppStore.
            log(LOGTYPELOG, "Products successfully received!");

            // install all observers
            if (startupTransactionRestorer == null) {
                // restore completed transactions
                startupTransactionRestorer = getCurrentEntitlements(new AppleRestoreTransactionDelegate());
                log(LOGTYPELOG, "Startup purchase transaction restore started!");
            }

            if (transactionUpdateObserver == null) {
                // observe for transaction updates
                transactionUpdateObserver = observeTransactionUpdates(new AppleTransactionUpdateObserver());
            }

            if (promotionTransactionObserver == null) {
                // observe promotion transactions
                promotionTransactionObserver = observePromotionTransactions();
            }


            // notify of success...
            observer.handleInstall();
        }

        @Override
        public void didFail(NSError error) {
            String errorMessage = "Error requesting products: " + (error != null ? error.getLocalizedDescription() : "unknown");
            log(LOGTYPEERROR, errorMessage);

            // Products request failed (likely due to insuficient network connection).
            observer.handleInstallError(new FetchItemInformationException(errorMessage));
        }
    }

    /// delegate for fetching single product ID and purchase its. called when products are not available yet
    /// during purchase request
    private class FetchProductAndPurchaseDelegate implements FetchProductsDelegate {
        @Override
        public void didReceiveResponse(final NSArray<Product> products, Set<String> freeTrialEligible) {
            // Received the registered products from AppStore.
            if (products.size() == 1) {
                // Create a SKPayment from the product and start purchase flow
                Product product = products.get(0);
                log(LOGTYPELOG, "Product info received/purchasing product " + product.getId() + " ...");
                product.purchase(new NSSet<Product.PurchaseOption>(), new VoidBlock2<Product.PurchaseResult, NSError>() {
                    @Override
                    public void invoke(Product.PurchaseResult purchaseResult, NSError nsError) {

                    }
                });
            } else {
                // wrong product count returned
                String errorMessage = "Error purchasing product (wrong product info count returned: " + products.size() + ")!";
                log(LOGTYPEERROR, errorMessage);
                observer.handlePurchaseError(new GdxPayException(errorMessage));
            }
        }

        @Override
        public void didFail(NSError error) {
            String errorMessage = "Error requesting product info to later purchase: " + (error != null ? error.getLocalizedDescription() : "unknown");
            log(LOGTYPEERROR, errorMessage);
            observer.handlePurchaseError(new GdxPayException(errorMessage));
        }
    }

    ///  delegate for handling events on restoring currently completed transactions
    private class AppleRestoreTransactionDelegate implements CurrentEntitlementsDelegate {
        @Override
        public void onComplete() {
            // All products have been restored.
            log(LOGTYPELOG, "All transactions have been restored!");

            observer.handleRestore(restoredTransactions.toArray(new com.badlogic.gdx.pay.Transaction[0]));
            restoredTransactions.clear();
        }

        @Override
        public void onFailed(NSError error) {
            // Restoration failed.
            log(LOGTYPEERROR, "Restoring of transactions failed: " + error.toString());
            observer.handleRestoreError(new GdxPayException("Restoring of purchases failed: " + error.getLocalizedDescription()));
        }

        @Override
        public Task handleNext(Runnable scheduleNext, VerificationResult.Transaction result) {
            if (result.isVerified()) {
                Transaction transaction = result.getUnsafePayloadValue();
                // A product has been restored.
                // Parse transaction data.
                com.badlogic.gdx.pay.Transaction ta = transaction(transaction);
                if (ta != null) {
                    restoredTransactions.add(ta);
                    log(LOGTYPELOG, "Transaction has been restored: " + getOriginalTxID(transaction));
                }
            }
            return null; // proceed to next item
        }
    }

    ///  observer for transaction update -- finish them
    class AppleTransactionUpdateObserver implements TransactionUpdatesDelegate {
        public void transactionUpdated(VerificationResult.Transaction result) {
            if (result.isVerified()) {
                // Product was successfully purchased.
                final Transaction transaction = result.getUnsafePayloadValue();

                // Parse transaction data.
                final com.badlogic.gdx.pay.Transaction t = transaction(transaction);
                if (t == null) return;

                log(LOGTYPELOG, "Transaction updated: " + getOriginalTxID(transaction));

                // Finish transaction.
                transaction.finish(new Runnable() {
                    @Override
                    public void run() {
                        observer.handlePurchase(t);
                        log(LOGTYPELOG, "Transaction was finished: " + getOriginalTxID(transaction));
                    }
                });
            } else {
                // Purchase not verified.

                // Decide if user cancelled or transaction failed.
                NSError error = result.getError();
                if (error == null) {
                    log(LOGTYPEERROR, "Transaction failed but error-object is null");
                    observer.handlePurchaseError(new GdxPayException("Transaction failed "));
                } else {
                    log(LOGTYPEERROR, "Transaction failed: " + error);
                    observer.handlePurchaseError(new GdxPayException("Transaction failed: " + error.getLocalizedDescription()));
                }
            }
        }
    }

    //
    // ---------------  Internals API  ---------------
    //

    ///  delegate for fetching product info from AppleStore
    private interface FetchProductsDelegate {
        void didReceiveResponse(NSArray<Product> products, Set<String> freeTrialEligible);
        void didFail(NSError error);
    }

    ///  delegate for restoring current(completed) transactions
    private interface CurrentEntitlementsDelegate extends AsyncSequenceWalker<VerificationResult.Transaction> {
    }

    ///  delegate for observing transaction update
    interface TransactionUpdatesDelegate {
        void transactionUpdated(VerificationResult.Transaction result);
    }

    //
    // ---------------  StoreKit2  ---------------
    //

    /// requests products list by identifiers, delivers result through delegate
    private CancelableTask getProducts(
            Collection<String> identifiers,
            final FetchProductsDelegate delegate,
            final boolean fetchEligibleStatus) {
        final CancelableTask task = new CancelableTask(null); // to set later
        Task tsk = Product.getProducts(NSArray.fromStrings(identifiers), new VoidBlock2<NSArray<Product>, NSError>() {
            @Override
            public void invoke(NSArray<Product> products, NSError nsError) {
                if (nsError != null || products == null) delegate.didFail(nsError);
                else if (!fetchEligibleStatus || products.isEmpty()) delegate.didReceiveResponse(products, null);
                else {
                    // either will start another task or call delegate and exit
                    fetchEligibleForIntroOffer(task, products, delegate);
                }
            }
        });
        task.update(tsk);
        return task;
    }

    /// for each product with promotion check if it is eligible for into offer
    private void fetchEligibleForIntroOffer(
            final CancelableTask task,
            final NSArray<Product> products,
            final FetchProductsDelegate delegate
    ) {
        final Set<String> statuses = new HashSet<>();
        class FetchState implements VoidBooleanBlock {
            final Iterator<Product> iter = products.iterator();
            Product next;
            void performFetch() {
                while (iter.hasNext()) {
                    next = iter.next();
                    Product.SubscriptionInfo info = next.getSubscription();
                    if (info != null) {
                        // start task
                        Task tsk = info.isEligibleForIntroOffer(this);
                        task.update(tsk);
                        return;
                    }
                }
                // there is no product left, deliver callback
                delegate.didReceiveResponse(products, statuses);
            }

            @Override
            public void invoke(boolean status) {
                if (status) statuses.add(next.getId());
                performFetch();
            }
        }
        FetchState state = new FetchState();
        state.performFetch();
    }

    ///  Restores current transactions, should be called as early as possible
    private CancelableTask getCurrentEntitlements(final CurrentEntitlementsDelegate delegate) {
        return new AsyncSequenceProcessor<>(Transaction.currentEntitlements(), delegate).start();
    }

    /// setups task for observing transaction updates
    CancelableTask observeTransactionUpdates(final TransactionUpdatesDelegate delegate) {
        return new AsyncSequenceProcessor<>(Transaction.updates(),
                new AsyncSequenceWalker<VerificationResult.Transaction>() {
                    @Override
                    public void onFailed(NSError nsError) {
                        log(LOGTYPELOG, "Unexpected: Transaction.updates() failed with " + nsError.getLocalizedDescription());
                    }

                    @Override
                    public void onComplete() {
                        // should not happen
                        log(LOGTYPELOG, "Unexpected: Transaction.updates() has complete()");
                    }

                    @Override
                    public Task handleNext(Runnable scheduleNext, VerificationResult.Transaction value) {
                        delegate.transactionUpdated(value);
                        return null; // proceed next
                    }
                }
        ).start();
    }

    /// setups task for observing promotion transactions
    CancelableTask observePromotionTransactions() {
        return new AsyncSequenceProcessor<>(PurchaseIntent.intents(), new AsyncSequenceWalker<PurchaseIntent>() {
            @Override
            public void onFailed(NSError nsError) {
                log(LOGTYPEERROR, "Unexpected: Transaction.updates() failed with " + nsError.getLocalizedDescription());
            }

            @Override
            public void onComplete() {
                log(LOGTYPEERROR, "Unexpected: Transaction.updates() has complete()");
            }

            @Override
            public Task handleNext(final Runnable scheduleNext, PurchaseIntent intent) {
                final Product product = intent.getProduct();
                if (shouldProcessPromotionalStorePayment(product.getId())) {
                    // have to complete purchase
                    // move to next iteration
                    return product.purchase(new NSSet<Product.PurchaseOption>(), new VoidBlock2<Product.PurchaseResult, NSError>() {
                        @Override
                        public void invoke(Product.PurchaseResult purchaseResult, NSError nsError) {
                            if (nsError != null) {
                                log(LOGTYPEERROR, "Failed to purchase " + product.getId() + " " +
                                        nsError.getLocalizedDescription());
                            }
                            // move to next iteration
                            scheduleNext.run();
                        }
                    }); // don't start next(), till purchase is complete
                }

                return null; // process next()
            }
        }).start();
    }

    //
    // ---------------  StoreKit2 helpers ---------------
    //

    /// StoreKit2 task holder -- used to track running async tasks abd to be able to cancel it
    private static class CancelableTask {
        Task task;

        CancelableTask(Task task) {
            this.task = task;
        }

        void cancel() {
            task.cancel();
        }

        void update(Task task) {
            this.task = task;
        }
    }

    /// Interface for receiving data from AsyncSequence
    interface AsyncSequenceWalker<T extends NSObject> {
        void onFailed(NSError nsError);

        void onComplete();

        /**
         * @param scheduleNext will schedule next() call, to be used if handler has to perform additional activity.
         *                     in this case handler should return not-null task
         * @return null if processing has been finished (and can proceed with next item) or Task of activity
         * that was started by StoreKit, task object is used to cancel it if required.
         */
        Task handleNext(Runnable scheduleNext, T value);
    }

    /// Base implementation of iterator over AsyncSequence
    private static class AsyncSequenceProcessor<T extends NSObject> implements VoidBlock2<T, NSError> {
        protected CancelableTask task;
        private final AsyncIterator<T> iter;
        private final AsyncSequenceWalker<T> walker;
        private final Runnable scheduleNext = new Runnable() {
            @Override
            public void run() {
                scheduleNext();
            }
        };

        AsyncSequenceProcessor(AsyncSequence<T> seq, AsyncSequenceWalker<T> walker) {
            this.iter = seq.makeAsyncIterator();
            this.walker = walker;
        }

        CancelableTask start() {
            task = new CancelableTask(iter.next(this));
            return task;
        }

        @Override
        public void invoke(T value, NSError nsError) {
            if (nsError != null) walker.onFailed(nsError);
            else if (value == null) walker.onComplete();
            else {
                Task tsk = walker.handleNext(scheduleNext, value);
                if (tsk != null) task.update(tsk);
                else scheduleNext();
            }
        }

        private void scheduleNext() {
            task.update(iter.next(this));
        }
    }
}
