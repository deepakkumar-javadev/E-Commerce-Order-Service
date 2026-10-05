
package com.deepak.orderService.kafka;

import java.util.ArrayList;
import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.deepak.orderService.Dto.OrderItemResponseDto;
import com.deepak.orderService.Dto.orderResponseDto;
import com.deepak.orderService.service.orderService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

	public final orderService orderservice;
	public final OrderEventProducer orderEventProducer;

	// =====================================================
	// 1. AFTER ONLINE PAYMENT DONE
	// =====================================================

	@KafkaListener(topics = "payment.success", groupId = "order-service", containerFactory = "paymentKafkaListenerContainerFactory")
	public void ConsumePaymentSuccess(PaymentSuccessEvent event) {

		// 1. Find Order by Order ID
		orderResponseDto order = orderservice.getOrderForInventory(event.getOrderId());

		// 2. Update Payment Status
		orderservice.updatePaymentStatus(event.getOrderId(), event.getPaymentStatus());

		// 3. Prepare Inventory Reserve Event
		InventoryReserveEvent inventoryEvent = new InventoryReserveEvent();

		inventoryEvent.setOrderId(event.getOrderId());

		// 4. Prepare Inventory Items
		List<InventoryItemEvent> items = new ArrayList<>();

		// 5. Add SKU and Quantity for each Order Item
		for (OrderItemResponseDto item : order.getItems()) {

			InventoryItemEvent itemEvent = new InventoryItemEvent();

			itemEvent.setSkuCode(item.getSkuCode());

			itemEvent.setQuantity(item.getQuantity());

			items.add(itemEvent);
		}

		// 6. Set Items in Inventory Event
		inventoryEvent.setItems(items);

		
		System.out.println("========== INVENTORY RESERVE ==========");
		System.out.println("Order ID = " + inventoryEvent.getOrderId());
		System.out.println("Items = " + inventoryEvent.getItems());

		
		// 7. Publish inventory.reserve event
		orderEventProducer.publishInventoryReserve(inventoryEvent);
	
		System.out.println("========== INVENTORY RESERVE SENT ==========");
	}

	// =====================================================
	// 2. AFTER INVENTORY RESERVED for online
	// =====================================================

	@KafkaListener(topics = "inventory.reserved", groupId = "order-service", containerFactory = "inventoryKafkaListenerContainerFactory")
	public void ConsumerUpdateOrderStatus(OrderReservedEvent event) {

		System.out.println("Inventory reserved for Order ID: " + event.getOrderId());

		// Inventory Service sends status = RESERVED
		if ("RESERVED".equals(event.getStatus())) {

			System.out.println("Inventory successfully reserved. " + "Confirming order: " + event.getOrderId());

			// This method is COMMON for:
			// 1. COD
			// 2. ONLINE
			orderservice.confirmOrder(event.getOrderId());
		}
	}
}
