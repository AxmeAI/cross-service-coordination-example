"""Order coordinator agent - inventory, payment, shipping in sequence."""

import os, sys, time
sys.stdout.reconfigure(line_buffering=True)
from axme import AxmeClient, AxmeClientConfig

AGENT_ADDRESS = "order-coordinator-demo"

def handle_intent(client, intent_id):
    intent_data = client.get_intent(intent_id)
    intent = intent_data.get("intent", intent_data)
    payload = intent.get("payload", {})
    if "parent_payload" in payload:
        payload = payload["parent_payload"]

    order_id = payload.get("order_id", "unknown")
    items = payload.get("items", [])
    customer = payload.get("customer_id", "unknown")
    address = payload.get("shipping_address", "unknown")

    # Step 1: Inventory
    print(f"  [1/3] Reserving inventory for {len(items)} item(s)...")
    time.sleep(1)

    # Step 2: Payment
    print(f"  [2/3] Charging customer {customer}...")
    time.sleep(1)

    # Step 3: Shipping
    print(f"  [3/3] Creating shipment to {address}...")
    time.sleep(1)

    result = {
        "action": "complete",
        "order_id": order_id,
        "inventory_reserved": True,
        "payment_captured": True,
        "tracking_number": "SHIP-44321",
        "completed_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
    }
    client.resume_intent(intent_id, result)
    print(f"  Order {order_id} completed. Tracking: SHIP-44321")

def main():
    api_key = os.environ.get("AXME_API_KEY", "")
    if not api_key:
        print("Error: AXME_API_KEY not set."); sys.exit(1)
    client = AxmeClient(AxmeClientConfig(api_key=api_key))
    print(f"Agent listening on {AGENT_ADDRESS}...")
    print("Waiting for intents (Ctrl+C to stop)\n")
    for delivery in client.listen(AGENT_ADDRESS):
        intent_id = delivery.get("intent_id", "")
        status = delivery.get("status", "")
        if intent_id and status in ("DELIVERED", "CREATED", "IN_PROGRESS"):
            print(f"[{status}] Intent received: {intent_id}")
            try:
                handle_intent(client, intent_id)
            except Exception as e:
                print(f"  Error: {e}")

if __name__ == "__main__":
    main()
