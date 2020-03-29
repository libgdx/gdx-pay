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

/**
 * An IAP purchase manager interface (client). Use this interface in your main game's class for referencing the
 * actual, platform-dependant purchase system implementation.
 * <p>
 * Items for purchase are referenced by an item identifier integer value. Make sure to register the same identifier
 * on all the IAP services desired for easy porting. The same identifier should be registered in the IAP item setup
 * screen of each IAP service (Google Play IAP, Amazon IAP, iOS IAP, Apple Mac Store IAP, Steam etc). For stores that
 * support textual item identifiers (most of them except Steam) prefix a "item_" before the item identifier number.
 * <p>
 * Please note due to limitations by the various IAP services you need to manage identifiers on your own. It is not
 * possible for example to retrieve the IAP item list for all the stores. It is also not possible to store icons,
 * downloadable content etc. in some IAP services. Icons and downloadable content have to be either integrated into
 * your application or served by a separate
 * server that you setup. Your application is responsible to display the items for purchase.
 *
 * @author noblemaster
 */
public interface PurchaseManager extends InformationFinder {

    /**
     * Returns the store name.
     */
    String storeName();

    /**
     * Registers a purchase observer which handles installs of apps on a new device or aborted purchases from a previous session
     * that were not yet handled by the application. The observer is called for all unfinished transactions. The observer is also
     * called for refunds of previous purchased items.
     * <p>
     * Registering an observer is required. If no observer is registered the call to purchase an item will fail with a runtime
     * exception to teach you lesson to always remember to set a purchase observer. The purchase observer is needed to make sure
     * all purchases have been handled and served to the customer.
     * </p>
     *
     * @param observer             The observer which is called whenever purchases have to be handled by the application as well as when the
     *                             store has been installed.
     * @param config               The configuration. Please note offers inside the configuration can be updated on the fly (e.g. by
     * @param autoFetchInformation tells PurchaseManager to automatically fetch offer details on setup
     */
    void install(PurchaseObserver observer, PurchaseManagerConfig config, boolean autoFetchInformation);

    /**
     * Returns true if the purchase manager is installed (non-disposed) and ready to go.
     */
    boolean installed();

    /**
     * Disposes the purchase manager.
     */
    void dispose();

    /**
     * Requests to purchase an item. The listener will always be called once the purchase has either completed or failed.
     * <p>
     * Note: a GDX runtime exception is thrown if you have not registered a purchase observer.
     *
     * @param identifier The item to purchase.
     */
    void purchase(String identifier);

    /**
     * Restores existing purchases.
     */
    void purchaseRestore();

}
