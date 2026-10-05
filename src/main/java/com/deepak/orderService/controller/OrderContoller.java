package com.deepak.orderService.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.deepak.orderService.Dto.OrderItemResponseDto;
import com.deepak.orderService.Dto.orderRequestDto;
import com.deepak.orderService.Dto.orderResponseDto;
import com.deepak.orderService.entity.OrderStatus;
import com.deepak.orderService.entity.PaymentStatus;
import com.deepak.orderService.service.orderService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/orders")
@RestController
public class OrderContoller {

	private final orderService service;

	// # ADMIN ONLY APIs

	// 1. UPDATE ORDER-STATUS
	@PutMapping("/{orderId}/status")
	public ResponseEntity<orderResponseDto> updateOrderStatus(@PathVariable Long orderId,
			@RequestParam OrderStatus status) {

		return ResponseEntity.ok(service.updateOrderStatus(orderId, status));
	}

	// # CUSTOMER ONLY APIs

	// 2. create order
	@PostMapping("/placeOrder")
	public ResponseEntity<orderResponseDto> placeOrder(@RequestBody orderRequestDto request) {

		orderResponseDto placeOrderRes = service.createOrder(request);

		return ResponseEntity.ok(placeOrderRes);
	}

	// 3.Order CANCEL

	@PutMapping("/{id}/cancel")
	public ResponseEntity<orderResponseDto> cancellOrder(@PathVariable Long id) {

		orderResponseDto placeOrderRes = service.cancelOrder(id);
		return ResponseEntity.ok(placeOrderRes);
	}

	// # CUSTOMER + ADMIN APIs

	// 4. Get order

	@GetMapping("/getorder/{id}")
	public ResponseEntity<orderResponseDto> getOrders(@PathVariable Long id) {

		orderResponseDto placeOrderRes = service.getOrderById(id);
		return ResponseEntity.ok(placeOrderRes);

	}

	// 5. get all orders of a user

	@GetMapping("/user/{userid}")
	public ResponseEntity<List<orderResponseDto>> getAllOrdersByUserId(@PathVariable Long userid) {

		List<orderResponseDto> orderslist = service.getOrderByuserId(userid);
		return ResponseEntity.ok(orderslist);
	}

	//6. get order items
	@GetMapping("/{orderId}/items")
	public ResponseEntity<List<OrderItemResponseDto>> getOrderItems(@PathVariable Long orderId) {

		return ResponseEntity.ok(service.getOrderItems(orderId));
	}

	// # INTERNAL/AUTHENTICATED/CALLED THROUGH FeignClient.
	
	// update stock after place order (update order status )

	@PutMapping("/{orderId}/payment-status")
	public ResponseEntity<?> updatePaymentStatus(@PathVariable Long orderId, @RequestParam OrderStatus status) {

		return ResponseEntity.ok(service.updateStatus(orderId, status));
	}



	// UPDATE PAYMENT-STATUS
	@PutMapping("/{orderId}/payment-order-status")
	public ResponseEntity<?> updatePaymentStatusfromPaymentService(@PathVariable Long orderId,
			@RequestParam PaymentStatus status) {

		return ResponseEntity.ok(service.updatestatusfromPaymentService(orderId, status));
	}

}