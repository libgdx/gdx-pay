import Foundation
import StoreKit

@objc(GDXStoreKit2Bridge)
@available(iOS 15.0, macOS 12.0, *)
public final class GDXStoreKit2Bridge: NSObject {
    @objc public static let shared = GDXStoreKit2Bridge()

    private var productsById: [String: Product] = [:]
    private var transactionUpdatesTask: Task<Void, Never>?

    @objc public func canMakePayments() -> Bool {
        AppStore.canMakePayments
    }

    @objc(fetchProductsWithIdentifiers:completion:)
    public func fetchProducts(
        withIdentifiers identifiers: [String],
        completion: @escaping ([[String: Any]]?, NSError?) -> Void
    ) {
        Task {
            do {
                let products = try await Product.products(for: identifiers)
                productsById = Dictionary(uniqueKeysWithValues: products.map { ($0.id, $0) })

                var mappedProducts: [[String: Any]] = []
                for product in products {
                    mappedProducts.append(await map(product))
                }

                completion(mappedProducts, nil)
            } catch {
                completion(nil, error as NSError)
            }
        }
    }

    @objc(purchaseWithIdentifier:completion:)
    public func purchase(
        withIdentifier identifier: String,
        completion: @escaping ([String: Any]?, NSError?) -> Void
    ) {
        Task {
            guard let product = productsById[identifier] else {
                completion(nil, bridgeError(code: 1, message: "Product not loaded: \(identifier)"))
                return
            }

            do {
                let result = try await product.purchase()

                switch result {
                case .success(let verificationResult):
                    let transaction = try checked(verificationResult)
                    await transaction.finish()
                    completion(map(transaction), nil)

                case .userCancelled:
                    completion(["cancelled": true, "productID": identifier], nil)

                case .pending:
                    completion(["pending": true, "productID": identifier], nil)

                @unknown default:
                    completion(nil, bridgeError(code: 2, message: "Unknown purchase result for \(identifier)"))
                }
            } catch {
                completion(nil, error as NSError)
            }
        }
    }

    @objc(fetchCurrentEntitlementsWithCompletion:)
    public func fetchCurrentEntitlements(
        completion: @escaping ([[String: Any]]?, NSError?) -> Void
    ) {
        Task {
            do {
                var transactions: [[String: Any]] = []

                for await result in Transaction.currentEntitlements {
                    let transaction = try checked(result)
                    transactions.append(map(transaction))
                }

                completion(transactions, nil)
            } catch {
                completion(nil, error as NSError)
            }
        }
    }

    @objc(restorePurchasesWithCompletion:)
    public func restorePurchases(
        completion: @escaping ([[String: Any]]?, NSError?) -> Void
    ) {
        Task {
            do {
                try await AppStore.sync()
                fetchCurrentEntitlements(completion: completion)
            } catch {
                completion(nil, error as NSError)
            }
        }
    }

    @objc(startObservingTransactionsWithCompletion:)
    public func startObservingTransactions(
        completion: @escaping ([String: Any]?, NSError?) -> Void
    ) {
        stopObservingTransactions()

        transactionUpdatesTask = Task {
            for await result in Transaction.updates {
                do {
                    let transaction = try checked(result)
                    await transaction.finish()
                    completion(map(transaction), nil)
                } catch {
                    completion(nil, error as NSError)
                }
            }
        }
    }

    @objc public func stopObservingTransactions() {
        transactionUpdatesTask?.cancel()
        transactionUpdatesTask = nil
    }

    private func checked<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .verified(let value):
            return value
        case .unverified(_, let error):
            throw error
        }
    }

    private func map(_ product: Product) async -> [String: Any] {
        var mapped: [String: Any] = [
            "id": product.id,
            "displayName": product.displayName,
            "description": product.description,
            "displayPrice": product.displayPrice,
            "currencyCode": product.priceFormatStyle.currencyCode,
            "price": NSDecimalNumber(decimal: product.price)
        ]

        if let subscription = product.subscription,
           let introductoryOffer = subscription.introductoryOffer {
            mapped["eligibleForIntroOffer"] = await subscription.isEligibleForIntroOffer
            mapped["introPeriodValue"] = introductoryOffer.period.value
            mapped["introPeriodUnit"] = map(introductoryOffer.period.unit)
            mapped["introPrice"] = NSDecimalNumber(decimal: introductoryOffer.price)
        }

        return mapped
    }

    private func map(_ transaction: Transaction) -> [String: Any] {
        [
            "productID": transaction.productID,
            "originalID": String(transaction.originalID),
            "purchaseDate": transaction.purchaseDate.timeIntervalSince1970,
            "jsonRepresentation": String(data: transaction.jsonRepresentation, encoding: .utf8) ?? "",
            "jsonRepresentationBase64": transaction.jsonRepresentation.base64EncodedString()
        ]
    }

    private func map(_ unit: Product.SubscriptionPeriod.Unit) -> String {
        switch unit {
        case .day:
            return "day"
        case .week:
            return "week"
        case .month:
            return "month"
        case .year:
            return "year"
        @unknown default:
            return "unknown"
        }
    }

    private func bridgeError(code: Int, message: String) -> NSError {
        NSError(
            domain: "com.badlogic.gdx.pay.iosmoe.storekit2",
            code: code,
            userInfo: [NSLocalizedDescriptionKey: message]
        )
    }
}
