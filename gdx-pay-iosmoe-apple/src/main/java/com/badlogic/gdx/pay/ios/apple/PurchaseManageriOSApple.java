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

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import apple.foundation.NSArray;
import apple.foundation.NSBundle;
import apple.foundation.NSData;
import apple.foundation.NSDate;
import apple.foundation.NSDictionary;
import apple.foundation.NSError;
import apple.foundation.NSJSONSerialization;
import apple.foundation.NSLocale;
import apple.foundation.NSMutableSet;
import apple.foundation.NSMutableURLRequest;
import apple.foundation.NSNumberFormatter;
import apple.foundation.NSOperationQueue;
import apple.foundation.NSString;
import apple.foundation.NSURL;
import apple.foundation.NSURLConnection;
import apple.foundation.NSURLResponse;
import apple.foundation.enums.NSNumberFormatterBehavior;
import apple.foundation.enums.NSNumberFormatterStyle;
import apple.storekit.SKPayment;
import apple.storekit.SKPaymentQueue;
import apple.storekit.SKPaymentTransaction;
import apple.storekit.SKProduct;
import apple.storekit.SKProductsRequest;
import apple.storekit.SKProductsResponse;
import apple.storekit.SKReceiptRefreshRequest;
import apple.storekit.SKRequest;
import apple.storekit.enums.SKErrorCode;
import apple.storekit.enums.SKPaymentTransactionState;
import apple.storekit.protocol.SKPaymentTransactionObserver;
import apple.storekit.protocol.SKProductsRequestDelegate;
import apple.storekit.protocol.SKRequestDelegate;

import static apple.foundation.c.Foundation.NSLocaleCurrencyCode;
import static apple.foundation.enums.Enums.NSASCIIStringEncoding;
import static apple.foundation.enums.Enums.NSUTF8StringEncoding;

/** The purchase manager implementation for Apple's iOS IAP system.
 * 
 * <p>
 * To integrate into your iOS project do the following:
 * <ol>
 * <li>add the jar-files to your project's lib directory as follows (IAP will work automatically once the files are present):
 * <ul>
 * <li>gdx-pay.jar: This goes into your "core"/lib project.
 * <li>gdx-pay-client.jar: This goes into your "core"/lib project.
 * <li>gdx-pay-iosmoe.jar: This goes into your "iOS"/lib directory.
 * </ul>
 * </ol>
 * Please note that no code changes for iOS are necessary. As soon as you place the jar files everything will work out of the box
 * (instantiated via reflection).
 * 
 * @author HD_92 (BlueRiverInteractive)
 * @author noblemaster
 * @author alex-dorokhov
 * */
public class PurchaseManageriOSApple implements PurchaseManager, SKPaymentTransactionObserver {

    static {
        try {
            // Fix NatJ runtime class initialization order for binding classes.
            Class.forName(SKPaymentTransaction.class.getName());
            Class.forName(SKProduct.class.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String TAG = "GdxPay/AppleIOS";
    private static final boolean LOGDEBUG = true;
    private static final int LOGTYPELOG = 0;
    private static final int LOGTYPEERROR = 1;

    private static NSNumberFormatter numberFormatter;

    private PurchaseObserver observer;
    private PurchaseManagerConfig config;

    private AppleTransactionObserver appleObserver;
    private SKProductsRequest productsRequest;
    private NSArray<? extends SKProduct> products;

    private final List<Transaction> restoredTransactions = new ArrayList<Transaction>();

    @Override
    public String storeName() {
        return PurchaseManagerConfig.STORE_NAME_IOS_APPLE;
    }

	/**
     *  @param autoFetchInformation is not used, because without product information on ios it's not possible to fill
     *  {@link Transaction} object on successful purchase
     **/
    @Override
    public void install(PurchaseObserver observer, PurchaseManagerConfig config, boolean
            autoFetchInformation) {
        this.observer = observer;
        this.config = config;

        log(LOGTYPELOG, "Installing purchase observer...");

        // Check if the device is configured for purchases.
        if (SKPaymentQueue.canMakePayments()) {
            // Create string set from offer identifiers.
            int size = config.getOfferCount();
            NSMutableSet<String> productIdentifiers = (NSMutableSet<String>) NSMutableSet.alloc()
                    .initWithCapacity(size);
            for (int i = 0; i < size; i++) {
                productIdentifiers.addObject(config.getOffer(i).getIdentifierForStore
                        (PurchaseManagerConfig.STORE_NAME_IOS_APPLE));
            }

            // Request configured offers/products.
            log(LOGTYPELOG, "Requesting products...");
            productsRequest = SKProductsRequest.alloc().initWithProductIdentifiers
                    (productIdentifiers);
            productsRequest.setDelegate(new IosFetchProductsAndInstallDelegate());
            productsRequest.start();
        } else {
            log(LOGTYPEERROR, "Error setting up in-app-billing: Device not configured for " +
                    "purchases!");
            observer.handleInstallError(new RuntimeException("Error installing purchase observer:" +
                    " Device not configured for purchases!"));
        }
    }

    @Override
    public boolean installed() {
        return appleObserver != null;
    }

    @Override
    public void dispose() {
        if (appleObserver != null) {
            // Remove and null our apple transaction observer.
            ((SKPaymentQueue) SKPaymentQueue.defaultQueue()).removeTransactionObserver(appleObserver);
            appleObserver = null;
            productsRequest = null;
            products = null;
            restoredTransactions.clear();

            observer = null;
            config = null;

            log(LOGTYPELOG, "Disposed purchase manager!");
        }
    }

    @Override
    public void purchase(String identifier) {
        // Find the SKProduct for this identifier.
        Offer offer = config.getOffer(identifier);
        if (offer == null) {
            log(LOGTYPEERROR, "Invalid product identifier, " + identifier);
            observer.handlePurchaseError(new RuntimeException("Invalid product identifier, " + identifier));
        } else {
            String identifierForStore = offer.getIdentifierForStore
                    (PurchaseManagerConfig.STORE_NAME_IOS_APPLE);
            SKProduct product = getProductByStoreIdentifier(identifierForStore);
            if (product == null) {
                // Product with this identifier not found: load product info first and try to purchase again
                log(LOGTYPELOG, "Requesting product info for " + identifierForStore);
                NSMutableSet<String> identifierForStoreSet = (NSMutableSet<String>) NSMutableSet
                        .alloc().initWithCapacity(1);
                identifierForStoreSet.addObject(identifierForStore);
                productsRequest = SKProductsRequest.alloc().initWithProductIdentifiers
                        (identifierForStoreSet);
                productsRequest.setDelegate(new AppleProductsDelegatePurchase());
                productsRequest.start();
            } else {
                // Create a SKPayment from the product and start purchase flow
                log(LOGTYPELOG, "Purchasing product " + identifier + " ...");
                SKPayment payment = SKPayment.paymentWithProduct(product);
                ((SKPaymentQueue) SKPaymentQueue.defaultQueue()).addPayment(payment);
            }
        }
    }

    @Override
    public void purchaseRestore() {
        log(LOGTYPELOG, "Restoring purchases...");

        // Clear previously restored transactions.
        restoredTransactions.clear();
        // Start the restore flow.
        ((SKPaymentQueue) SKPaymentQueue.defaultQueue()).restoreCompletedTransactions();
    }

    SKProduct getProductByStoreIdentifier(String identifierForStore) {
        if (products == null)
            return null;
        for (SKProduct product : products) {
            if (product.productIdentifier().equals(identifierForStore)) {
                return product;
            }
        }
        return null;
    }

    /** Returns the original and unique transaction ID of a purchase. */
    private String getOriginalTxID(SKPaymentTransaction transaction) {
        if (transaction != null) {
            if (transaction.originalTransaction() != null) {
                // the "original" transaction ID is 'null' for first time purchases but non-'null' for restores (it's unique!)
                return transaction.originalTransaction().transactionIdentifier();
            } else {
                // the regular transaction idetifier. This one changes every time if a product is restored!!
                return transaction.transactionIdentifier();
            }
        } else {
            // transaction object was 'null': we shouldn't generally get here
            return null;
        }
    }

    /** Converts a purchase to our transaction object. */
    @Nullable
    Transaction transaction(SKPaymentTransaction t) {
        SKPayment payment = t.payment();
        String productIdentifier = payment.productIdentifier();
        SKProduct product = getProductByStoreIdentifier(productIdentifier);
        if (product == null) {
            // if we didn't request product information -OR- it's not in iTunes, it will be null
            System.err.println("gdx-pay: product not registered/loaded: " + productIdentifier);
        }

        // Build the transaction from the payment transaction object.
        Transaction transaction = new Transaction();

        Offer offerForStore = config.getOfferForStore(PurchaseManagerConfig.STORE_NAME_IOS_APPLE,
                productIdentifier);
        if (offerForStore == null) {
            System.err.println("Product not configured in PurchaseManagerConfig: " +
                    productIdentifier + ", skipping transaction.");
            return null;
        }

        transaction.setIdentifier(offerForStore.getIdentifier());

        transaction.setStoreName(PurchaseManagerConfig.STORE_NAME_IOS_APPLE);
        transaction.setOrderId(getOriginalTxID(t));

        transaction.setPurchaseTime(toJavaDate(t.transactionDate()));
        if (product != null) {
            // if we didn't load product information, product will be 'null' (we only set if available)
            transaction.setPurchaseText("Purchased: " + product.localizedTitle());
            transaction.setPurchaseCost((int) Math.round(product.price().doubleValue() * 100));
            NSLocale locale = product.priceLocale();
            transaction.setPurchaseCostCurrency((String) locale.objectForKey(NSLocaleCurrencyCode()));
        } else {
            // product information was empty (not loaded or product didn't exist)
            transaction.setPurchaseText("Purchased: " + productIdentifier);
            transaction.setPurchaseCost(0);
            transaction.setPurchaseCostCurrency(null);
        }

        transaction.setReversalTime(null);  // no refunds for iOS!
        transaction.setReversalText(null);

        if (payment.requestData() != null) {
            final String transactionData;
            transactionData = payment.requestData().base64EncodedStringWithOptions(0);
            transaction.setTransactionData(transactionData);
        } else {
            transaction.setTransactionData(null);
        }

        // return the transaction
        return transaction;
    }

    private Date toJavaDate(NSDate nsDate) {
        double sinceEpoch = nsDate.timeIntervalSince1970();
        return new Date((long) (sinceEpoch * 1000));
    }

    private class AppleProductsDelegatePurchase implements SKProductsRequestDelegate,
            SKRequestDelegate {

        @Override
        public void productsRequestDidReceiveResponse(SKProductsRequest request,
                                                      SKProductsResponse response) {
            // Received the registered products from AppStore.
            products = response.products();
            if (products.size() == 1) {
                // Create a SKPayment from the product and start purchase flow
                SKProduct product = products.get(0);
                log(LOGTYPELOG, "Product info received/purchasing product " + product
                        .productIdentifier() + " ...");
                SKPayment payment = SKPayment.paymentWithProduct(product);
                ((SKPaymentQueue) SKPaymentQueue.defaultQueue()).addPayment(payment);
            } else {
                // wrong product count returned
                String errorMessage = "Error purchasing product (wrong product info count " +
                        "returned: " + products.size() + ")!";
                log(LOGTYPEERROR, errorMessage);
                observer.handlePurchaseError(new RuntimeException(errorMessage));
            }
        }

        @Override
        public void requestDidFailWithError(SKRequest request, NSError error) {
            String errorMessage = "Error requesting product info to later purchase: " + (error !=
                    null ? error.toString() : "unknown");
            log(LOGTYPEERROR, errorMessage);
            observer.handlePurchaseError(new RuntimeException(errorMessage));
        }
    }

    private class IosFetchProductsAndInstallDelegate implements SKProductsRequestDelegate,
            SKRequestDelegate {

        @Override
        public void productsRequestDidReceiveResponse(SKProductsRequest request,
                                                      SKProductsResponse response) {
            // Received the registered products from AppStore.
            products = response.products();
            log(LOGTYPELOG, products.size() + " products successfully received");

            // Parse valid products
            if (products != null && !products.isEmpty()) {
                Iterator<? extends SKProduct> it = products.iterator();
                while (it.hasNext()) {
                    log(LOGTYPELOG, it.next().productIdentifier());
                }
            }

            // Parse invalid products
            NSArray<String> invalids = response.invalidProductIdentifiers();
            if (invalids != null && !invalids.isEmpty()) {
                Iterator<String> it = invalids.iterator();
                while (it.hasNext()) {
                    log(LOGTYPEERROR, "Invalid product received, " + it.next());
                }
            }

            final SKPaymentQueue defaultQueue = (SKPaymentQueue) SKPaymentQueue.defaultQueue();

            // Create and register our apple transaction observer.
            appleObserver = AppleTransactionObserver.alloc().init();
            appleObserver.purchaseManageriOSApple = PurchaseManageriOSApple.this;
            defaultQueue.addTransactionObserver(appleObserver);
            log(LOGTYPELOG, "Purchase observer successfully installed!");

            // notify of success...
            observer.handleInstall();

            // complete unfinished transactions
            final NSArray<? extends SKPaymentTransaction> transactions = defaultQueue
                    .transactions();
            log(LOGTYPELOG, "There are " + transactions.size() + " unfinished transactions. Try " +
                    "to finish...");
            appleObserver.paymentQueueUpdatedTransactions(defaultQueue, transactions);
        }

        @Override
        public void requestDidFailWithError(SKRequest request, NSError error) {
            log(LOGTYPEERROR, "Error requesting products: " + (error != null ? error.toString() :
                    "unknown"));
            // Products request failed (likely due to insuficient network connection).
            observer.handleInstallError(new RuntimeException("Error requesting products: " +
                    (error != null ? error.toString() : "unknown")));
        }
    }

    void log(final int type, final String message) {
        log(type, message, null);
    }

    void log(final int type, final String message, Throwable e) {
        if (LOGDEBUG) {
            if (type == LOGTYPELOG)
                System.out.println('[' + TAG + "] " + message);
            if (type == LOGTYPEERROR)
                System.err.println('[' + TAG + "] " + message);
            if (e != null)
                System.err.println('[' + TAG + "] " + e);
        }
    }

    @Override
    public Information getInformation(String identifier) {
        if (products != null) {
            for (SKProduct p : products) {
                if (p.productIdentifier().equals(identifier)) {
                    if (numberFormatter == null) {
                        numberFormatter = NSNumberFormatter.alloc().init();
                        numberFormatter.setFormatterBehavior(NSNumberFormatterBehavior
                                .Behavior10_4);
                        numberFormatter.setNumberStyle(NSNumberFormatterStyle.CurrencyStyle);
                    }
                    numberFormatter.setLocale(p.priceLocale());
                    return new Information(p.localizedTitle(), p.localizedDescription(),
                            numberFormatter.stringFromNumber(p.price()));
                }
            }
        }
        return Information.UNAVAILABLE;
    }

    @Override
    public String toString() {
        return PurchaseManagerConfig.STORE_NAME_IOS_APPLE;                // FIXME: shouldnt this be PurchaseManagerConfig.STORE_NAME_IOS_APPLE or storeName() ??!!
    }

    /*
    SKPaymentTransactionObserver
     */

    @Override
    public void paymentQueueUpdatedTransactions(SKPaymentQueue queue, NSArray<? extends
            SKPaymentTransaction> transactions) {
        for (final SKPaymentTransaction transaction : transactions) {
            long state = transaction.transactionState();
            switch ((int) state) {
                case (int) SKPaymentTransactionState.Purchased:
                    // Product was successfully purchased.

                    // Parse transaction data.
                    final Transaction t = transaction(transaction);
                    if (t == null) {
                        break;
                    }

                    // Find transaction receipt if not set, i.e. t.setTransactionDataSignature() == null
                    // NOTE: - as long as SKPaymentTransaction.transactionReceipt is not removed but only deprecated, let's use it
                    //       - FIXME: the function below sends ALL receipts, not just the one we need: we need to parse it out (does NOT work right now)!
                    //       - FIXME: the function below should be added also to restore(): this only gets used for direct-purchases ONLY!
                    //       - parsing "NSBundle.getMainBundle().getAppStoreReceiptURL();": https://developer.apple.com/library/ios/releasenotes/General/ValidateAppStoreReceipt/Chapters/ValidateLocally.html#//apple_ref/doc/uid/TP40010573-CH1-SW19
                    //         parsing is not for the faint of heart: lots of low-level coding :-/
                    //            --> we will use the deprecated feature as long as we can or a volunteer comes forward!
                    //            --> Apple might also provider better tooling support to parse out specific receipts in the future (yeah, that's going to happen...)
                    if (t.getTransactionDataSignature() == null) {
                        NSURL receiptURL = NSBundle.mainBundle().appStoreReceiptURL();

                        NSData receipt = NSData.dataWithContentsOfURL(receiptURL);
                        if (receipt == null) {
                            log(LOGTYPELOG, "Fetching receipt...");
                            final SKReceiptRefreshRequest request = SKReceiptRefreshRequest.alloc().init();
                            request.setDelegate(new SKRequestDelegate() {

                                @Override
                                public void requestDidFinish(SKRequest r) {
                                    // Receipt refresh request finished.
                                    if (r.equals(request)) {
                                        NSURL receiptURL = NSBundle.mainBundle()
                                                .appStoreReceiptURL();
                                        NSData receipt = NSData.dataWithContentsOfURL(receiptURL);
                                        String encodedReceipt = receipt
                                                .base64EncodedStringWithOptions(0);

                                        // FIXME: parse out actual receipt for this IAP purchase:
                                        t.setTransactionDataSignature(encodedReceipt);
                                        log(LOGTYPELOG, "Receipt was fetched!");
                                    } else {
                                        log(LOGTYPEERROR, "Receipt fetching failed: Request " +
                                                "doesn't equal initial request!");
                                    }
                                    log(LOGTYPELOG, "Transaction was completed: " +
                                            getOriginalTxID(transaction));
                                    observer.handlePurchase(t);
                                    // Finish transaction.
                                    ((SKPaymentQueue) SKPaymentQueue.defaultQueue())
                                            .finishTransaction(transaction);
                                }

                                @Override
                                public void requestDidFailWithError(SKRequest request, NSError
                                        error) {
                                    // Receipt refresh request failed. Let's just continue.
                                    log(LOGTYPEERROR, "Receipt fetching failed: " + error
                                            .toString());
                                    log(LOGTYPELOG, "Transaction was completed: " +
                                            getOriginalTxID(transaction));
                                    observer.handlePurchase(t);

                                    // Finish transaction.
                                    ((SKPaymentQueue) SKPaymentQueue.defaultQueue())
                                            .finishTransaction(transaction);
                                }
                            });
                            request.start();
                        } else {
                            String encodedReceipt = receipt.base64EncodedStringWithOptions(0);

                            // FIXME: parse out actual receipt for this IAP purchase:
                            t.setTransactionDataSignature(encodedReceipt);


                            log(LOGTYPELOG, "Transaction was completed: " + getOriginalTxID
                                    (transaction));
                            observer.handlePurchase(t);

                            // Finish transaction.
                            ((SKPaymentQueue) SKPaymentQueue.defaultQueue()).finishTransaction
                                    (transaction);

                        }
                    } else {
                        // we are done: let's report!
                        log(LOGTYPELOG, "Transaction was completed: " + getOriginalTxID
                                (transaction));
                        observer.handlePurchase(t);

                        // Finish transaction.
                        ((SKPaymentQueue) SKPaymentQueue.defaultQueue()).finishTransaction
                                (transaction);
                    }
                    break;
                case (int) SKPaymentTransactionState.Failed:
                    // Purchase failed.

                    // Decide if user cancelled or transaction failed.
                    NSError error = transaction.error();
                    if (error == null) {
                        log(LOGTYPEERROR, "Transaction failed but error-object is null: " +
                                transaction);
                        observer.handlePurchaseError(new RuntimeException("Transaction failed: "
                                + transaction));
                    } else if (error.code() == SKErrorCode.PaymentCancelled) {
                        log(LOGTYPEERROR, "Transaction was cancelled by user!");
                        observer.handlePurchaseCanceled();
                    } else {
                        log(LOGTYPEERROR, "Transaction failed: " + error.toString());
                        observer.handlePurchaseError(new RuntimeException("Transaction failed: "
                                + error.toString()));
                    }

                    // Finish transaction.
                    ((SKPaymentQueue) SKPaymentQueue.defaultQueue()).finishTransaction(transaction);
                    break;
                case (int) SKPaymentTransactionState.Restored:
                    // A product has been restored.

                    // Parse transaction data.
                    Transaction ta = transaction(transaction);
                    if (ta == null)
                        break;

                    restoredTransactions.add(ta);

                    // Finish transaction.
                    ((SKPaymentQueue) SKPaymentQueue.defaultQueue()).finishTransaction(transaction);

                    log(LOGTYPELOG, "Transaction has been restored: " + getOriginalTxID
                            (transaction));
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void paymentQueueRestoreCompletedTransactionsFinished(SKPaymentQueue queue) {
        // All products have been restored.
        log(LOGTYPELOG, "All transactions have been restored!");

        observer.handleRestore(restoredTransactions.toArray(new Transaction[restoredTransactions
                .size()]));
        restoredTransactions.clear();
    }

    @Override
    public void paymentQueueRestoreCompletedTransactionsFailedWithError(SKPaymentQueue queue,
                                                                        NSError error) {
        // Restoration failed.

        // Decide if user cancelled or transaction failed.
        if (error.code() == SKErrorCode.PaymentCancelled) {
            log(LOGTYPEERROR, "Restoring of transactions was cancelled by user!");
            observer.handleRestoreError(new RuntimeException("Restoring of purchases was " +
                    "cancelled by user!"));
        } else {
            log(LOGTYPEERROR, "Restoring of transactions failed: " + error.toString());
            observer.handleRestoreError(new RuntimeException("Restoring of purchases failed: " +
                    error.toString()));
        }
    }
}
