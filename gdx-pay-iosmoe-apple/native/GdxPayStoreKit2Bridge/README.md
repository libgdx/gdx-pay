# GdxPayStoreKit2Bridge

This Swift package is the native StoreKit 2 bridge used by the MOE backend.

The public API intentionally exposes Objective-C compatible Foundation types only:

* `String`
* `Array`
* `Dictionary`
* `NSNumber` / `NSError`
* callback blocks

That keeps the Java side compatible with NatJ/WrapNatJGen while the actual StoreKit 2 implementation can use Swift concurrency internally.

## Build outline

Build this package as an iOS framework or XCFramework, then generate NatJ bindings for `GDXStoreKit2Bridge`.

Typical local flow:

```sh
swift build
```

For app integration, prefer an `.xcframework` containing simulator and device slices. The Java binding in `src/main/java/com/badlogic/gdx/pay/ios/apple/bindings/GDXStoreKit2Bridge.java` mirrors the Objective-C selectors exported by this Swift class.
