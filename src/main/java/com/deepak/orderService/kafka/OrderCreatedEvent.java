package com.deepak.orderService.kafka;

import java.util.List;

import lombok.Data;

@Data
public class OrderCreatedEvent {

	private Long orderId;
	private Long userId;
	private String orderNumber;
	private Double totalAmount;
	private String paymentMethod;
	private List<OrderItemEvent> items;
}
