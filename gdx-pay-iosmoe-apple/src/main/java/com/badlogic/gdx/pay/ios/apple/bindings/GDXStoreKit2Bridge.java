package com.badlogic.gdx.pay.ios.apple.bindings;

import apple.NSObject;
import apple.foundation.NSArray;
import apple.foundation.NSDictionary;
import apple.foundation.NSError;

import org.moe.natj.general.NatJ;
import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.Library;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.ObjCBlock;
import org.moe.natj.objc.ann.ObjCClassBinding;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Selector;

@Library("GdxPayStoreKit2Bridge")
@Runtime(ObjCRuntime.class)
@ObjCClassBinding
@ObjCClassName("GDXStoreKit2Bridge")
public class GDXStoreKit2Bridge extends NSObject {
    static {
        NatJ.register();
    }

    protected GDXStoreKit2Bridge(Pointer peer) {
        super(peer);
    }

    @Selector("shared")
    public static native GDXStoreKit2Bridge shared();

    @Selector("canMakePayments")
    public native boolean canMakePayments();

    @Selector("fetchProductsWithIdentifiers:completion:")
    public native void fetchProductsWithIdentifiersCompletion(
            NSArray<String> identifiers,
            @ObjCBlock(name = "call_fetchProductsWithIdentifiersCompletion")
            Block_fetchProductsWithIdentifiersCompletion completion);

    @Selector("purchaseWithIdentifier:completion:")
    public native void purchaseWithIdentifierCompletion(
            String identifier,
            @ObjCBlock(name = "call_purchaseWithIdentifierCompletion")
            Block_purchaseWithIdentifierCompletion completion);

    @Selector("fetchCurrentEntitlementsWithCompletion:")
    public native void fetchCurrentEntitlementsWithCompletion(
            @ObjCBlock(name = "call_fetchCurrentEntitlementsWithCompletion")
            Block_fetchCurrentEntitlementsWithCompletion completion);

    @Selector("restorePurchasesWithCompletion:")
    public native void restorePurchasesWithCompletion(
            @ObjCBlock(name = "call_restorePurchasesWithCompletion")
            Block_restorePurchasesWithCompletion completion);

    @Selector("startObservingTransactionsWithCompletion:")
    public native void startObservingTransactionsWithCompletion(
            @ObjCBlock(name = "call_startObservingTransactionsWithCompletion")
            Block_startObservingTransactionsWithCompletion completion);

    @Selector("stopObservingTransactions")
    public native void stopObservingTransactions();

    public interface Block_fetchProductsWithIdentifiersCompletion {
        void call_fetchProductsWithIdentifiersCompletion(NSArray<NSDictionary<String, Object>> products, NSError error);
    }

    public interface Block_purchaseWithIdentifierCompletion {
        void call_purchaseWithIdentifierCompletion(NSDictionary<String, Object> transaction, NSError error);
    }

    public interface Block_fetchCurrentEntitlementsWithCompletion {
        void call_fetchCurrentEntitlementsWithCompletion(NSArray<NSDictionary<String, Object>> transactions, NSError error);
    }

    public interface Block_restorePurchasesWithCompletion {
        void call_restorePurchasesWithCompletion(NSArray<NSDictionary<String, Object>> transactions, NSError error);
    }

    public interface Block_startObservingTransactionsWithCompletion {
        void call_startObservingTransactionsWithCompletion(NSDictionary<String, Object> transaction, NSError error);
    }
}
