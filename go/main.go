// Cross-service coordination — Go example.
//
// Order flow: inventory-service -> payment-service -> shipping-service.
// No central orchestrator, no saga pattern boilerplate.
//
// Usage:
//
//	export AXME_API_KEY="your-key"
//	go run main.go
package main

import (
	"context"
	"fmt"
	"log"
	"os"

	"github.com/AxmeAI/axme-sdk-go/axme"
)

func main() {
	client, err := axme.NewClient(axme.ClientConfig{
		APIKey: os.Getenv("AXME_API_KEY"),
	})
	if err != nil {
		log.Fatalf("create client: %v", err)
	}

	ctx := context.Background()

	// Submit order — coordinates across inventory, payment, and shipping
	intentID, err := client.SendIntent(ctx, map[string]any{
		"intent_type": "order.place.v1",
		"to_agent":    "agent://myorg/production/order-service",
		"order_id":    "ord_98765",
		"items": []map[string]any{
			{"sku": "LAPTOP-001", "quantity": 1, "price_cents": 129900},
			{"sku": "CHARGER-USB-C", "quantity": 2, "price_cents": 2999},
		},
		"shipping_address": map[string]any{
			"street": "123 Main St",
			"city":   "San Francisco",
			"state":  "CA",
			"zip":    "94102",
		},
	}, axme.RequestOptions{})
	if err != nil {
		log.Fatalf("send intent: %v", err)
	}
	fmt.Printf("Order submitted: %s\n", intentID)

	// Wait for full multi-service workflow to complete
	result, err := client.WaitFor(ctx, intentID, axme.ObserveOptions{})
	if err != nil {
		log.Fatalf("wait: %v", err)
	}
	fmt.Printf("Final status: %v\n", result["status"])
}
