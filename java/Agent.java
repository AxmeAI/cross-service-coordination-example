/*
 * Order coordinator agent — Java example.
 *
 * Fetches an intent by ID, coordinates inventory + payment + shipping,
 * and resumes with result.
 *
 * Usage:
 *   export AXME_API_KEY="<agent-key>"
 *   javac -cp axme-sdk.jar Agent.java
 *   java -cp .:axme-sdk.jar Agent <intent_id>
 */

import dev.axme.sdk.AxmeClient;
import dev.axme.sdk.AxmeClientConfig;
import dev.axme.sdk.RequestOptions;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class Agent {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java Agent <intent_id>");
            System.exit(1);
        }

        String apiKey = System.getenv("AXME_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Error: AXME_API_KEY not set.");
            System.exit(1);
        }

        String intentId = args[0];
        var client = new AxmeClient(AxmeClientConfig.forCloud(apiKey));

        System.out.println("Processing intent: " + intentId);

        var intentData = client.getIntent(intentId, new RequestOptions());
        @SuppressWarnings("unchecked")
        var intent = (Map<String, Object>) intentData.getOrDefault("intent", intentData);
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) intent.getOrDefault("payload", Map.of());
        if (payload.containsKey("parent_payload")) {
            @SuppressWarnings("unchecked")
            var pp = (Map<String, Object>) payload.get("parent_payload");
            payload = pp;
        }

        String orderId = (String) payload.getOrDefault("order_id", "unknown");
        @SuppressWarnings("unchecked")
        var items = (List<?>) payload.getOrDefault("items", List.of());
        String customer = (String) payload.getOrDefault("customer_id", "unknown");
        String address = (String) payload.getOrDefault("shipping_address", "unknown");

        // Step 1: Inventory
        System.out.println("  [1/3] Reserving inventory for " + items.size() + " item(s)...");
        Thread.sleep(1000);

        // Step 2: Payment
        System.out.println("  [2/3] Charging customer " + customer + "...");
        Thread.sleep(1000);

        // Step 3: Shipping
        System.out.println("  [3/3] Creating shipment to " + address + "...");
        Thread.sleep(1000);

        var result = Map.<String, Object>of(
            "action", "complete",
            "order_id", orderId,
            "inventory_reserved", true,
            "payment_captured", true,
            "tracking_number", "SHIP-44321",
            "completed_at", Instant.now().toString()
        );

        client.resumeIntent(intentId, result, new RequestOptions());
        System.out.println("  Order " + orderId + " completed. Tracking: SHIP-44321");
    }
}
