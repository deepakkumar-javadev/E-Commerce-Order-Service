package com.deepak.orderService.Dto;

import lombok.Data;

@Data
public class orderRequestDto {
	
	private Long userId;
	private String paymentMethod;
}
