"""
Cross-service coordination — Python example.

Order flow: inventory-service → payment-service → shipping-service.
No central orchestrator, no saga pattern boilerplate.

Usage:
    pip install axme
    export AXME_API_KEY="your-key"
    python main.py
"""

import os
from axme import AxmeClient, AxmeClientConfig


def main():
    client = AxmeClient(
        AxmeClientConfig(api_key=os.environ["AXME_API_KEY"])
    )

    # Submit order — coordinates across inventory, payment, and shipping
    intent_id = client.send_intent(
        {
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
        }
    )
    print(f"Order submitted: {intent_id}")

    # Watch the full multi-service workflow
    print("Watching workflow...")
    for event in client.observe(intent_id):
        status = event.get("status", "")
        event_type = event.get("event_type", "")
        print(f"  [{status}] {event_type}")
        if status in ("COMPLETED", "FAILED", "TIMED_OUT", "CANCELLED"):
            break

    # Fetch final state
    intent = client.get_intent(intent_id)
    print(f"\nFinal status: {intent['intent']['lifecycle_status']}")


if __name__ == "__main__":
    main()
