/**
 * Cross-service coordination — TypeScript example.
 *
 * Order flow: inventory-service → payment-service → shipping-service.
 * No central orchestrator, no saga pattern boilerplate.
 *
 * Usage:
 *   npm install @axme/axme
 *   export AXME_API_KEY="your-key"
 *   npx tsx main.ts
 */

import { AxmeClient } from "@axme/axme";

async function main() {
  const client = new AxmeClient({ apiKey: process.env.AXME_API_KEY! });

  // Submit order — coordinates across inventory, payment, and shipping
  const intentId = await client.sendIntent({
    intentType: "order.place.v1",
    toAgent: "agent://myorg/production/order-service",
    payload: {
      orderId: "ord_98765",
      items: [
        { sku: "LAPTOP-001", quantity: 1, priceCents: 129900 },
        { sku: "CHARGER-USB-C", quantity: 2, priceCents: 2999 },
      ],
      shippingAddress: {
        street: "123 Main St",
        city: "San Francisco",
        state: "CA",
        zip: "94102",
      },
    },
  });
  console.log(`Order submitted: ${intentId}`);

  // Wait for full multi-service workflow to complete
  const result = await client.waitFor(intentId);
  console.log(`Final status: ${result.status}`);
}

main().catch(console.error);
