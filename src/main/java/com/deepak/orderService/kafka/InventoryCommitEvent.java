package com.deepak.orderService.kafka;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCommitEvent {

	private Long orderId;
	private String orderNumber;
	private Long userId;
	private List<InventoryCommitItem> items;
}
