package com.deepak.orderService.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deepak.orderService.Dto.CartItemResponseDto;
import com.deepak.orderService.Dto.InventoryResDto;
import com.deepak.orderService.Dto.OrderItemResponseDto;
import com.deepak.orderService.Dto.PaymentRequestDto;
import com.deepak.orderService.Dto.PaymentResponseDto;
import com.deepak.orderService.Dto.cartResponseDto;
import com.deepak.orderService.Dto.orderRequestDto;
import com.deepak.orderService.Dto.orderResponseDto;
import com.deepak.orderService.Repository.OrderRepository;
import com.deepak.orderService.entity.Order;
import com.deepak.orderService.entity.OrderItem;
import com.deepak.orderService.entity.OrderStatus;
import com.deepak.orderService.entity.PaymentStatus;
import com.deepak.orderService.feignClients.CartClient;
import com.deepak.orderService.feignClients.InventoryClient;
import com.deepak.orderService.feignClients.paymentClient;
import com.deepak.orderService.kafka.InventoryCommitEvent;
import com.deepak.orderService.kafka.InventoryCommitItem;
import com.deepak.orderService.kafka.OrderCreatedEvent;
import com.deepak.orderService.kafka.OrderDeliveredEvent;
import com.deepak.orderService.kafka.OrderEventProducer;
import com.deepak.orderService.kafka.OrderItemEvent;

import lombok.RequiredArgsConstructor;

// private final
@Service
@Transactional
@RequiredArgsConstructor
public class OrderServiceImpl implements orderService {

	@Value("${internal.service.token}")
	private String internalServiceToken;
	private final OrderRepository orderRepository;
	private final CartClient cartClient;
	private final InventoryClient inventoryClient;
	private final paymentClient paymentclient;

	// Kafka Producer
	private final OrderEventProducer orderEventProducer;

	// =====================================================
	// IMPORTANT
	//
	// DON'T REDUCE STOCK HERE
	// DON'T CLEAR CART HERE
	//
	// Inventory Service owns inventory.
	// Cart Service owns cart.
	//
	// COD:
	// order.created -> Kafka -> Inventory
	// @Override
	public orderResponseDto createOrder(orderRequestDto request) {

		// =====================================================
		// 1. GET CART
		// =====================================================

		cartResponseDto cart = cartClient.getCartByUserId(request.getUserId());

		if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {

			throw new RuntimeException("Cart is Empty");
		}

		// =====================================================
		// 2. VALIDATE PAYMENT METHOD
		// =====================================================

		if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {

			throw new RuntimeException("Payment method is required");
		}

		String paymentMethod = request.getPaymentMethod().toUpperCase();

		if (!paymentMethod.equals("COD") && !paymentMethod.equals("ONLINE")) {

			throw new RuntimeException("Invalid payment method");
		}

		// =====================================================
		// 3. IDEMPOTENCY CHECK
		// Only for ONLINE payment
		// =====================================================

		if (paymentMethod.equals("ONLINE")) {

			Optional<Order> existingPendingOrder = orderRepository
					.findFirstByUseridAndOrderstatusAndCreateAtAfterOrderByCreateAtDesc(request.getUserId(),
							OrderStatus.PENDING, LocalDateTime.now().minusMinutes(15));

			if (existingPendingOrder.isPresent()) {

				Order existing = existingPendingOrder.get();

				PaymentResponseDto existingPayment = paymentclient.getPayment(existing.getOrderid());

				orderResponseDto response = new orderResponseDto();

				response.setUserId(existing.getUserid());
				response.setOrderId(existing.getOrderid());
				response.setOrderNumber(existing.getOrderNumber());
				response.setTotalAmount(existing.getTotalAmount());
				response.setOrderStatus(existing.getOrderstatus());
				response.setOrderDate(existing.getCreateAt());
				response.setPaymentMethod(existing.getPaymentMethod());

				// Payment details
				if (existingPayment != null) {

					response.setPaymentStatus(existingPayment.getPaymentStatus());

					response.setRazorpayOrderId(existingPayment.getRazorpayOrderId());

					response.setPaymentLink(existingPayment.getPaymentLink());

					response.setCurrency(existingPayment.getCurrency());

					response.setKey(existingPayment.getKey());
				}

				// Order Items
				List<OrderItemResponseDto> itemList = new ArrayList<>();

				for (OrderItem item : existing.getOrderItems()) {

					OrderItemResponseDto itemResponse = new OrderItemResponseDto();

					itemResponse.setProductId(item.getProductId());

					itemResponse.setProductName(item.getProductName());

					itemResponse.setQuantity(item.getQuantity());

					itemResponse.setPrice(item.getPrice());

					itemResponse.setSkuCode(item.getSkuCode());

					itemList.add(itemResponse);
				}

				response.setItems(itemList);

				response.setMsg("Existing pending online order found. Reusing payment details.");

				return response;
			}
		}

		// =====================================================
		// 4. COLLECT SKU CODES
		// =====================================================

		List<String> skuCodes = cart.getItems().stream().map(CartItemResponseDto::getSkuCode).filter(Objects::nonNull)
				.distinct().toList();

		if (skuCodes.isEmpty()) {

			throw new RuntimeException("No valid SKU found in cart");
		}

		// =====================================================
		// 5. CHECK INVENTORY
		//
		// IMPORTANT:
		// This is only PRE-CHECK.
		// Actual reserve/reduce will happen
		// inside Inventory Service.
		// =====================================================

		List<InventoryResDto> inventories = inventoryClient.checkStock(skuCodes);

		if (inventories == null || inventories.isEmpty()) {

			throw new RuntimeException("Inventory information not found");
		}

		Map<String, InventoryResDto> inventoryMap = inventories.stream()
				.collect(Collectors.toMap(InventoryResDto::getSkuCode, Function.identity()));

		// =====================================================
		// 6. VALIDATE STOCK
		// =====================================================

		for (CartItemResponseDto item : cart.getItems()) {

			InventoryResDto inventory = inventoryMap.get(item.getSkuCode());

			if (inventory == null) {

				throw new RuntimeException(item.getSkuCode() + " inventory not found");
			}

			if (inventory.getStockQuantity() < item.getQuantity()) {

				throw new RuntimeException(item.getSkuCode() + " Out Of Stock");
			}
		}

		// =====================================================
		// 7. CALCULATE TOTAL
		// =====================================================

		double totalAmount = cart.getItems().stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();

		// =====================================================
		// 8. CREATE ORDER
		// =====================================================

		Order order = new Order();

		order.setUserid(request.getUserId());

		order.setOrderNumber(UUID.randomUUID().toString());

		order.setTotalAmount(totalAmount);

		order.setPaymentMethod(paymentMethod);

		order.setCreateAt(LocalDateTime.now());

		// =====================================================
		// COD
		// Order can be confirmed immediately
		// Payment will happen at delivery
		// =====================================================

		if (paymentMethod.equals("COD")) {

			order.setOrderstatus(OrderStatus.PENDING);

			order.setPaymentstatus(PaymentStatus.PENDING);

		}

		// =====================================================
		// ONLINE
		// Payment is still pending
		// =====================================================

		else {

			order.setOrderstatus(OrderStatus.PENDING);

			order.setPaymentstatus(PaymentStatus.PENDING);
		}

		Order savedOrder = orderRepository.save(order);

		// =====================================================
		// 9. SAVE ORDER ITEMS
		// =====================================================

		List<OrderItem> listItems = new ArrayList<>();

		for (CartItemResponseDto cartItem : cart.getItems()) {

			OrderItem item = new OrderItem();

			item.setOrder(savedOrder);

			item.setProductId(cartItem.getProductId());

			item.setProductName(cartItem.getProductName());

			item.setSkuCode(cartItem.getSkuCode());

			item.setQuantity(cartItem.getQuantity());

			item.setPrice(cartItem.getPrice());

			listItems.add(item);
		}

		savedOrder.setOrderItems(listItems);

		savedOrder = orderRepository.save(savedOrder);

		// =====================================================
		// 10. COD -> PUBLISH order.created

		// =====================================================

		if (paymentMethod.equals("COD")) {

			OrderCreatedEvent event = new OrderCreatedEvent();

			event.setOrderId(savedOrder.getOrderid());

			event.setUserId(savedOrder.getUserid());

			event.setOrderNumber(savedOrder.getOrderNumber());

			event.setTotalAmount(savedOrder.getTotalAmount());

			event.setPaymentMethod(savedOrder.getPaymentMethod());

			// Order Items Event
			List<OrderItemEvent> itemEvents = new ArrayList<>();

			for (OrderItem item : savedOrder.getOrderItems()) {

				OrderItemEvent itemEvent = new OrderItemEvent();

				itemEvent.setProductId(item.getProductId());

				itemEvent.setProductName(item.getProductName());

				itemEvent.setSkuCode(item.getSkuCode());

				itemEvent.setQuantity(item.getQuantity());

				itemEvent.setPrice(item.getPrice());

				itemEvents.add(itemEvent);
			}

			// set kafka event
			event.setItems(itemEvents);

			// =================================================
			// Kafka Producer
			//
			// COD ONLY
			//
			// Order Service
			// ↓
			// order.created
			// ↓
			// Kafka
			// ↓
			// Inventory Service
			// =================================================
			// sent kafka event on topic
			orderEventProducer.publishOrderCreated(event);

//			//CLEAR CART IN COD -> AFTER PLACE ORDER
//			cartClient.clearCart(savedOrder.getUserid());
		}

		// =====================================================
		// 11. PAYMENT SERVICE
		// =====================================================

		PaymentResponseDto paymentResponse = null;

		// =====================================================
		// COD PAYMENT | paymentorder creation for COD
		// =====================================================

		if (paymentMethod.equals("COD")) {

			PaymentRequestDto paymentRequest = new PaymentRequestDto();

			paymentRequest.setOrderId(savedOrder.getOrderid());

			paymentRequest.setAmount(savedOrder.getTotalAmount());

			paymentRequest.setPaymentMethod("COD");

			paymentRequest.setOrderNumber(savedOrder.getOrderNumber());

			/*
			 * Payment Service:
			 *
			 * COD: - Payment record create - paymentStatus = PENDING - NO Razorpay order -
			 * razorpayOrderId = null - paymentLink = null
			 */

			paymentResponse = paymentclient.createPaymentOrder(paymentRequest);
		}

		// =====================================================
		// ONLINE PAYMENT | Paymentorder creation for ONLINE
		// =====================================================

		else if (paymentMethod.equals("ONLINE")) {

			PaymentRequestDto paymentRequest = new PaymentRequestDto();

			paymentRequest.setOrderId(savedOrder.getOrderid());

			paymentRequest.setAmount(savedOrder.getTotalAmount());

			paymentRequest.setPaymentMethod("ONLINE");

			paymentRequest.setOrderNumber(savedOrder.getOrderNumber());

			/*
			 * Payment Service:
			 *
			 * ONLINE: - Razorpay order create - razorpayOrderId generate - paymentLink
			 * generate if required - paymentStatus = PENDING
			 */

			paymentResponse = paymentclient.createPaymentOrder(paymentRequest);
		}

		// ONLINE:
		// payment.completed -> Kafka -> Inventory
		// =====================================================

		// =====================================================
		// 12. CREATE RESPONSE
		// =====================================================

		orderResponseDto response = new orderResponseDto();

		// =====================================================
		// ORDER DETAILS
		// =====================================================

		response.setUserId(savedOrder.getUserid());

		response.setOrderId(savedOrder.getOrderid());

		response.setOrderNumber(savedOrder.getOrderNumber());

		response.setTotalAmount(savedOrder.getTotalAmount());

		response.setOrderStatus(savedOrder.getOrderstatus());

		response.setOrderDate(savedOrder.getCreateAt());

		response.setPaymentMethod(savedOrder.getPaymentMethod());

		// =====================================================
		// 13. ORDER ITEMS RESPONSE
		// =====================================================

		List<OrderItemResponseDto> itemList = new ArrayList<>();

		for (OrderItem item : savedOrder.getOrderItems()) {

			OrderItemResponseDto orderItemRes = new OrderItemResponseDto();

			orderItemRes.setProductId(item.getProductId());

			orderItemRes.setProductName(item.getProductName());

			orderItemRes.setQuantity(item.getQuantity());

			orderItemRes.setPrice(item.getPrice());

			orderItemRes.setSkuCode(item.getSkuCode());

			itemList.add(orderItemRes);
		}

		response.setItems(itemList);

		// =====================================================
		// 14. COD RESPONSE
		// =====================================================

		if (paymentMethod.equals("COD")) {

			response.setPaymentStatus(PaymentStatus.PENDING);

			// COD mein Razorpay nahi hai

			response.setRazorpayOrderId(null);

			response.setPaymentLink(null);

			response.setCurrency("INR");

			response.setMsg("COD Order Placed successfully");

			return response;
		}

		// =====================================================
		// 15. ONLINE RESPONSE
		// =====================================================

		if (paymentMethod.equals("ONLINE")) {

			if (paymentResponse == null) {

				throw new RuntimeException("Payment service response is null");
			}

			response.setPaymentStatus(paymentResponse.getPaymentStatus());

			response.setRazorpayOrderId(paymentResponse.getRazorpayOrderId());

			response.setPaymentLink(paymentResponse.getPaymentLink());

			response.setCurrency(paymentResponse.getCurrency());

			response.setKey(paymentResponse.getKey());

			response.setMsg("Order created. Please complete online payment.");

			return response;
		}

		throw new RuntimeException("Invalid payment method");
	}

	// fetch order details..

	@Override
	public orderResponseDto getOrderById(Long id) {
		// TODO Auto-generated method stub

		Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("order not found " + id));

		orderResponseDto response = new orderResponseDto();

		// =========================
		// Order Details
		// =========================
		response.setUserId(order.getUserid());
		response.setOrderId(order.getOrderid());
		response.setOrderNumber(order.getOrderNumber());
		response.setTotalAmount(order.getTotalAmount());
		response.setOrderStatus(order.getOrderstatus());
		response.setOrderDate(order.getCreateAt());

		// =========================
		// Payment Details
		// =========================

		PaymentResponseDto payment = paymentclient.getPayment(id);

		response.setPaymentMethod(order.getPaymentMethod());

		if (payment != null) {
			// Agar PaymentStatus Order entity mein hai
			response.setPaymentStatus(payment.getPaymentStatus());

			response.setRazorpayOrderId(payment.getRazorpayOrderId());
			response.setCurrency(payment.getCurrency());
			response.setPaymentLink(payment.getPaymentLink());
			response.setKey(payment.getKey());
		}
		// =========================
		// Order Items details
		// =========================
		List<OrderItemResponseDto> items = order.getOrderItems().stream().map(item -> {

			OrderItemResponseDto itemDto = new OrderItemResponseDto();

			itemDto.setProductId(item.getProductId());
			itemDto.setProductName(item.getProductName());
			itemDto.setQuantity(item.getQuantity());
			itemDto.setPrice(item.getPrice());
			itemDto.setSkuCode(item.getSkuCode());
			return itemDto;
		}).toList();

		response.setItems(items);

		// =========================
		// Message
		// =========================
		response.setMsg("Order fetched successfully");

		return response;

	}

	@Override
	public List<orderResponseDto> getOrderByuserId(Long userid) {

		List<Order> orders =
	            orderRepository.findByUseridOrderByCreateAtDesc(userid);

	    List<orderResponseDto> listOrders = new ArrayList<>();

	    for (Order order : orders) {

	        orderResponseDto response = new orderResponseDto();

	        // =========================
	        // ORDER DETAILS
	        // =========================

	        response.setUserId(order.getUserid());
	        response.setOrderId(order.getOrderid());
	        response.setOrderNumber(order.getOrderNumber());
	        response.setTotalAmount(order.getTotalAmount());
	        response.setOrderStatus(order.getOrderstatus());
	        response.setOrderDate(order.getCreateAt());
	        response.setPaymentMethod(order.getPaymentMethod());
	        response.setPaymentStatus(order.getPaymentstatus());

	        // =========================
	        // ORDER ITEMS
	        // =========================

	        List<OrderItemResponseDto> items =
	                order.getOrderItems().stream().map(item -> {

	                    OrderItemResponseDto dto =
	                            new OrderItemResponseDto();

	                    dto.setProductId(item.getProductId());
	                    dto.setProductName(item.getProductName());
	                    dto.setQuantity(item.getQuantity());
	                    dto.setPrice(item.getPrice());
	                    dto.setSkuCode(item.getSkuCode());

	                    return dto;

	                }).toList();

	        response.setItems(items);

	        // =========================
	        // MESSAGE
	        // =========================

	        response.setMsg("Order fetched successfully");

	        listOrders.add(response);
	    }

	    return listOrders;
	}

	@Override
	public orderResponseDto cancelOrder(Long id) {

		orderResponseDto resDto = new orderResponseDto();
		return resDto;
	}

	@Override
	public orderResponseDto placeOrder(Long userId) {

		orderResponseDto resDto = new orderResponseDto();
		return resDto;

	}

	// update order status ,......

	@Override
	public orderResponseDto updateStatus(Long orderId, OrderStatus status) {

		// 1. Find Order
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

		// 2. Update Status
		order.setOrderstatus(status);
		order.setUpdatedAt(LocalDateTime.now());

		// 3. Save Order
		Order updatedOrder = orderRepository.save(order);

		// 4. Prepare Response
		orderResponseDto response = new orderResponseDto();

		response.setOrderId(updatedOrder.getOrderid());
		response.setOrderNumber(updatedOrder.getOrderNumber());
		response.setTotalAmount(updatedOrder.getTotalAmount());
		response.setOrderStatus(updatedOrder.getOrderstatus());
		response.setOrderDate(updatedOrder.getCreateAt());

		response.setMsg("Order status updated successfully");

		return response;
	}

	// update paymentstatus
	public orderResponseDto updatestatusfromPaymentService(Long orderId, PaymentStatus status) {
		// 1. Find Order
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

		// 2. Update Payment Status
		order.setPaymentstatus(status);

		// Optional: update order status when payment is successful
		if (status == PaymentStatus.PAID) {
			order.setOrderstatus(OrderStatus.CONFIRMED);
		}

		order.setUpdatedAt(LocalDateTime.now());

		// 3. Save Order
		Order updatedOrder = orderRepository.save(order);

		// 4. Prepare Response
		orderResponseDto response = new orderResponseDto();

		response.setOrderId(updatedOrder.getOrderid());
		response.setOrderNumber(updatedOrder.getOrderNumber());
		response.setTotalAmount(updatedOrder.getTotalAmount());
		response.setOrderStatus(updatedOrder.getOrderstatus());
		response.setOrderDate(updatedOrder.getCreateAt());
		response.setMsg("Payment status updated successfully");

		return response;

	}

	@Override
	public List<OrderItemResponseDto> getOrderItems(Long orderId) {

		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

		return order.getOrderItems().stream().map(item -> {

			OrderItemResponseDto dto = new OrderItemResponseDto();

			dto.setProductId(item.getProductId());
			dto.setSkuCode(item.getSkuCode());
			dto.setQuantity(item.getQuantity());

			return dto;
		}).toList();

	}

	// kafka(update paymentstatus)
	@Override
	public void updatePaymentStatus(Long orderId, PaymentStatus status) {

		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

		order.setPaymentstatus(status);

		orderRepository.save(order);
	}

	public void confirmOrder(Long orderId) {

		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

		order.setOrderstatus(OrderStatus.CONFIRMED);
		orderRepository.save(order);

		// Clear cart after order confirmation
		cartClient.clearCart(order.getUserid(), internalServiceToken);
	}

	@Override
	public orderResponseDto updateOrderStatus(Long orderId, OrderStatus status) {

		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

		OrderStatus currentStatus = order.getOrderstatus();

		switch (status) {

		case PACKED:
			if (currentStatus != OrderStatus.CONFIRMED) {
				throw new RuntimeException("Order must be CONFIRMED before PACKED");

			}
			break;

		case SHIPPED:
			if (currentStatus != OrderStatus.PACKED) {
				throw new RuntimeException("Order must be PACKED before SHIPPED");
			}
			break;

		case OUT_FOR_DELIVERY:
			if (currentStatus != OrderStatus.SHIPPED) {
				throw new RuntimeException("Order must be SHIPPED before OUT_FOR_DELIVERY");
			}
			break;

		case DELIVERED:

			if (currentStatus != OrderStatus.OUT_FOR_DELIVERY) {
				throw new RuntimeException("Order must be OUT_FOR_DELIVERY before DELIVERED");
			}

			// COD payment collected at delivery
			if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
				order.setPaymentstatus(PaymentStatus.PAID);
			}

			break;

		default:
			break;
		}

		order.setOrderstatus(status);
		order.setUpdatedAt(LocalDateTime.now());

		Order savedOrder = orderRepository.save(order);

		// ==============================
		// DELIVERED → Kafka
		// ==============================

		if (status == OrderStatus.DELIVERED) {

			// COD payment collected at delivery
			if ("COD".equalsIgnoreCase(savedOrder.getPaymentMethod())) {
				savedOrder.setPaymentstatus(PaymentStatus.PAID);
				savedOrder = orderRepository.save(savedOrder);
			}

			// =========================
			// 1. Notification Event
			// =========================

			OrderDeliveredEvent event = new OrderDeliveredEvent();

			event.setOrderId(savedOrder.getOrderid());
			event.setOrderNumber(savedOrder.getOrderNumber());
			event.setUserId(savedOrder.getUserid());
			event.setAmount(savedOrder.getTotalAmount());
			event.setPaymentMethod(savedOrder.getPaymentMethod());
			event.setPaymentStatus(savedOrder.getPaymentstatus());

			orderEventProducer.publishOrderDelivered(event);

			// =========================
			// 2. Inventory Commit Event
			// =========================

			List<InventoryCommitItem> items = savedOrder.getOrderItems().stream()
					.map(item -> new InventoryCommitItem(item.getSkuCode(), item.getQuantity())).toList();

			InventoryCommitEvent commitEvent = new InventoryCommitEvent(savedOrder.getOrderid(),
					savedOrder.getOrderNumber(), savedOrder.getUserid(), items);

			orderEventProducer.publishInventoryCommit(commitEvent);
		}

		// ==============================
		// Prepare response
		// ==============================

		orderResponseDto response = new orderResponseDto();

		response.setUserId(savedOrder.getUserid());
		response.setOrderId(savedOrder.getOrderid());
		response.setOrderNumber(savedOrder.getOrderNumber());
		response.setTotalAmount(savedOrder.getTotalAmount());
		response.setOrderStatus(savedOrder.getOrderstatus());
		response.setOrderDate(savedOrder.getCreateAt());
		response.setPaymentMethod(savedOrder.getPaymentMethod());
		response.setPaymentStatus(savedOrder.getPaymentstatus());

		// Order items

		List<OrderItemResponseDto> items = savedOrder.getOrderItems().stream().map(item -> {

			OrderItemResponseDto dto = new OrderItemResponseDto();

			dto.setProductId(item.getProductId());
			dto.setProductName(item.getProductName());
			dto.setQuantity(item.getQuantity());
			dto.setPrice(item.getPrice());
			dto.setSkuCode(item.getSkuCode());

			return dto;

		}).toList();

		response.setItems(items);

		response.setMsg("Order status updated successfully");

		return response;
	}

	public orderResponseDto getOrderForInventory(Long orderId) {

		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

		orderResponseDto response = new orderResponseDto();

		response.setOrderId(order.getOrderid());
		response.setUserId(order.getUserid());
		response.setOrderNumber(order.getOrderNumber());

		List<OrderItemResponseDto> items = order.getOrderItems().stream().map(item -> {

			OrderItemResponseDto dto = new OrderItemResponseDto();

			dto.setProductId(item.getProductId());
			dto.setProductName(item.getProductName());
			dto.setQuantity(item.getQuantity());
			dto.setPrice(item.getPrice());
			dto.setSkuCode(item.getSkuCode());

			return dto;
		}).toList();

		response.setItems(items);

		return response;
	}

	// additonal features....

//	@Override
//	public orderResponseDto orderHistory(Long id) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public orderResponseDto reduceInventory(Long id) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public orderResponseDto clearCart(Long id) {
//		// TODO Auto-generated method stub
//		return null;
//	}

}
