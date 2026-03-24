/**
 * Order coordinator agent — TypeScript example.
 *
 * Coordinates inventory reservation, payment, and shipping in sequence.
 *
 * Usage:
 *   export AXME_API_KEY="<agent-key>"
 *   npx tsx agent.ts
 */

import { AxmeClient } from "@axme/axme";

const AGENT_ADDRESS = "order-coordinator-demo";

async function handleIntent(client: AxmeClient, intentId: string) {
  const intentData = await client.getIntent(intentId);
  const intent = intentData.intent ?? intentData;
  let payload = intent.payload ?? {};
  if (payload.parent_payload) {
    payload = payload.parent_payload;
  }

  const orderId = payload.order_id ?? "unknown";
  const items: any[] = payload.items ?? [];
  const customer = payload.customer_id ?? "unknown";
  const address = payload.shipping_address ?? "unknown";

  // Step 1: Inventory
  console.log(`  [1/3] Reserving inventory for ${items.length} item(s)...`);
  await new Promise((r) => setTimeout(r, 1000));

  // Step 2: Payment
  console.log(`  [2/3] Charging customer ${customer}...`);
  await new Promise((r) => setTimeout(r, 1000));

  // Step 3: Shipping
  console.log(`  [3/3] Creating shipment to ${address}...`);
  await new Promise((r) => setTimeout(r, 1000));

  const result = {
    action: "complete",
    order_id: orderId,
    inventory_reserved: true,
    payment_captured: true,
    tracking_number: "SHIP-44321",
    completed_at: new Date().toISOString(),
  };

  await client.resumeIntent(intentId, result, { ownerAgent: "order-coordinator-demo" });
  console.log(`  Order ${orderId} completed. Tracking: SHIP-44321`);
}

async function main() {
  const apiKey = process.env.AXME_API_KEY;
  if (!apiKey) {
    console.error("Error: AXME_API_KEY not set.");
    process.exit(1);
  }

  const client = new AxmeClient({ apiKey });

  console.log(`Agent listening on ${AGENT_ADDRESS}...`);
  console.log("Waiting for intents (Ctrl+C to stop)\n");

  for await (const delivery of client.listen(AGENT_ADDRESS)) {
    const intentId = delivery.intent_id;
    const status = delivery.status;
    if (intentId && ["DELIVERED", "CREATED", "IN_PROGRESS"].includes(status)) {
      console.log(`[${status}] Intent received: ${intentId}`);
      try {
        await handleIntent(client, intentId);
      } catch (e) {
        console.error(`  Error: ${e}`);
      }
    }
  }
}

main().catch(console.error);
