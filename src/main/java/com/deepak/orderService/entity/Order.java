package com.deepak.orderService.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "Orders", indexes = {
		@Index(name = "idx_order_user_id", columnList = "userid"),
		@Index(name = "idx_order_order_number", columnList = "ordernumber"),
		@Index(name = "idx_order_payment_status", columnList = "paymentstatus"),
		@Index(name = "idx_order_created_at", columnList = "createat")

})
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long orderid;

	private String orderNumber;

	private Long userid;

	private Double totalAmount;

	private String paymentMethod;

	@Enumerated(EnumType.STRING)
	private OrderStatus orderstatus;

	@Enumerated(EnumType.STRING)
	private PaymentStatus paymentstatus;

	private LocalDateTime createAt;

	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
	private List<OrderItem> orderItems = new ArrayList<>();

	@PrePersist // run before new record added
	protected void onCreate() {
		createAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate // run before updating existing record
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
