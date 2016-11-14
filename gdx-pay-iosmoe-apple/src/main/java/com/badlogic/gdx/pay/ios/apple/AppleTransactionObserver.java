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

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.Owned;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Selector;

import apple.NSObject;
import apple.foundation.NSArray;
import apple.foundation.NSError;
import apple.storekit.SKPaymentQueue;
import apple.storekit.SKPaymentTransaction;
import apple.storekit.protocol.SKPaymentTransactionObserver;

@org.moe.natj.general.ann.Runtime(ObjCRuntime.class)
@ObjCClassName("AppleTransactionObserver")
@RegisterOnStartup
public class AppleTransactionObserver extends NSObject implements SKPaymentTransactionObserver {

    PurchaseManageriOSApple purchaseManageriOSApple;

    @Owned
    @Selector("alloc")
    public static native AppleTransactionObserver alloc();

    @Selector("init")
    public native AppleTransactionObserver init();

    protected AppleTransactionObserver(Pointer peer) {
        super(peer);
    }

    @Override
    public void paymentQueueUpdatedTransactions(SKPaymentQueue queue, NSArray<? extends
            SKPaymentTransaction> transactions) {
        purchaseManageriOSApple.paymentQueueUpdatedTransactions(queue, transactions);
    }

    @Override
    public void paymentQueueRestoreCompletedTransactionsFinished(SKPaymentQueue queue) {
        purchaseManageriOSApple.paymentQueueRestoreCompletedTransactionsFinished(queue);
    }

    @Override
    public void paymentQueueRestoreCompletedTransactionsFailedWithError(SKPaymentQueue queue,
                                                                        NSError error) {
        purchaseManageriOSApple.paymentQueueRestoreCompletedTransactionsFailedWithError(queue,
                error);
    }
}