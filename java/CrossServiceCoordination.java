/*
 * Cross-service coordination — Java example.
 *
 * Order flow: inventory-service → payment-service → shipping-service.
 * No central orchestrator, no saga pattern boilerplate.
 *
 * Usage:
 *   export AXME_API_KEY="your-key"
 *   mvn compile exec:java -Dexec.mainClass="CrossServiceCoordination"
 */

import dev.axme.sdk.AxmeClient;
import dev.axme.sdk.AxmeClientConfig;
import dev.axme.sdk.RequestOptions;
import dev.axme.sdk.ObserveOptions;
import java.util.List;
import java.util.Map;

public class CrossServiceCoordination {
    public static void main(String[] args) throws Exception {
        var client = new AxmeClient(
            AxmeClientConfig.forCloud(System.getenv("AXME_API_KEY"))
        );

        // Submit order — coordinates across inventory, payment, and shipping
        String intentId = client.sendIntent(Map.of(
            "intent_type", "order.place.v1",
            "to_agent", "agent://myorg/production/order-service",
            "payload", Map.of(
                "order_id", "ord_98765",
                "items", List.of(
                    Map.of("sku", "LAPTOP-001", "quantity", 1, "price_cents", 129900),
                    Map.of("sku", "CHARGER-USB-C", "quantity", 2, "price_cents", 2999)
                ),
                "shipping_address", Map.of(
                    "street", "123 Main St",
                    "city", "San Francisco",
                    "state", "CA",
                    "zip", "94102"
                )
            )
        ), new RequestOptions());
        System.out.println("Order submitted: " + intentId);

        // Wait for full multi-service workflow to complete
        var result = client.waitFor(intentId, new ObserveOptions());
        System.out.println("Final status: " + result.get("status"));
    }
}
