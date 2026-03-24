// Order coordinator agent — .NET example.
//
// Fetches an intent by ID, coordinates inventory + payment + shipping,
// and resumes with result.
//
// Usage:
//   export AXME_API_KEY="<agent-key>"
//   dotnet run -- <intent_id>

using Axme.Sdk;
using System.Text.Json.Nodes;

if (args.Length < 1)
{
    Console.Error.WriteLine("Usage: dotnet run -- <intent_id>");
    return 1;
}

var apiKey = Environment.GetEnvironmentVariable("AXME_API_KEY");
if (string.IsNullOrEmpty(apiKey))
{
    Console.Error.WriteLine("Error: AXME_API_KEY not set.");
    return 1;
}

var intentId = args[0];
var client = new AxmeClient(new AxmeClientConfig { ApiKey = apiKey });

Console.WriteLine($"Processing intent: {intentId}");

var intentData = await client.GetIntentAsync(intentId);
var intent = intentData["intent"]?.AsObject() ?? intentData;
var payload = intent["payload"]?.AsObject() ?? new JsonObject();
if (payload["parent_payload"] is JsonObject parentPayload)
{
    payload = parentPayload;
}

var orderId = payload["order_id"]?.ToString() ?? "unknown";
var items = payload["items"]?.AsArray() ?? new JsonArray();
var customer = payload["customer_id"]?.ToString() ?? "unknown";
var address = payload["shipping_address"]?.ToString() ?? "unknown";

// Step 1: Inventory
Console.WriteLine($"  [1/3] Reserving inventory for {items.Count} item(s)...");
await Task.Delay(1000);

// Step 2: Payment
Console.WriteLine($"  [2/3] Charging customer {customer}...");
await Task.Delay(1000);

// Step 3: Shipping
Console.WriteLine($"  [3/3] Creating shipment to {address}...");
await Task.Delay(1000);

var result = new JsonObject
{
    ["action"] = "complete",
    ["order_id"] = orderId,
    ["inventory_reserved"] = true,
    ["payment_captured"] = true,
    ["tracking_number"] = "SHIP-44321",
    ["completed_at"] = DateTime.UtcNow.ToString("o")
};

await client.ResumeIntentAsync(intentId, result);
Console.WriteLine($"  Order {orderId} completed. Tracking: SHIP-44321");
return 0;
