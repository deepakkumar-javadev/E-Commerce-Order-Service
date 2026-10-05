package com.deepak.orderService.Dto;

import lombok.Data;

@Data
public class InventoryResDto {
	private Long id;
	private String skuCode;
	private int stockQuantity;
	private String availablityStatus;
}
