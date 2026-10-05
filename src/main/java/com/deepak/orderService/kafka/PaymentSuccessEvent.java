package com.deepak.orderService.kafka;

import com.deepak.orderService.entity.PaymentStatus;

import lombok.Data;

@Data
public class PaymentSuccessEvent {

	private Long orderId;
	private Long paymentId;
	private Double amount;
	private String paymentMethod;
	private PaymentStatus paymentStatus;
	private String transactionId;
	
	
}
