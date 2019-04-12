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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.badlogic.gdx.pay.FetchItemInformationException;
import com.badlogic.gdx.pay.GdxPayException;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;
import libcore.io.Base64;
import org.robovm.apple.foundation.Foundation;
import org.robovm.apple.foundation.NSArray;
import org.robovm.apple.foundation.NSBundle;
import org.robovm.apple.foundation.NSData;
import org.robovm.apple.foundation.NSDataBase64EncodingOptions;
import org.robovm.apple.foundation.NSError;
import org.robovm.apple.foundation.NSNumberFormatter;
import org.robovm.apple.foundation.NSNumberFormatterBehavior;
import org.robovm.apple.foundation.NSNumberFormatterStyle;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.storekit.SKErrorCode;
import org.robovm.apple.storekit.SKPayment;
import org.robovm.apple.storekit.SKPaymentQueue;
import org.robovm.apple.storekit.SKPaymentTransaction;
import org.robovm.apple.storekit.SKPaymentTransactionObserverAdapter;
import org.robovm.apple.storekit.SKPaymentTransactionState;
import org.robovm.apple.storekit.SKProduct;
import org.robovm.apple.storekit.SKProductsRequest;
import org.robovm.apple.storekit.SKProductsRequestDelegateAdapter;
import org.robovm.apple.storekit.SKProductsResponse;
import org.robovm.apple.storekit.SKReceiptRefreshRequest;
import org.robovm.apple.storekit.SKRequest;
import org.robovm.apple.storekit.SKRequestDelegateAdapter;

/** The purchase manager implementation for Apple's iOS IAP system (RoboVM).
 *
 * @author HD_92 (BlueRiverInteractive)
 * @author noblemaster
 * @author alex-dorokhov
 * */
public class PurchaseManageriOSApple implements PurchaseManager {
    private static final String TAG = "GdxPay/AppleIOS";
    private static final boolean LOGDEBUG = true;
    private static final int LOGTYPELOG = 0;
    private static final int LOGTYPEERROR = 1;

    private static NSNumberFormatter numberFormatter;

    private PurchaseObserver observer;
    private PurchaseManagerConfig config;

    private AppleTransactionObserver appleObserver;
    private PromotionTransactionObserver startupTransactionObserver;
    private SKProductsRequest productsRequest;
    private NSArray<SKProduct> products;

    private final List<Transaction> restoredTransactions = new ArrayList<Transaction>();

    /**
     * set to false if you don't accept payments that were initiated from Apple App Store Promotions
     */
    public static boolean addStorePayments = true;

    @Override
    public String storeName () {
        return PurchaseManagerConfig.STORE_NAME_IOS_APPLE;
    }

	/**
     *  @param autoFetchInformation is not used, because without product information on ios it's not possible to fill
     *  {@link Transaction} object on successful purchase
     **/
    @Override
	public void install (PurchaseObserver observer, PurchaseManagerConfig config, boolean autoFetchInformation) {
        this.observer = observer;
        this.config = config;

        log(LOGTYPELOG, "Installing purchase observer...");

        // Check if the device is configured for purchases.
        if (SKPaymentQueue.canMakePayments()) {
            // Create string set from offer identifiers.
            int size = config.getOfferCount();
            Set<String> productIdentifiers = new HashSet<String>(size);
            for (int i = 0; i < size; i++) {
                productIdentifiers.add(config.getOffer(i).getIdentifierForStore(PurchaseManagerConfig.STORE_NAME_IOS_APPLE));
            }

            if (appleObserver == null) {
                // Installing intermediate observer to handle App Store promotions
                startupTransactionObserver = new PromotionTransactionObserver();
                final SKPaymentQueue defaultQueue = SKPaymentQueue.getDefaultQueue();
                defaultQueue.addTransactionObserver(appleObserver);
                defaultQueue.addStrongRef(appleObserver);
                log(LOGTYPELOG, "Startup purchase observer successfully installed!");
            }

            // Request configured offers/products.
            log(LOGTYPELOG, "Requesting products...");
            productsRequest = new SKProductsRequest(productIdentifiers);
            productsRequest.setDelegate(new IosFetchProductsAndInstallDelegate());
            productsRequest.start();
        } else {
            log(LOGTYPEERROR, "Error setting up in-app-billing: Device not configured for purchases!");
            observer.handleInstallError(new GdxPayException(
                "Error installing purchase observer: Device not configured for purchases!"));
        }
    }

    @Override
    public boolean installed () {
        return appleObserver != null;
    }

    @Override
    public void dispose () {
        if (appleObserver != null) {
            // Remove and null our apple transaction observer.

            SKPaymentQueue defaultQueue = SKPaymentQueue.getDefaultQueue();
            defaultQueue.removeTransactionObserver(appleObserver);
            defaultQueue.removeStrongRef(appleObserver);

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
    public void purchase (String identifier) {
        // Find the SKProduct for this identifier.
        String identifierForStore = config.getOffer(identifier).getIdentifierForStore(PurchaseManagerConfig.STORE_NAME_IOS_APPLE);
        SKProduct product = getProductByStoreIdentifier(identifierForStore);
        if (product == null) {
            // Product with this identifier not found: load product info first and try to purchase again
            log(LOGTYPELOG, "Requesting product info for " + identifierForStore);
            Set<String> identifierForStoreSet = new HashSet<String>(1);
            identifierForStoreSet.add(identifierForStore);
            productsRequest = new SKProductsRequest(identifierForStoreSet);
            productsRequest.setDelegate(new AppleProductsDelegatePurchase());
            productsRequest.start();
        }
        else {
            // Create a SKPayment from the product and start purchase flow
            log(LOGTYPELOG, "Purchasing product " + identifier + " ...");
            SKPayment payment = new SKPayment(product);
            SKPaymentQueue.getDefaultQueue().addPayment(payment);
        }
    }

    @Override
    public void purchaseRestore () {
        log(LOGTYPELOG, "Restoring purchases...");

        // Clear previously restored transactions.
        restoredTransactions.clear();
        // Start the restore flow.
        SKPaymentQueue.getDefaultQueue().restoreCompletedTransactions();
    }

    SKProduct getProductByStoreIdentifier (String identifierForStore) {
        if (products == null) return null;
        for (SKProduct product : products) {
            if (product.getProductIdentifier().equals(identifierForStore)) {
                return product;
            }
        }
        return null;
    }

    /** Returns the original and unique transaction ID of a purchase. */
    private String getOriginalTxID(SKPaymentTransaction transaction) {
        if (transaction != null) {
            if (transaction.getOriginalTransaction() != null) {
                // the "original" transaction ID is 'null' for first time purchases but non-'null' for restores (it's unique!)
                return transaction.getOriginalTransaction().getTransactionIdentifier();
            } else {
                // the regular transaction idetifier. This one changes every time if a product is restored!!
                return transaction.getTransactionIdentifier();
            }
        }
        else {
            // transaction object was 'null': we shouldn't generally get here
            return null;
        }
    }

    /** Converts a purchase to our transaction object. */
    @Nullable
    Transaction transaction (SKPaymentTransaction t) {
        SKPayment payment = t.getPayment();
        String productIdentifier = payment.getProductIdentifier();
        SKProduct product = getProductByStoreIdentifier(productIdentifier);
        if (product == null) {
            // if we didn't request product information -OR- it's not in iTunes, it will be null
            System.err.println("gdx-pay: product not registered/loaded: " + productIdentifier);
        }

        // Build the transaction from the payment transaction object.
        Transaction transaction = new Transaction();

        Offer offerForStore = config.getOfferForStore(PurchaseManagerConfig.STORE_NAME_IOS_APPLE, productIdentifier);
        if (offerForStore == null) {
            System.err.println("Product not configured in PurchaseManagerConfig: " + productIdentifier + ", skipping transaction.");
            return null;
        }

        transaction.setIdentifier(offerForStore.getIdentifier());

        transaction.setStoreName(PurchaseManagerConfig.STORE_NAME_IOS_APPLE);
        transaction.setOrderId(getOriginalTxID(t));

        transaction.setPurchaseTime(t.getTransactionDate().toDate());
        if (product != null) {
            // if we didn't load product information, product will be 'null' (we only set if available)
            transaction.setPurchaseText("Purchased: " + product.getLocalizedTitle());
            transaction.setPurchaseCost((int) Math.round(product.getPrice().doubleValue() * 100));
            transaction.setPurchaseCostCurrency(product.getPriceLocale().getCurrencyCode());
        }
        else {
            // product information was empty (not loaded or product didn't exist)
            transaction.setPurchaseText("Purchased: " + productIdentifier);
            transaction.setPurchaseCost(0);
            transaction.setPurchaseCostCurrency(null);
        }

        transaction.setReversalTime(null);  // no refunds for iOS!
        transaction.setReversalText(null);

        if (payment.getRequestData() != null) {
            final String transactionData;
            if (Foundation.getMajorSystemVersion() >= 7) {
                transactionData = payment.getRequestData().toBase64EncodedString(NSDataBase64EncodingOptions.None);
            } else {
                transactionData = Base64.encode(payment.getRequestData().getBytes());
            }
            transaction.setTransactionData(transactionData);
        }
        else {
            transaction.setTransactionData(null);
        }

        // NOTE: although deprecated as of iOS 7, "transactionReceipt" is still available as of iOS 9 & hopefully long there after :)
        String transactionDataSignature;
        try {
            NSData transactionReceipt = t.getTransactionReceipt();
            transactionDataSignature = transactionReceipt.toBase64EncodedString(NSDataBase64EncodingOptions.None);
        } catch (Throwable e) {
          log(LOGTYPELOG, "SKPaymentTransaction.transactionReceipt appears broken (was deprecated starting iOS 7.0).", e);
          transactionDataSignature = null;
        }
        transaction.setTransactionDataSignature(transactionDataSignature);

        // return the transaction
        return transaction;
    }

    private class AppleProductsDelegatePurchase extends SKProductsRequestDelegateAdapter {
      @Override
      public void didReceiveResponse (SKProductsRequest request, SKProductsResponse response) {
          // Received the registered products from AppStore.
          products = response.getProducts();
          if (products.size() == 1) {
            // Create a SKPayment from the product and start purchase flow
            SKProduct product = products.get(0);
            log(LOGTYPELOG, "Product info received/purchasing product " + product.getProductIdentifier() + " ...");
            SKPayment payment = new SKPayment(product);
            SKPaymentQueue.getDefaultQueue().addPayment(payment);
          }
          else {
            // wrong product count returned
            String errorMessage = "Error purchasing product (wrong product info count returned: " + products.size() + ")!";
            log(LOGTYPEERROR, errorMessage);
            observer.handlePurchaseError(new GdxPayException(errorMessage));
          }
      }

      @Override
      public void didFail (SKRequest request, NSError error) {
          String errorMessage = "Error requesting product info to later purchase: " + (error != null ? error.getLocalizedDescription() : "unknown");
          log(LOGTYPEERROR, errorMessage);
          observer.handlePurchaseError(new GdxPayException(errorMessage));
      }
  }

    private class IosFetchProductsAndInstallDelegate extends SKProductsRequestDelegateAdapter {
        @Override
        public void didReceiveResponse (SKProductsRequest request, SKProductsResponse response) {
            // Received the registered products from AppStore.
            products = response.getProducts();
            log(LOGTYPELOG, "Products successfully received!");

            final SKPaymentQueue defaultQueue = SKPaymentQueue.getDefaultQueue();

            // Create and register our apple transaction observer.
            if (appleObserver == null) {
                if (startupTransactionObserver != null) {
                    defaultQueue.removeTransactionObserver(startupTransactionObserver);
                    defaultQueue.removeStrongRef(startupTransactionObserver);
                    startupTransactionObserver = null;
                }

                appleObserver = new AppleTransactionObserver();
                defaultQueue.addTransactionObserver(appleObserver);
                defaultQueue.addStrongRef(appleObserver);
                log(LOGTYPELOG, "Purchase observer successfully installed!");
            }

            // notify of success...
            observer.handleInstall();

            // complete unfinished transactions
            final NSArray<SKPaymentTransaction> transactions = defaultQueue.getTransactions();
            log(LOGTYPELOG, "There are " + transactions.size() + " unfinished transactions. Try to finish...");
            appleObserver.updatedTransactions(defaultQueue, transactions);
        }

        @Override
        public void didFail (SKRequest request, NSError error) {
            log(LOGTYPEERROR, "Error requesting products: " + (error != null ? error.toString() : "unknown"));

            // Products request failed (likely due to insuficient network connection).
            observer.handleInstallError(new FetchItemInformationException("Error requesting products: "
                + (error != null ? error.toString() : "unknown")));

        }
    }

    // Transaction Observer for App Store promotions must be in place right after
    // didFinishLaunching(). So this is installed at app start before our full
    // AppleTransactionObserver is ready after fetching product information.
    private class PromotionTransactionObserver extends SKPaymentTransactionObserverAdapter {
        @Override
        public boolean shouldAddStorePayment(SKPaymentQueue queue, SKPayment payment, SKProduct product) {
            return addStorePayments;
        }
    }

    private class AppleTransactionObserver extends SKPaymentTransactionObserverAdapter {

        @Override
        public boolean shouldAddStorePayment(SKPaymentQueue queue, SKPayment payment, SKProduct product) {
            return addStorePayments;
        }

        @Override
        public void updatedTransactions (SKPaymentQueue queue, NSArray<SKPaymentTransaction> transactions) {
            for (final SKPaymentTransaction transaction : transactions) {
                SKPaymentTransactionState state = transaction.getTransactionState();
                switch (state) {
                case Purchased:
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
                        NSURL receiptURL = NSBundle.getMainBundle().getAppStoreReceiptURL();
                        NSData receipt = NSData.read(receiptURL);
                        if (receipt == null) {
                            log(LOGTYPELOG, "Fetching receipt...");
                            final SKReceiptRefreshRequest request = new SKReceiptRefreshRequest();
                            request.setDelegate(new SKRequestDelegateAdapter() {
                                @Override
                                public void didFinish (SKRequest r) {
                                    // Receipt refresh request finished.

                                    if (r.equals(request)) {
                                        NSURL receiptURL = NSBundle.getMainBundle().getAppStoreReceiptURL();
                                        NSData receipt = NSData.read(receiptURL);
                                        String encodedReceipt = receipt.toBase64EncodedString(NSDataBase64EncodingOptions.None);
// FIXME: parse out actual receipt for this IAP purchase:       t.setTransactionDataSignature(encodedReceipt);

                                        log(LOGTYPELOG, "Receipt was fetched!");
                                    } else {
                                        log(LOGTYPEERROR, "Receipt fetching failed: Request doesn't equal initial request!");
                                    }

                                    log(LOGTYPELOG, "Transaction was completed: " + getOriginalTxID(transaction));
                                    observer.handlePurchase(t);

                                    // Finish transaction.
                                    SKPaymentQueue.getDefaultQueue().finishTransaction(transaction);
                                }

                                @Override
                                public void didFail (SKRequest request, NSError error) {
                                    // Receipt refresh request failed. Let's just continue.
                                    log(LOGTYPEERROR, "Receipt fetching failed: " + error.toString());
                                    log(LOGTYPELOG, "Transaction was completed: " + getOriginalTxID(transaction));
                                    observer.handlePurchase(t);

                                    // Finish transaction.
                                    SKPaymentQueue.getDefaultQueue().finishTransaction(transaction);
                                }
                            });
                            request.start();
                        } else {
                            String encodedReceipt = receipt.toBase64EncodedString(NSDataBase64EncodingOptions.None);
// FIXME: parse out actual receipt for this IAP purchase:        t.setTransactionDataSignature(encodedReceipt);

                            log(LOGTYPELOG, "Transaction was completed: " + getOriginalTxID(transaction));
                            observer.handlePurchase(t);

                            // Finish transaction.
                            SKPaymentQueue.getDefaultQueue().finishTransaction(transaction);
                        }
                    }
                    else {
                        // we are done: let's report!
                        log(LOGTYPELOG, "Transaction was completed: " + getOriginalTxID(transaction));
                        observer.handlePurchase(t);

                        // Finish transaction.
                        SKPaymentQueue.getDefaultQueue().finishTransaction(transaction);
                    }
                    break;
                case Failed:
                    // Purchase failed.

                    // Decide if user cancelled or transaction failed.
                    NSError error = transaction.getError();
                    if (error == null) {
                        log(LOGTYPEERROR, "Transaction failed but error-object is null: " + transaction);
                        observer.handlePurchaseError(new GdxPayException("Transaction failed: " + transaction));
                    }
                    else if (error.getCode() == SKErrorCode.PaymentCancelled.value()) {
                        log(LOGTYPEERROR, "Transaction was cancelled by user!");
                        observer.handlePurchaseCanceled();
                    } else {
                        log(LOGTYPEERROR, "Transaction failed: " + error.toString());
                        observer.handlePurchaseError(new GdxPayException("Transaction failed: " + error.getLocalizedDescription()));
                    }

                    // Finish transaction.
                    SKPaymentQueue.getDefaultQueue().finishTransaction(transaction);
                    break;
                case Restored:
                    // A product has been restored.

                    // Parse transaction data.
                    Transaction ta = transaction(transaction);
                    if (ta == null)
                        break;

                    restoredTransactions.add(ta);

                    // Finish transaction.
                    SKPaymentQueue.getDefaultQueue().finishTransaction(transaction);

                    log(LOGTYPELOG, "Transaction has been restored: " + getOriginalTxID(transaction));
                    break;
                default:
                    break;
                }
            }
        }

        @Override
        public void restoreCompletedTransactionsFinished (SKPaymentQueue queue) {
            // All products have been restored.
            log(LOGTYPELOG, "All transactions have been restored!");

            observer.handleRestore(restoredTransactions.toArray(new Transaction[restoredTransactions.size()]));
            restoredTransactions.clear();
        }

        @Override
        public void restoreCompletedTransactionsFailed (SKPaymentQueue queue, NSError error) {
            // Restoration failed.

            // Decide if user cancelled or transaction failed.
            if (error.getCode() == SKErrorCode.PaymentCancelled.value()) {
                log(LOGTYPEERROR, "Restoring of transactions was cancelled by user!");
                observer.handleRestoreError(new GdxPayException("Restoring of purchases was cancelled by user!"));
            } else {
                log(LOGTYPEERROR, "Restoring of transactions failed: " + error.toString());
                observer.handleRestoreError(new GdxPayException("Restoring of purchases failed: " + error.getLocalizedDescription()));
            }
        }
    }

    void log (final int type, final String message) {
        log(type, message, null);
    }

    void log (final int type, final String message, Throwable e) {
        if (LOGDEBUG) {
            if (type == LOGTYPELOG) System.out.println('[' + TAG + "] " + message);
            if (type == LOGTYPEERROR) System.err.println('[' + TAG + "] " + message);
            if (e != null) System.err.println('[' + TAG + "] " + e);
        }
    }

    @Override
    public Information getInformation(String identifier) {
        if (products != null) {
            for (SKProduct p : products) {
                if (p.getProductIdentifier().equals(identifier)) {
                    if (numberFormatter == null) {
                        numberFormatter = new NSNumberFormatter();
                        numberFormatter.setFormatterBehavior(NSNumberFormatterBehavior._10_4);
                        numberFormatter.setNumberStyle(NSNumberFormatterStyle.Currency);
                    }
                    numberFormatter.setLocale(p.getPriceLocale());
                    return Information.newBuilder()
                        .localName(p.getLocalizedTitle())
                        .localDescription(p.getLocalizedDescription())
                        .localPricing(numberFormatter.format(p.getPrice()))
                        .priceCurrencyCode(p.getPriceLocale().getCurrencyCode())
                        .build();
                }
            }
        }
        return Information.UNAVAILABLE;
    }

    @Override
    public String toString () {
        return PurchaseManagerConfig.STORE_NAME_IOS_APPLE;				// FIXME: shouldnt this be PurchaseManagerConfig.STORE_NAME_IOS_APPLE or storeName() ??!!
    }
}
