package com.deepak.orderService.Dto;

import lombok.Data;

@Data
public class PaymentRequestDto {

	private Long orderId;

	private Double amount;
	
	private String paymentMethod;
	
	private String orderNumber;
}
