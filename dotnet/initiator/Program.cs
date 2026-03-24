// Cross-service coordination — .NET example.
//
// Order flow: inventory-service → payment-service → shipping-service.
// No central orchestrator, no saga pattern boilerplate.
//
// Usage:
//   export AXME_API_KEY="your-key"
//   dotnet run

using Axme.Sdk;
using System.Text.Json.Nodes;

var client = new AxmeClient(new AxmeClientConfig
{
    ApiKey = Environment.GetEnvironmentVariable("AXME_API_KEY")!
});

// Submit order — coordinates across inventory, payment, and shipping
var intentId = await client.SendIntentAsync(new JsonObject
{
    ["intent_type"] = "order.place.v1",
    ["to_agent"] = "agent://myorg/production/order-service",
    ["payload"] = new JsonObject
    {
        ["order_id"] = "ord_98765",
        ["items"] = new JsonArray
        {
            new JsonObject { ["sku"] = "LAPTOP-001", ["quantity"] = 1, ["price_cents"] = 129900 },
            new JsonObject { ["sku"] = "CHARGER-USB-C", ["quantity"] = 2, ["price_cents"] = 2999 }
        },
        ["shipping_address"] = new JsonObject
        {
            ["street"] = "123 Main St",
            ["city"] = "San Francisco",
            ["state"] = "CA",
            ["zip"] = "94102"
        }
    }
});
Console.WriteLine($"Order submitted: {intentId}");

// Wait for full multi-service workflow to complete
var result = await client.WaitForAsync(intentId);
Console.WriteLine($"Final status: {result["status"]}");
