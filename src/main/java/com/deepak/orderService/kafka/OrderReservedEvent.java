package com.deepak.orderService.kafka;


import lombok.Data;

@Data
public class OrderReservedEvent {


	private Long orderId;
	private Long userId;
	private String  status;
}
