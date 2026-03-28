# Cross-Service Coordination Example

Services A, B, C need to coordinate. You build a central orchestrator. Or implement sagas with compensating transactions. Or add a message queue and hope for the best. Every approach needs its own failure-handling infrastructure.

**There is a better way.** Model each step as an intent. The platform coordinates delivery, tracks progress, and handles failures across services.

> **Alpha** · Built with [AXME](https://github.com/AxmeAI/axme) (AXP Intent Protocol).
> [cloud.axme.ai](https://cloud.axme.ai) · [hello@axme.ai](mailto:hello@axme.ai)

---

## The Problem

An order flow touches three services: inventory, payment, shipping. You need them to coordinate reliably:

```
Order placed → reserve inventory → charge payment → create shipment
                    ↓ failure          ↓ failure
              release inventory   refund payment + release inventory
```

What you end up building:
- **Central orchestrator** — single point of failure, owns all business logic
- **Saga pattern** — compensating transactions for every step, state machine, dead letter queues
- **Message queues** — RabbitMQ/Kafka between every pair of services, consumer groups, DLQs
- **Distributed transactions** — 2PC across services (fragile, slow, rarely works in practice)
- **Monitoring** — correlation IDs, distributed tracing, alerting on stuck workflows

---

## The Solution: Multi-Step Intent Workflow

```
Client → send_intent("place order")
         ↓
   inventory-service → reserve
         ↓
   payment-service → charge
         ↓
   shipping-service → create shipment
         ↓
   COMPLETED (or platform handles rollback)
```

Each service step is a durable intent. The platform coordinates delivery, tracks the workflow, and provides full observability.

---

## Quick Start

### Python

```bash
pip install axme
export AXME_API_KEY="your-key"   # Get one: axme login
```

```python
from axme import AxmeClient, AxmeClientConfig
import os

client = AxmeClient(AxmeClientConfig(api_key=os.environ["AXME_API_KEY"]))

# Submit order — coordinates across inventory, payment, and shipping
intent_id = client.send_intent({
    "intent_type": "order.place.v1",
    "to_agent": "agent://myorg/production/order-service",
    "payload": {
        "order_id": "ord_98765",
        "items": [
            {"sku": "LAPTOP-001", "quantity": 1, "price_cents": 129900},
            {"sku": "CHARGER-USB-C", "quantity": 2, "price_cents": 2999},
        ],
        "shipping_address": {
            "street": "123 Main St",
            "city": "San Francisco",
            "state": "CA",
            "zip": "94102",
        },
    },
})

print(f"Order submitted: {intent_id}")

# Watch the full multi-service workflow
for event in client.observe(intent_id):
    print(f"  [{event['status']}] {event['event_type']}")
    if event["status"] in ("COMPLETED", "FAILED", "TIMED_OUT"):
        break
```

### TypeScript

```bash
npm install @axme/axme
```

```typescript
import { AxmeClient } from "@axme/axme";

const client = new AxmeClient({ apiKey: process.env.AXME_API_KEY! });

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

const result = await client.waitFor(intentId);
console.log(`Done: ${result.status}`);
```

---

## More Languages

Full implementations in all 5 languages:

| Language | Directory | Install |
|----------|-----------|---------|
| [Python](python/) | `python/` | `pip install axme` |
| [TypeScript](typescript/) | `typescript/` | `npm install @axme/axme` |
| [Go](go/) | `go/` | `go get github.com/AxmeAI/axme-sdk-go` |
| [Java](java/) | `java/` | Maven Central: `ai.axme:axme-sdk` |
| [.NET](dotnet/) | `dotnet/` | `dotnet add package Axme.Sdk` |

---

## Before / After

### Before: Saga Pattern + Message Queues (400+ lines)

```python
class OrderSaga:
    """State machine with compensating transactions for every step."""

    async def execute(self, order):
        # Step 1: Reserve inventory
        try:
            reservation = await inventory_client.reserve(order.items)
        except Exception:
            return SagaResult(status="failed", step="inventory")

        # Step 2: Charge payment
        try:
            charge = await payment_client.charge(order.total, order.payment_method)
        except Exception:
            # Compensate: release inventory
            await inventory_client.release(reservation.id)
            return SagaResult(status="failed", step="payment")

        # Step 3: Create shipment
        try:
            shipment = await shipping_client.create(order.address, reservation.id)
        except Exception:
            # Compensate: refund payment + release inventory
            await payment_client.refund(charge.id)
            await inventory_client.release(reservation.id)
            return SagaResult(status="failed", step="shipping")

        return SagaResult(status="completed", shipment_id=shipment.id)

# Plus: saga state table, dead letter queues, retry consumers,
# compensation failure handling, distributed tracing, correlation IDs...
```

### After: AXME Multi-Step Workflow (20 lines)

```python
from axme import AxmeClient, AxmeClientConfig

client = AxmeClient(AxmeClientConfig(api_key=os.environ["AXME_API_KEY"]))

intent_id = client.send_intent({
    "intent_type": "order.place.v1",
    "to_agent": "agent://myorg/production/order-service",
    "payload": {
        "order_id": "ord_98765",
        "items": [
            {"sku": "LAPTOP-001", "quantity": 1, "price_cents": 129900},
        ],
        "shipping_address": {"city": "San Francisco", "state": "CA"},
    },
})

for event in client.observe(intent_id):
    print(f"[{event['status']}] {event['event_type']}")
    if event["status"] in ("COMPLETED", "FAILED"):
        break
```

No saga state machine. No compensating transactions. No message queues. No dead letter handling. No distributed tracing setup.

---

## How It Works

```
┌────────────┐  send_intent()   ┌────────────────┐
│            │ ───────────────> │                │
│   Client   │                  │   AXME Cloud   │
│            │ <─ observe(SSE)  │   (platform)   │
└────────────┘                  └───────┬────────┘
                                        │
                  ┌─────────────────────┼─────────────────────┐
                  │                     │                     │
          ┌───────▼────────┐    ┌───────▼────────┐    ┌───────▼────────┐
          │   Inventory    │    │    Payment     │    │   Shipping     │
          │   Service      │    │    Service     │    │   Service      │
          │   (agent)      │    │    (agent)     │    │   (agent)      │
          │                │    │                │    │                │
          │   reserve()    │    │   charge()     │    │   ship()       │
          └────────────────┘    └────────────────┘    └────────────────┘

Step 1: reserve inventory -> Step 2: charge payment -> Step 3: create shipment
```

1. Client submits an order **intent** via AXME SDK
2. Order service agent receives the intent, coordinates the workflow
3. Each step (inventory, payment, shipping) is a **sub-intent** with delivery guarantees
4. Platform tracks the full workflow — retries failed steps, provides observability
5. Client **observes** every step via SSE — one stream for the entire multi-service flow
6. If any step fails, the platform provides failure context for compensation

---

## Run the Full Example

### Prerequisites

```bash
# Install CLI (one-time)
curl -fsSL https://raw.githubusercontent.com/AxmeAI/axme-cli/main/install.sh | sh
# Open a new terminal, or run the "source" command shown by the installer

# Log in
axme login

# Install Python SDK
pip install axme
```

### Terminal 1 - submit the intent

```bash
axme scenarios apply scenario.json
# Note the intent_id in the output
```

### Terminal 2 - start the agent

Get the agent key after scenario apply:

```bash
# macOS
cat ~/Library/Application\ Support/axme/scenario-agents.json | grep -A2 order-coordinator-demo

# Linux
cat ~/.config/axme/scenario-agents.json | grep -A2 order-coordinator-demo
```

Then run the agent in your language of choice:

```bash
# Python (SSE stream listener)
AXME_API_KEY=<agent-key> python agent.py

# TypeScript (SSE stream listener, requires Node 20+)
cd typescript && npm install
AXME_API_KEY=<agent-key> npx tsx agent.ts

# Go (SSE stream listener)
cd go && go run ./cmd/agent/

# Java (processes a single intent by ID)
cd java/agent && mvn compile
AXME_API_KEY=<agent-key> mvn -q exec:java -Dexec.mainClass="Agent" -Dexec.args="<step-intent-id>"

# .NET (processes a single intent by ID)
cd dotnet/agent && dotnet run -- <step-intent-id>
```

### Verify

```bash
axme intents get <intent_id>
# lifecycle_status: COMPLETED
```

---

## Related

- [AXME](https://github.com/AxmeAI/axme) — project overview
- [AXP Spec](https://github.com/AxmeAI/axp-spec) — open Intent Protocol specification
- [AXME Examples](https://github.com/AxmeAI/axme-examples) — 20+ runnable examples across 5 languages
- [AXME CLI](https://github.com/AxmeAI/axme-cli) — manage intents, agents, scenarios from the terminal

---

Built with [AXME](https://github.com/AxmeAI/axme) (AXP Intent Protocol).
