package com.deepak.orderService.Dto;

import java.time.LocalDateTime;
import java.util.List;

import com.deepak.orderService.entity.OrderStatus;
import com.deepak.orderService.entity.PaymentStatus;

import lombok.Data;

@Data
public class orderResponseDto {

    // Order Details
	private Long userId;
    private Long orderId;
    private String orderNumber;
    private Double totalAmount;
    private OrderStatus orderStatus;
    private LocalDateTime orderDate;

    // Payment Details
    private String paymentMethod;
    private PaymentStatus paymentStatus;
    private String razorpayOrderId;
    private String currency;
	private String paymentLink;
	private String key;

    // Order Items
    private List<OrderItemResponseDto> items;

    // Message
    private String msg;
	
}
