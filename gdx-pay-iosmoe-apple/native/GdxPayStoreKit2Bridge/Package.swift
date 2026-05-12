// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "GdxPayStoreKit2Bridge",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "GdxPayStoreKit2Bridge",
            type: .dynamic,
            targets: ["GdxPayStoreKit2Bridge"]
        )
    ],
    targets: [
        .target(
            name: "GdxPayStoreKit2Bridge",
            path: "Sources/GdxPayStoreKit2Bridge"
        )
    ]
)
