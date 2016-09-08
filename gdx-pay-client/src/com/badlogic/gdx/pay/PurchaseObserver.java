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

package com.badlogic.gdx.pay;

/** Observer to wait for the purchase manager to initialize as well as to handle restored/refunded transactions.
 *
 * @author noblemaster
 *
 * <p>Calling thread of callback methods might not be libGDX main loop thread!</p>
 */
public interface PurchaseObserver {

	/** Called when the purchase manager has successfully initialized.
	 *
	 * <p>The in-app purchase service has been connected, and in-app products have been successfully
	 * retrieved from the in-app payment service. </p>
	 *
	 * <p>Calling thread might not be Libgdx main loop thread!</p>
	 */
	void handleInstall();

	/** Called when the installation of the purchase manager failed.
	 * 
	 * @param e The error, e.g. network outage, invalid keys, etc.
	 *
	 * <p>Calling thread might not be libGDX main loop thread!</p>
	 */
	void handleInstallError(Throwable e);

	/** Called when purchases have been restored. The "valid" parameter will indicate if the purchase was successful (true) or was
	 * aborted or refunded by the user (false).
	 *
	 * <p>Calling thread might not be libGDX main loop thread!</p>
	 *
	 * @param transactions The restored purchases. */
	void handleRestore(Transaction[] transactions);

	/** Called when a restore failed for unexpected reasons.
	 * 
	 * @param e The error, e.g. network outage, invalid identifier, etc.
	 *
	 * <p>Calling thread might not be libGDX main loop thread!</p>
	 */
	void handleRestoreError(Throwable e);
	
	/** The item that was purchased. The "valid" parameter will indicate if the purchase was successful (true) or was aborted or
	 * refunded by the user (false).
	 * 
	 * @param transaction The purchased item information.
	 * <p>Calling thread might not be libGDX main loop thread!</p>
	 * */
	void handlePurchase(Transaction transaction);

	/** Called when a purchase failed for unexpected reasons.
	 * 
	 * @param e The error, e.g. network outage, invalid identifier, etc.
	 * <p>Calling thread might not be libGDX main loop thread!</p>
	 * */
	void handlePurchaseError(Throwable e);
	
	/** Called when a purchase is canceled by the user.
	 *
	 * <p>Calling thread might not be libGDX main loop thread!</p>
	 */
	void handlePurchaseCanceled();
}
