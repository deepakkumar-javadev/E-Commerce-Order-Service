package com.deepak.orderService.Dto;

import java.util.List;

import lombok.Data;


@Data
public class cartResponseDto {

	private Long cartId;
	private Long userId;
	private List<CartItemResponseDto> items;
}
