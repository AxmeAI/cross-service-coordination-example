// Cross-service coordination — Go example.
//
// Order flow: inventory-service → payment-service → shipping-service.
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
	client := axme.NewClient(axme.Config{
		APIKey: os.Getenv("AXME_API_KEY"),
	})

	ctx := context.Background()

	// Submit order — coordinates across inventory, payment, and shipping
	intentID, err := client.SendIntent(ctx, axme.SendIntentRequest{
		IntentType: "order.place.v1",
		ToAgent:    "agent://myorg/production/order-service",
		Payload: map[string]interface{}{
			"order_id": "ord_98765",
			"items": []map[string]interface{}{
				{"sku": "LAPTOP-001", "quantity": 1, "price_cents": 129900},
				{"sku": "CHARGER-USB-C", "quantity": 2, "price_cents": 2999},
			},
			"shipping_address": map[string]interface{}{
				"street": "123 Main St",
				"city":   "San Francisco",
				"state":  "CA",
				"zip":    "94102",
			},
		},
	})
	if err != nil {
		log.Fatalf("send intent: %v", err)
	}
	fmt.Printf("Order submitted: %s\n", intentID)

	// Wait for full multi-service workflow to complete
	result, err := client.WaitFor(ctx, intentID)
	if err != nil {
		log.Fatalf("wait: %v", err)
	}
	fmt.Printf("Final status: %s\n", result.Status)
}
