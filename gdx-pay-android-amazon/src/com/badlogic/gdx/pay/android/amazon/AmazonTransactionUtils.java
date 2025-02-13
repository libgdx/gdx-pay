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

import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserData;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.Transaction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;

/**
 * A set of utility methods for Amazon transaction handling.
 * 
 */
public class AmazonTransactionUtils {

	private AmazonTransactionUtils() {
	}

	/** Converts a Receipt to our transaction object. */
	static Transaction convertReceiptToTransaction(int i, String requestId, Receipt receipt, final UserData userData) {

		// build the transaction from the purchase object
		Transaction transaction = new Transaction();
		transaction.setIdentifier(receipt.getSku());
		transaction.setOrderId(receipt.getReceiptId());
		transaction.setStoreName(PurchaseManagerConfig.STORE_NAME_ANDROID_AMAZON);
		transaction.setRequestId(requestId);
		transaction.setUserId(userData.getUserId());
		transaction.setPurchaseTime(receipt.getPurchaseDate());
		transaction.setPurchaseText("Purchased: " + receipt.getSku().toString());
		// transaction.setPurchaseCost(receipt.getSku()); // TODO: GdxPay: impl.
		// parsing of COST + CURRENCY via skuDetails.getPrice()!
		// transaction.setPurchaseCostCurrency(null);

		if (receipt.isCanceled()) {
			// order has been refunded or cancelled
			transaction.setReversalTime(receipt.getCancelDate());
			// transaction.setReversalText(receipt..getPurchaseState() == 1 ?
			// "Cancelled" : "Refunded");
		} else {
			// still valid!
			transaction.setReversalTime(null);
			transaction.setReversalText(null);
		}

		transaction.setTransactionData(receipt.toJSON().toString());
		// transaction.setTransactionDataSignature(purchase.getSignature());

		return transaction;
	}

	/** Converts a Product to our Information object. */
	static Information convertProductToInformation(Product product) {
		
		String priceString = product.getPrice();
		Float priceAsFloat = tryParsePriceToFloat(priceString);
		BigDecimal priceAsBigDecimal = tryParsePriceToBigDecimal(priceString);
		return Information.newBuilder()
				.localName(product.getTitle())
				.localDescription(product.getDescription())
				.localPricing(priceString)
				.priceCurrencyCode(tryParseCurrency(priceString))
				.priceInCents(convertToPriceInCents(priceAsFloat))
				.priceAsDouble(priceAsFloat == null ? null : priceAsFloat.doubleValue())
				.priceAsBigDecimal(priceAsBigDecimal)
				.build();
	}

	// Faulty currency parsing, feel free to improve
	// Currently returns first character that is neither a digit nor a comma nor
	// a dot
	static String tryParseCurrency(String priceString) {
		if (priceString == null)
			return null;
		for (int i = 0, n = priceString.length(); i < n; i++) {
			char current = priceString.charAt(i);
			if (current == '.' || current == ',' || Character.isDigit(current))
				continue;
			return new String(new char[] { current });
		}
		return null;
	}

	static Integer convertToPriceInCents(Float priceAsFloat) {
		return priceAsFloat == null ? null : MathUtils.ceilPositive(priceAsFloat * 100);
	}

	private static String tryParsePrice(String priceString) {
		if (priceString == null || priceString.length() == 0)
			return null;

		// Remove currency from string
		// The ugly way
		priceString = priceString.substring(1);

		// Remaining should be parseable
		return priceString;
	}

	private static Float tryParsePriceToFloat(String priceString) {
		priceString = tryParsePrice(priceString);
		if (priceString == null)
			return null;
		try {
			return NumberFormat.getInstance().parse(priceString).floatValue();
		} catch (ParseException exception) {
			// Silenced
		}
		return null;
	}

	private static BigDecimal tryParsePriceToBigDecimal(String priceString) {
		priceString = tryParsePrice(priceString);
		if (priceString == null)
			return null;
		try {
			return new BigDecimal(priceString);
		} catch (NumberFormatException exception) {
			// Silenced
		}
		return null;
	}
}
