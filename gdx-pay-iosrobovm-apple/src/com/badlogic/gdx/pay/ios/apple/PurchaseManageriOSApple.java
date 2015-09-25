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

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;

/** The purchase manager implementation for Apple's iOS IAP system.
 * 
 * <p>
 * To integrate into your iOS project do the following:
 * <ol>
 * <li>add the jar-files to your project's lib directory as follows (IAP will work automatically once the files are present):
 * <ul>
 * <li>gdx-pay.jar: This goes into your "core"/lib project.
 * <li>gdx-pay-client.jar: This goes into your "core"/lib project.
 * <li>gdx-pay-iosrobovm.jar: This goes into your "iOS"/lib directory.
 * </ul>
 * </ol>
 * Please note that no code changes for iOS are necessary. As soon as you place the jar files everything will work out of the box
 * (instantiated via reflection).
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
    private SKProductsRequest productsRequest;
    private NSArray<SKProduct> products;

    private final List<Transaction> restoredTransactions = new ArrayList<Transaction>();

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

            // Request configured offers/products.
            log(LOGTYPELOG, "Requesting products...");
            productsRequest = new SKProductsRequest(productIdentifiers);
            productsRequest.setDelegate(new AppleProductsDelegate());
            productsRequest.start();
        } else {
            log(LOGTYPEERROR, "Error setting up in-app-billing: Device not configured for purchases!");
            observer.handleInstallError(new RuntimeException(
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
            SKPaymentQueue.getDefaultQueue().removeTransactionObserver(appleObserver);
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

    /** Converts a purchase to our transaction object. */
    Transaction transaction (SKPaymentTransaction t) {
        SKPayment payment = t.getPayment();
        String productIdentifier = payment.getProductIdentifier();
        SKProduct product = getProductByStoreIdentifier(productIdentifier);
        if (product == null) {
            System.err.println("gdx-pay: Ignoring unknown product: " + productIdentifier);
            return null;
        }

        // Build the transaction from the payment transaction object.
        Transaction transaction = new Transaction();
        transaction.setIdentifier(config.getOfferForStore(PurchaseManagerConfig.STORE_NAME_IOS_APPLE, payment.getProductIdentifier()).getIdentifier());

        transaction.setStoreName(PurchaseManagerConfig.STORE_NAME_IOS_APPLE);
        transaction.setOrderId(t.getTransactionIdentifier());
        
        transaction.setPurchaseTime(t.getTransactionDate().toDate());
        transaction.setPurchaseText("Purchased: " + product.getLocalizedTitle());
        transaction.setPurchaseCost((int) Math.round(product.getPrice().doubleValue() * 100));
        transaction.setPurchaseCostCurrency(product.getPriceLocale().getCurrencyCode());
        
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
        transaction.setTransactionDataSignature(null);

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
            observer.handlePurchaseError(new RuntimeException(errorMessage));
          }
      }

      @Override
      public void didFail (SKRequest request, NSError error) {
          String errorMessage = "Error requesting product info to later purchase: " + (error != null ? error.toString() : "unknown");
          log(LOGTYPEERROR, errorMessage);
          observer.handlePurchaseError(new RuntimeException(errorMessage));
      }
  }

    private class AppleProductsDelegate extends SKProductsRequestDelegateAdapter {
        @Override
        public void didReceiveResponse (SKProductsRequest request, SKProductsResponse response) {
            // Received the registered products from AppStore.
            products = response.getProducts();
            log(LOGTYPELOG, "Products successfully received!");

            final SKPaymentQueue defaultQueue = SKPaymentQueue.getDefaultQueue();

            // Create and register our apple transaction observer.
            appleObserver = new AppleTransactionObserver();
            defaultQueue.addTransactionObserver(appleObserver);
            log(LOGTYPELOG, "Purchase observer successfully installed!");

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
            observer.handleInstallError(new RuntimeException("Error requesting products: "
                + (error != null ? error.toString() : "unknown")));

        }
    }

    private class AppleTransactionObserver extends SKPaymentTransactionObserverAdapter {
        @Override
        public void updatedTransactions (SKPaymentQueue queue, NSArray<SKPaymentTransaction> transactions) {
            for (final SKPaymentTransaction transaction : transactions) {
                SKPaymentTransactionState state = transaction.getTransactionState();
                switch (state) {
                case Purchased:
                    // Product was successfully purchased.

                    // Parse transaction data.
                    final Transaction t = transaction(transaction);
                    if (t == null)
                        break;

                    // Find transaction receipt.
                    if (Foundation.getMajorSystemVersion() >= 7) {
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
                                        t.setTransactionDataSignature(encodedReceipt);

                                        log(LOGTYPELOG, "Receipt was fetched!");
                                    } else {
                                        log(LOGTYPEERROR, "Receipt fetching failed: Request doesn't equal initial request!");
                                    }

                                    log(LOGTYPELOG, "Transaction was completed: " + transaction.getTransactionIdentifier());

                                    observer.handlePurchase(t);

                                    // Finish transaction.
                                    SKPaymentQueue.getDefaultQueue().finishTransaction(transaction);
                                }

                                @Override
                                public void didFail (SKRequest request, NSError error) {
                                    // Receipt refresh request failed. Let's just continue.
                                    log(LOGTYPEERROR, "Receipt fetching failed: " + error.toString());

                                    log(LOGTYPELOG, "Transaction was completed: " + transaction.getTransactionIdentifier());

                                    observer.handlePurchase(t);

                                    // Finish transaction.
                                    SKPaymentQueue.getDefaultQueue().finishTransaction(transaction);
                                }
                            });
                            request.start();
                        } else {
                            String encodedReceipt = receipt.toBase64EncodedString(NSDataBase64EncodingOptions.None);
                            t.setTransactionDataSignature(encodedReceipt);

                            log(LOGTYPELOG, "Transaction was completed: " + transaction.getTransactionIdentifier());

                            observer.handlePurchase(t);

                            // Finish transaction.
                            SKPaymentQueue.getDefaultQueue().finishTransaction(transaction);
                        }
                    } else {
                        t.setTransactionDataSignature(Base64.encode(transaction.getTransactionReceipt().getBytes()));

                        log(LOGTYPELOG, "Transaction was completed: " + transaction.getTransactionIdentifier());

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
                        observer.handlePurchaseError(new RuntimeException("Transaction failed: " + transaction));
                    }
                    else if (error.getCode() == SKErrorCode.PaymentCancelled.value()) {
                        log(LOGTYPEERROR, "Transaction was cancelled by user!");
                        observer.handlePurchaseCanceled();
                    } else {
                        log(LOGTYPEERROR, "Transaction failed: " + error.toString());
                        observer.handlePurchaseError(new RuntimeException("Transaction failed: " + error.toString()));
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

                    log(LOGTYPELOG, "Transaction has been restored: " + transaction.getTransactionIdentifier());
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
                observer.handleRestoreError(new RuntimeException("Restoring of purchases was cancelled by user!"));
            } else {
                log(LOGTYPEERROR, "Restoring of transactions failed: " + error.toString());
                observer.handleRestoreError(new RuntimeException("Restoring of purchases failed: " + error.toString()));
            }
        }
    }

    void log (final int type, final String message) {
        if (LOGDEBUG) {
            if (type == LOGTYPELOG) System.out.println('[' + TAG + "] " + message);
            if (type == LOGTYPEERROR) System.err.println('[' + TAG + "] " + message);
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
                    Information i = new Information(p.getLocalizedTitle(), p.getLocalizedDescription(),
                        numberFormatter.format(p.getPrice()));
                    return i;
                }
            }
        }
        return Information.UNAVAILABLE;
    }
    
    @Override
    public String toString () {
        return "AppleIOS";				// FIXME: shouldnt this be PurchaseManagerConfig.STORE_NAME_IOS_APPLE or storeName() ??!!
    }
}
