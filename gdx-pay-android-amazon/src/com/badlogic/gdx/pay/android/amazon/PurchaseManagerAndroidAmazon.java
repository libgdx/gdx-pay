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

package com.badlogic.gdx.pay.android.amazon;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.ProductType;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserData;
import com.amazon.device.iap.model.UserDataResponse;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;
import com.badlogic.gdx.utils.Array;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/** The purchase manager implementation for OUYA.
 * <p>
 * Include the gdx-pay-android-ouya.jar for this to work (plus gdx-pay-android.jar). Also update the "uses-permission" settings in
 * AndroidManifest.xml and your proguard settings.
 * 
 * @author just4phil */
public class PurchaseManagerAndroidAmazon implements PurchaseManager, PurchasingListener {

	/** Debug tag for logging. */
	private static final String TAG = "GdxPay/Amazon";
	private static final boolean LOGDEBUG 		= true;
	private static final boolean SHOWTOASTS 	= false;
	private static final int LOGTYPELOG = 0;
	private static final int LOGTYPEERROR = 1;

	/** Our Android activity. */
	Activity activity;

	/** The registered observer. */
	PurchaseObserver observer;

	/** The configuration. */
	PurchaseManagerConfig config;
	
	/** The productList */
	Set<String> productIdentifiers;

	private final Map<String, Information> informationMap = new ConcurrentHashMap<String, Information>();
	
	// ------- for Toasts (debugging) -----
	String toastText;
	int duration;

	private String currentUserId = null;
	private String currentMarketplace = null;
	
	// --------------------------------------------------

	public PurchaseManagerAndroidAmazon (Activity activity, int requestCode) {
		this.activity = activity;
	}

	public String getCurrentUserId() {
		return currentUserId;
	}

	public String getCurrentMarketplace() {
		return currentMarketplace;
	}
	
	private void updateUserData(UserData userData) {
		this.currentUserId = userData.getUserId();
		this.currentMarketplace = userData.getMarketplace();
	}
	
	private void clearUserData() {
		this.currentUserId = null;
		this.currentMarketplace = null;
	}

	@Override
	public String storeName () {
		return PurchaseManagerConfig.STORE_NAME_ANDROID_AMAZON;
	}

	/* TODO use autoFetchInformation */
	@Override
	public void install (final PurchaseObserver observer, PurchaseManagerConfig config, boolean autoFetchInformation) {
		this.observer = observer;
		this.config = config;
		
		// --- copy all available products to the list of productIdentifiers
		int offerSize = config.getOfferCount();
		productIdentifiers = new HashSet<String>(offerSize);
		for (int z = 0; z < config.getOfferCount(); z++) {
			productIdentifiers.add(config.getOffer(z).getIdentifierForStore(storeName()));
		}
		
		PurchasingService.registerListener(activity.getApplicationContext(), this);
		
		// PurchasingService.IS_SANDBOX_MODE returns a boolean value. 
		// Use this boolean value to check whether your app is running in test mode under the App Tester 
		// or in the live production environment.
		showMessage(LOGTYPELOG, "Amazon IAP: sandbox mode is:" + PurchasingService.IS_SANDBOX_MODE);
		
		observer.handleInstall();
		
		PurchasingService.getUserData();
		
		PurchasingService.getProductData(productIdentifiers);
	}

	// ----- Handler --------------------

	Handler handler = new HandlerExtension(Looper.getMainLooper());

	final static int showToast = 0;

	final class HandlerExtension extends Handler {

		public HandlerExtension(Looper mainLooper) {
			super(mainLooper);
		}

		@Override
		public void handleMessage (Message msg) {

			switch (msg.what) {

			case showToast:
				Toast toast = Toast.makeText(activity, toastText, duration);
				toast.show();
				break;
			}
		}
	}

	@Override
	public void purchase(String identifier) {  
		String identifierForStore = config.getOffer(identifier).getIdentifierForStore(storeName());
		PurchasingService.purchase(identifierForStore);		
	}

	@Override
	public void purchaseRestore() {
		PurchasingService.getPurchaseUpdates(true);		// true: always gets ALL purchased items (complete history)
	}
	
//=====================================================================================

	    /**
	     * Method to handle receipts
	     * 
	     * @param requestId
	     * @param receipt
	     * @param userData
	     */
	    public void handleReceipt(final String requestId, final Receipt receipt, final UserData userData) {

	    	showMessage(LOGTYPELOG,  "Handle receipt: requestId (" + requestId
	        + ") receipt: "
	        + receipt
	        + ")");
	        
		     // convert receipt to transaction
	        Transaction trans = convertReceiptToTransaction(1, requestId, receipt, userData);	// provides cancleState also
	        
	    	switch (receipt.getProductType()) {
	                
	        case CONSUMABLE:
				// inform the listener
				observer.handlePurchase(trans);
	            // Automatically consume item
	        	PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.FULFILLED);
	            break;
	            
	        case ENTITLED:
				// inform the listener
				observer.handlePurchase(trans);
	            break;
	            
	        case SUBSCRIPTION:
	            // TODO: check subscription sample for how to handle consumable purchases
	            break;
	        }
	    }
//====================================================================================

	void showMessage (final int type, final String message) {
		if (LOGDEBUG) {
			if (type == LOGTYPELOG) Log.d(TAG, message);
			if (type == LOGTYPEERROR) Log.e(TAG, message);
		}
		if (SHOWTOASTS) {
			if (type == LOGTYPELOG) showToast(message);
			if (type == LOGTYPEERROR) showToast("error: " + message);
		}
	}

	// ---- saves the toast text and displays it
	void showToast (String toastText) {
		this.duration = Toast.LENGTH_SHORT;
		this.toastText = toastText;
		handler.sendEmptyMessage(showToast);
	}

	@Override
	public boolean installed () {
//		if (PurchaseSystem.hasManager()) return true;	// this leads to unwanted binding via reflection !!
		return observer != null;
	}

	@Override
	public void dispose () {

		if (observer != null) {			
			// remove observer and config as well
			observer = null;
			config = null;
			showMessage(LOGTYPELOG, "disposed all the Amazon IAP stuff.");
		}
	}

    @Override
    public Information getInformation(String identifier) {
        Information information = informationMap.get(identifier);
        return information == null ? Information.UNAVAILABLE : information;
    }
    
    
    //=================================================
    
    
  
    /**
     * This is the callback for {@link PurchasingService#getUserData}. For
     * successful case, get the current user from {@link UserDataResponse} and
     * call {@link SampleIAPManager#setAmazonUserId} method to load the Amazon
     * user and related purchase information
     * 
     * @param response
     */
    @Override
    public void onUserDataResponse(final UserDataResponse response) {

        final UserDataResponse.RequestStatus status = response.getRequestStatus();

    	showMessage(LOGTYPELOG,  "onGetUserDataResponse: requestId (" + response.getRequestId()
        + ") userIdRequestStatus: "
        + status
        + ")");
        
        switch (status) {
        case SUCCESSFUL:
        	UserData userData = response.getUserData();
        	showMessage(LOGTYPELOG,  "onUserDataResponse: get user id (" + userData.getUserId()
                       + "), marketplace ("
                       + userData.getMarketplace()
                       + ") ");
        	updateUserData(userData);
            break;

        case FAILED:
        case NOT_SUPPORTED:
        	showMessage(LOGTYPEERROR,  "onUserDataResponse failed, status code is " + status);
            clearUserData();
            break;
        }
    }

    
    /**
     * This is the callback for {@link PurchasingService#getProductData}.
     */
    @Override
    public void onProductDataResponse(final ProductDataResponse response) {
        final ProductDataResponse.RequestStatus status = response.getRequestStatus();
        showMessage(LOGTYPELOG,  "onProductDataResponse: RequestStatus (" + status + ")");

        switch (status) {
        case SUCCESSFUL:
        	showMessage(LOGTYPELOG,  "onProductDataResponse: successful");
        	
        	// Store product information
        	Map<String, Product> availableSkus = response.getProductData();
            showMessage(LOGTYPELOG,  "onProductDataResponse: " + availableSkus.size() + " available skus");
        	for (Entry<String, Product> entry : availableSkus.entrySet()) {
        		Product product = entry.getValue();
        		String priceString = product.getPrice();
            	informationMap.put(entry.getKey(),
            			Information.newBuilder()
        				.localName(product.getTitle())
        				.localDescription(product.getDescription())
        				.localPricing(priceString)
        				.priceCurrencyCode(tryParseCurrency(priceString))
        				.priceInCents(tryParsePriceInCents(priceString))
        				.build());
        	}
        	
            final Set<String> unavailableSkus = response.getUnavailableSkus();
            showMessage(LOGTYPELOG,  "onProductDataResponse: " + unavailableSkus.size() + " unavailable skus");
        	for (String sku : unavailableSkus) {
                showMessage(LOGTYPELOG,  "onProductDataResponse: sku " + sku + " is not available");
        	}
            break;
            
        case FAILED:
        case NOT_SUPPORTED:
        	showMessage(LOGTYPEERROR,  "onProductDataResponse: failed, should retry request");
            break;
        }
    }

    // Faulty currency parsing, feel free to improve
    // Currently returns first character that is neither a digit nor a comma nor a dot
	private String tryParseCurrency(String priceString) {
		if (priceString == null)
			return null;
		for (int i = 0, n = priceString.length(); i < n; i++) {
			char current = priceString.charAt(i);
			if (
					current == '.'
					|| current == ','
					|| Character.isDigit(current)
					)
				continue;
			return new String(new char[] { current });
		}
		return null;
	}

    private Integer tryParsePriceInCents(String priceString) {
		if (priceString == null || priceString.length() == 0)
			return null;
		try {
			// Remove currency from string
			// The ugly way
			priceString = priceString.substring(1);
			
			// Remaining should be parseable
			float value = NumberFormat.getInstance().parse(priceString).floatValue();
			return MathUtils.ceilPositive(value * 100);
		} catch (ParseException exception) {
	    	showMessage(LOGTYPEERROR,  "Unable to parse price string: (" + priceString + ") -- " + exception.getLocalizedMessage());
		}
		return null;
	}

	/**
     * This is the callback for {@link PurchasingService#getPurchaseUpdates}.
     * 
     * You will receive Entitlement receipts from this callback.
     * 
     */
    @Override
    public void onPurchaseUpdatesResponse(final PurchaseUpdatesResponse response) {
    	showMessage(LOGTYPELOG,  "onPurchaseUpdatesResponse: requestId (" + response.getRequestId()
                   + ") purchaseUpdatesResponseStatus ("
                   + response.getRequestStatus()
                   + ") userId ("
                   + response.getUserData().getUserId()
                   + ")");
        final PurchaseUpdatesResponse.RequestStatus status = response.getRequestStatus();
        switch (status) {
        case SUCCESSFUL:

            updateUserData(response.getUserData());
            
//            for (final Receipt receipt : response.getReceipts()) {
//                handleReceipt(response.getRequestId().toString(), receipt, response.getUserData());
//            }

            // send result to observer --------
			List<Transaction> transactions = new ArrayList<Transaction>(response.getReceipts().size());

			Array<Receipt> consumables = new Array<Receipt>(response.getReceipts().size());
			for (int i = 0; i < response.getReceipts().size(); i++) {
				Receipt receipt = response.getReceipts().get(i);
				
				// Memoize consumables for later check
				if (receipt.getProductType() == ProductType.CONSUMABLE)
					consumables.add(receipt);

				transactions.add(convertReceiptToTransaction(i, response.getRequestId().toString(), receipt, response.getUserData()));
			}
			// send inventory to observer
			observer.handleRestore(transactions.toArray(new Transaction[transactions.size()]));

            // Automatically consume consumables if this was not previously the case
			for (int i = 0; i < consumables.size; i++) {
				Receipt consumable = consumables.get(i);
	        	PurchasingService.notifyFulfillment(consumable.getReceiptId(), FulfillmentResult.FULFILLED);
			}
			
			//---- check if there are more receipts -------
            if (response.hasMore()) {
                PurchasingService.getPurchaseUpdates(false);
            }
            break;
            
        case FAILED:
        	showMessage(LOGTYPEERROR,  "onPurchaseUpdatesResponse: FAILED, should retry request");
        	observer.handleRestoreError(new Throwable("onPurchaseUpdatesResponse: FAILED, should retry request"));
      		break;
      		
        case NOT_SUPPORTED:
        	showMessage(LOGTYPEERROR,  "onPurchaseUpdatesResponse: NOT_SUPPORTED, should retry request");
        	observer.handleRestoreError(new Throwable("onPurchaseUpdatesResponse: NOT_SUPPORTED, should retry request"));
            break;
        }

    }

    /**
     * This is the callback for {@link PurchasingService#purchase}. For each
     * time the application sends a purchase request
     * {@link PurchasingService#purchase}, Amazon Appstore will call this
     * callback when the purchase request is completed. If the RequestStatus is
     * Successful or AlreadyPurchased then application needs to call
     * {@link SampleIAPManager#handleReceipt} to handle the purchase
     * fulfillment. If the RequestStatus is INVALID_SKU, NOT_SUPPORTED, or
     * FAILED, notify corresponding method of {@link SampleIAPManager} .
     */
    @Override
    public void onPurchaseResponse(final PurchaseResponse response) {
        final String requestId = response.getRequestId().toString();
        final String userId = response.getUserData().getUserId();
        final PurchaseResponse.RequestStatus status = response.getRequestStatus();
        showMessage(LOGTYPELOG,  "onPurchaseResponse: requestId (" + requestId
                   + ") userId ("
                   + userId
                   + ") purchaseRequestStatus ("
                   + status
                   + ")");

        switch (status) {
        case SUCCESSFUL:
            final Receipt receipt = response.getReceipt();
            
            updateUserData(response.getUserData());
            showMessage(LOGTYPELOG,  "onPurchaseResponse: receipt json:" + receipt.toJSON());
            handleReceipt(response.getRequestId().toString(), receipt, response.getUserData());
            break;
            
        case ALREADY_PURCHASED:
        	showMessage(LOGTYPELOG, "onPurchaseResponse: already purchased, you should verify the entitlement purchase on your side and make sure the purchase was granted to customer");
        	observer.handlePurchaseError(new Throwable("onPurchaseResponse: ALREADY_PURCHASED"));
            break;
            
        case INVALID_SKU:
        	showMessage(LOGTYPEERROR, 
                  "onPurchaseResponse: invalid SKU!  onProductDataResponse should have disabled buy button already.");
        	observer.handlePurchaseError(new Throwable("onPurchaseResponse: INVALID_SKU"));
            break;
            
        case FAILED:
        	showMessage(LOGTYPEERROR, "onPurchaseResponse: FAILED");
        	observer.handlePurchaseError(new Throwable("onPurchaseResponse: FAILED"));
            break;
            
        case NOT_SUPPORTED:
        	showMessage(LOGTYPEERROR,  "onPurchaseResponse: NOT_SUPPORTED so remove purchase request from local storage");
        	observer.handlePurchaseError(new Throwable("onPurchaseResponse: NOT_SUPPORTED"));
            break;
        }
    }

    //=======================================================
    
    
    
	/** Converts a Receipt to our transaction object. */
	Transaction convertReceiptToTransaction (int i, String requestId, Receipt receipt, final UserData userData) {

		// build the transaction from the purchase object
		Transaction transaction = new Transaction();
		transaction.setIdentifier(receipt.getSku());
		transaction.setOrderId(receipt.getReceiptId());
		transaction.setStoreName(storeName());
		transaction.setRequestId(requestId);
		transaction.setUserId(userData.getUserId());
		transaction.setPurchaseTime(receipt.getPurchaseDate());
		transaction.setPurchaseText("Purchased: " + receipt.getSku().toString());
		// transaction.setPurchaseCost(receipt.getSku()); // TODO: GdxPay: impl. parsing of COST + CURRENCY via skuDetails.getPrice()!
		// transaction.setPurchaseCostCurrency(null);

		 if (receipt.isCanceled()) {
		// order has been refunded or cancelled
		 transaction.setReversalTime(receipt.getCancelDate());
//		 transaction.setReversalText(receipt..getPurchaseState() == 1 ? "Cancelled" : "Refunded");
		 } else {
		 // still valid!
			 transaction.setReversalTime(null);
			 transaction.setReversalText(null);
		 }

		 transaction.setTransactionData(receipt.toJSON().toString());
		// transaction.setTransactionDataSignature(purchase.getSignature());

		showMessage(LOGTYPELOG, "converted purchased product " + i + " to transaction.");
		return transaction;
	}
	
	@Override
	public String toString () {
		return storeName();
	}
}
