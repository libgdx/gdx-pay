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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserData;
import com.amazon.device.iap.model.UserDataResponse;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;

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
	
	// ------- for Toasts (debugging) -----
	String toastText;
	int duration;

	private String currentUserId = null;
	private String currentMarketplace = null;
	
	// --------------------------------------------------

	public PurchaseManagerAndroidAmazon (Activity activity, int requestCode) {
		this.activity = activity;
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
		String requestId = PurchasingService.purchase(identifierForStore).toString();		
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
	        
		     // convert receipt to transaction
	        Transaction trans = convertReceiptToTransaction(1, requestId, receipt, userData);	// provides cancleState also
	        
	    	switch (receipt.getProductType()) {
	                
	        case CONSUMABLE:
	            // TODO: check consumable sample for how to handle consumable purchases
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

//	public void onActivityResult (int requestCode, int resultCode, Intent data) {
//		// forwards activities to OpenIAB for processing
//		// this is only relevant for android
//	}

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
        // not implemented yet for this purchase manager -> TODO
        return Information.UNAVAILABLE;
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
    	showMessage(LOGTYPELOG,  "onGetUserDataResponse: requestId (" + response.getRequestId()
                   + ") userIdRequestStatus: "
                   + response.getRequestStatus()
                   + ")");

        final UserDataResponse.RequestStatus status = response.getRequestStatus();
        
        switch (status) {
        case SUCCESSFUL:
        	showMessage(LOGTYPELOG,  "onUserDataResponse: get user id (" + response.getUserData().getUserId()
                       + ", marketplace ("
                       + response.getUserData().getMarketplace()
                       + ") ");
            currentUserId = response.getUserData().getUserId();
            currentMarketplace = response.getUserData().getMarketplace();
            break;

        case FAILED:
        case NOT_SUPPORTED:
        	showMessage(LOGTYPEERROR,  "onUserDataResponse failed, status code is " + status);
            currentUserId = null;
            currentMarketplace = null;
            break;
        }
    }

    
    //========= TODO =============
    /**
     * This is the callback for {@link PurchasingService#getProductData}. After
     * SDK sends the product details and availability to this method, it will
     * call {@link SampleIAPManager#enablePurchaseForSkus}
     * {@link SampleIAPManager#disablePurchaseForSkus} or
     * {@link SampleIAPManager#disableAllPurchases} method to set the purchase
     * status accordingly.
     */
    @Override
    public void onProductDataResponse(final ProductDataResponse response) {
        final ProductDataResponse.RequestStatus status = response.getRequestStatus();
        showMessage(LOGTYPELOG,  "onProductDataResponse: RequestStatus (" + status + ")");

        switch (status) {
        case SUCCESSFUL:
        	showMessage(LOGTYPELOG,  "onProductDataResponse: successful.  The item data map in this response includes the valid SKUs");
            final Set<String> unavailableSkus = response.getUnavailableSkus();
            showMessage(LOGTYPELOG,  "onProductDataResponse: " + unavailableSkus.size() + " unavailable skus");
//            enablePurchaseForSkus(response.getProductData());
//            disablePurchaseForSkus(response.getUnavailableSkus());
//            refreshLevel2Availability();
            break;
            
        case FAILED:
        case NOT_SUPPORTED:
        	showMessage(LOGTYPEERROR,  "onProductDataResponse: failed, should retry request");
//            disableAllPurchases();
            break;
        }
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

            currentUserId = response.getUserData().getUserId();
            currentMarketplace = response.getUserData().getMarketplace();
            
//            for (final Receipt receipt : response.getReceipts()) {
//                handleReceipt(response.getRequestId().toString(), receipt, response.getUserData());
//            }

            // send result to observer --------
			List<Transaction> transactions = new ArrayList<Transaction>(response.getReceipts().size());

			for (int i = 0; i < response.getReceipts().size(); i++) {
				transactions.add(convertReceiptToTransaction(i, response.getRequestId().toString(), response.getReceipts().get(i), response.getUserData()));
			}
			// send inventory to observer
			observer.handleRestore(transactions.toArray(new Transaction[transactions.size()]));
			
			//---- check if there are more receipts -------
            if (response.hasMore()) {
                PurchasingService.getPurchaseUpdates(false);
            }
            break;
            
        case FAILED:
        	showMessage(LOGTYPEERROR,  "onPurchaseUpdatesResponse: FAILED, should retry request");
//          disableAllPurchases();
        	observer.handleRestoreError(new Throwable("onPurchaseUpdatesResponse: FAILED, should retry request"));
      		break;
      		
        case NOT_SUPPORTED:
        	showMessage(LOGTYPEERROR,  "onPurchaseUpdatesResponse: NOT_SUPPORTED, should retry request");
//            disableAllPurchases();
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
            
            currentUserId = userId;
            currentMarketplace = response.getUserData().getMarketplace();
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
//            final Set<String> unavailableSkus = new HashSet<String>();
//            unavailableSkus.add(response.getReceipt().getSku());
//            disablePurchaseForSkus(unavailableSkus);
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
//    }

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
