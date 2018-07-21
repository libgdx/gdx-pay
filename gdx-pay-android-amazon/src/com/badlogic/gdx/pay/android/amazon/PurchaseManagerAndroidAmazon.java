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

import android.app.Activity;

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
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.pay.FetchItemInformationException;
import com.badlogic.gdx.pay.GdxPayException;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.ItemAlreadyOwnedException;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** The purchase manager implementation for Amazon.
 * <p>
 * See https://developer.amazon.com/de/docs/in-app-purchasing/iap-implement-iap.html
 *
 * @author just4phil */
public class PurchaseManagerAndroidAmazon implements PurchaseManager, PurchasingListener {

	/** Debug tag for logging. */
	private static final String TAG = "GdxPay/Amazon";

	/** Our Android activity. */
	private Activity activity;

	/** The registered observer. */
	private PurchaseObserver observer;

	/** The configuration. */
	private PurchaseManagerConfig config;

	/** The productList */
	private Set<String> productIdentifiers;

	private final Map<String, Information> informationMap = new ConcurrentHashMap<>();

	private String currentUserId = null;
	private String currentMarketplace = null;
	private boolean productDataRetrieved = false;

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
		if (userData == null) {
			this.currentUserId = null;
			this.currentMarketplace = null;
		} else {
			this.currentUserId = userData.getUserId();
			this.currentMarketplace = userData.getMarketplace();
		}
	}

	@Override
	public String storeName () {
		return PurchaseManagerConfig.STORE_NAME_ANDROID_AMAZON;
	}

	@Override
	public void install (final PurchaseObserver observer, PurchaseManagerConfig config, boolean autoFetchInformation) {
		this.observer = observer;
		this.config = config;

		// --- copy all available products to the list of productIdentifiers
		int offerSize = config.getOfferCount();
		productIdentifiers = new HashSet<>(offerSize);
		for (int z = 0; z < config.getOfferCount(); z++) {
			productIdentifiers.add(config.getOffer(z).getIdentifierForStore(storeName()));
		}

		PurchasingService.registerListener(activity.getApplicationContext(), this);

		// PurchasingService.IS_SANDBOX_MODE returns a boolean value.
		// Use this boolean value to check whether your app is running in test mode under the App Tester
		// or in the live production environment.
		Gdx.app.log(TAG, "Amazon IAP: sandbox mode is:" + PurchasingService.IS_SANDBOX_MODE);

		PurchasingService.getUserData();

		if (autoFetchInformation)
		    PurchasingService.getProductData(productIdentifiers);
		else
			productDataRetrieved = true;
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
	     */
		private void handleReceipt(final String requestId, final Receipt receipt, final UserData userData) {

			Gdx.app.log(TAG, "Handle receipt: requestId (" + requestId + ") receipt: " + receipt + ")");

			// convert receipt to transaction
	        Transaction trans = AmazonTransactionUtils.convertReceiptToTransaction(1, requestId, receipt, userData);	// provides cancleState also

			switch (receipt.getProductType()) {
				case CONSUMABLE:
				case SUBSCRIPTION:
				case ENTITLED:
					// inform the listener
					observer.handlePurchase(trans);
					// Automatically inform Amazon about the fullfilment
					PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.FULFILLED);
					break;

			}
	    }
//====================================================================================

	@Override
	public boolean installed () {
		return observer != null && currentUserId != null && productDataRetrieved;
	}

	@Override
	public void dispose () {

		if (observer != null) {
			// remove observer and config as well
			observer = null;
			config = null;
			Gdx.app.log(TAG, "disposed all the Amazon IAP stuff.");
		}
	}

    @Override
    public Information getInformation(String identifier) {
        Information information = informationMap.get(identifier);
        return information == null ? Information.UNAVAILABLE : information;
    }


    //=================================================



    /**
     * This is the callback for {@link PurchasingService#getUserData}.
     */
    @Override
    public void onUserDataResponse(final UserDataResponse response) {

        final UserDataResponse.RequestStatus status = response.getRequestStatus();

		Gdx.app.log(TAG, "onGetUserDataResponse: requestId (" + response.getRequestId()
        + ") userIdRequestStatus: "
        + status
        + ")");

        switch (status) {
        case SUCCESSFUL:
        	UserData userData = response.getUserData();
			Gdx.app.log(TAG, "onUserDataResponse: get user id (" + userData.getUserId()
                       + "), marketplace ("
                       + userData.getMarketplace()
                       + ") ");
        	updateUserData(userData);
        	notifyObserverWhenInstalled();
            break;

        case FAILED:
        case NOT_SUPPORTED:
			Gdx.app.error(TAG, "onUserDataResponse failed, status code is " + status);
        	updateUserData(null);
        	observer.handleInstallError(new GdxPayException("onUserDataResponse failed, status code is " + status));
            break;
        }
    }

	private void notifyObserverWhenInstalled() {
		if (installed())
			observer.handleInstall();
	}


	/**
     * This is the callback for {@link PurchasingService#getProductData}.
     */
    @Override
    public void onProductDataResponse(final ProductDataResponse response) {
        final ProductDataResponse.RequestStatus status = response.getRequestStatus();
		Gdx.app.log(TAG, "onProductDataResponse: RequestStatus (" + status + ")");

        switch (status) {
        case SUCCESSFUL:
			Gdx.app.log(TAG, "onProductDataResponse: successful");

        	// Store product information
        	Map<String, Product> availableSkus = response.getProductData();
			Gdx.app.log(TAG, "onProductDataResponse: " + availableSkus.size() + " available skus");
        	for (Entry<String, Product> entry : availableSkus.entrySet()) {
            	informationMap.put(entry.getKey(), AmazonTransactionUtils.convertProductToInformation(entry.getValue()));
        	}

            final Set<String> unavailableSkus = response.getUnavailableSkus();
			Gdx.app.log(TAG, "onProductDataResponse: " + unavailableSkus.size() + " unavailable skus");
        	for (String sku : unavailableSkus) {
				Gdx.app.log(TAG, "onProductDataResponse: sku " + sku + " is not available");
        	}
        	if (!productDataRetrieved) {
				productDataRetrieved = true;
				notifyObserverWhenInstalled();
			}
            break;

        case FAILED:
        case NOT_SUPPORTED:
			Gdx.app.error(TAG, "onProductDataResponse: failed, should retry request");
			if (!productDataRetrieved) {
				observer.handleInstallError(new FetchItemInformationException(String.valueOf(status)));
			}

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
		Gdx.app.log(TAG, "onPurchaseUpdatesResponse: requestId (" + response.getRequestId()
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
			List<Transaction> transactions = new ArrayList<>(response.getReceipts().size());

			Array<Receipt> consumables = new Array<>(response.getReceipts().size());
			for (int i = 0; i < response.getReceipts().size(); i++) {
				Receipt receipt = response.getReceipts().get(i);

				// Memoize consumables for later check
				if (receipt.getProductType() == ProductType.CONSUMABLE)
					consumables.add(receipt);

				transactions.add(AmazonTransactionUtils.convertReceiptToTransaction(i, response.getRequestId().toString(), receipt, response.getUserData()));
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
			Gdx.app.error(TAG, "onPurchaseUpdatesResponse: FAILED, should retry request");
        	observer.handleRestoreError(new GdxPayException("onPurchaseUpdatesResponse: FAILED, should retry request"));
      		break;

        case NOT_SUPPORTED:
			Gdx.app.error(TAG, "onPurchaseUpdatesResponse: NOT_SUPPORTED, should retry request");
        	observer.handleRestoreError(new GdxPayException("onPurchaseUpdatesResponse: NOT_SUPPORTED, should retry request"));
            break;
        }

    }

    /**
     * This is the callback for {@link PurchasingService#purchase}. For each
     * time the application sends a purchase request
     * {@link PurchasingService#purchase}, Amazon Appstore will call this
     * callback when the purchase request is completed.
     */
    @Override
    public void onPurchaseResponse(final PurchaseResponse response) {
        final String requestId = response.getRequestId().toString();
        final String userId = response.getUserData().getUserId();
        final PurchaseResponse.RequestStatus status = response.getRequestStatus();
		Gdx.app.log(TAG, "onPurchaseResponse: requestId (" + requestId
                   + ") userId ("
                   + userId
                   + ") purchaseRequestStatus ("
                   + status
                   + ")");

        switch (status) {
        case SUCCESSFUL:
            final Receipt receipt = response.getReceipt();

            updateUserData(response.getUserData());
			Gdx.app.log(TAG, "onPurchaseResponse: receipt json:" + receipt.toJSON());
            handleReceipt(response.getRequestId().toString(), receipt, response.getUserData());
            break;

        case ALREADY_PURCHASED:
			Gdx.app.log(TAG, "onPurchaseResponse: already purchased, you should verify the entitlement purchase on your side and make sure the purchase was granted to customer");
        	observer.handlePurchaseError(new ItemAlreadyOwnedException());
            break;

        case INVALID_SKU:
			Gdx.app.error(TAG, "onPurchaseResponse: invalid SKU!  onProductDataResponse should have disabled buy button already.");
        	observer.handlePurchaseError(new GdxPayException("onPurchaseResponse: INVALID_SKU"));
            break;

        case FAILED:
			Gdx.app.error(TAG, "onPurchaseResponse: FAILED");
        	observer.handlePurchaseError(new GdxPayException("onPurchaseResponse: FAILED"));
            break;

        case NOT_SUPPORTED:
			Gdx.app.error(TAG, "onPurchaseResponse: NOT_SUPPORTED so remove purchase request from local storage");
        	observer.handlePurchaseError(new GdxPayException("onPurchaseResponse: NOT_SUPPORTED"));
            break;
        }
    }

    //=======================================================

	@Override
	public String toString () {
		return storeName();
	}
}
