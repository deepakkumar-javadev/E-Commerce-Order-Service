package com.deepak.orderService.Dto;

import com.deepak.orderService.entity.PaymentStatus;

import lombok.Data;

@Data
public class PaymentResponseDto {

	private String razorpayOrderId;
	private String currency;
	private Long amount;
	private String key;
	private PaymentStatus paymentStatus;
	private String paymentLink;

}
