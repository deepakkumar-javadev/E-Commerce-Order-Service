package com.deepak.orderService.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderEventProducer {

	private final KafkaTemplate<String, Object> kafkaTemplate;

	// COD
	public void publishOrderCreated(OrderCreatedEvent event) {

		kafkaTemplate.send("order.created", event);
	}

	//ONLINE
	public void publishInventoryReserve(InventoryReserveEvent event) {

		kafkaTemplate.send("inventory.reserve", event).whenComplete((result, ex) -> {

			if (ex != null) {
				System.out.println("❌ INVENTORY.RESERVE FAILED = " + ex.getMessage());
				ex.printStackTrace();

			} else {
				System.out.println("✅ INVENTORY.RESERVE SUCCESS");

				System.out.println("Topic = " + result.getRecordMetadata().topic());

				System.out.println("Partition = " + result.getRecordMetadata().partition());

				System.out.println("Offset = " + result.getRecordMetadata().offset());
			}
		});
	}

	// DELIVERED
	public void publishOrderDelivered(OrderDeliveredEvent event) {
		kafkaTemplate.send("order.delivered", event);
	}

	// DELIVERED → Inventory Commit
	public void publishInventoryCommit(InventoryCommitEvent event) {
		kafkaTemplate.send("inventory.commit", event);
	}

}
