package com.deepak.orderService.service;

import java.util.List;

import com.deepak.orderService.Dto.OrderItemResponseDto;
import com.deepak.orderService.Dto.orderRequestDto;
import com.deepak.orderService.Dto.orderResponseDto;
import com.deepak.orderService.entity.OrderStatus;
import com.deepak.orderService.entity.PaymentStatus;

public interface orderService {

	public orderResponseDto placeOrder(Long userId);

	public orderResponseDto createOrder(orderRequestDto request);

	public orderResponseDto getOrderById(Long id);

	public orderResponseDto getOrderForInventory(Long orderId);

	public List<orderResponseDto> getOrderByuserId(Long userid);

	public orderResponseDto cancelOrder(Long id);

	public orderResponseDto updateStatus(Long orderId, OrderStatus status);

	public List<OrderItemResponseDto> getOrderItems(Long orderId);

	public orderResponseDto updatestatusfromPaymentService(Long orderId, PaymentStatus status);

	// kafka
	public void updatePaymentStatus(Long orderId, PaymentStatus status);

	// kafka online
	public void confirmOrder(Long orderId);

	// UPDATE ORDER-STATUS
	public orderResponseDto updateOrderStatus(Long orderId, OrderStatus status);

//	// additonal services 
//	public orderResponseDto orderStatus();
//
//	public orderResponseDto orderHistory(Long id);
//
//	public orderResponseDto reduceInventory(Long id);
//
//	public orderResponseDto clearCart(Long id);
}