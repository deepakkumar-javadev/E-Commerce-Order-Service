package com.deepak.orderService.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.deepak.orderService.Dto.cartResponseDto;
import com.deepak.orderService.config.FeignConfig;

@FeignClient(name = "ECOM-CART-SERVICE", url = "http://localhost:8085",configuration = FeignConfig.class)
public interface CartClient {

	@GetMapping("/cart/getcart/{userId}")
	public cartResponseDto getCartByUserId(@PathVariable Long userId);

	@DeleteMapping("cart/removecart/cart/{userId}/items/{productId}")
	public ResponseEntity<String> RemoveItemFromCart(@PathVariable Long userId, @PathVariable Long productId);

	// ONLINE/ internal call
	@DeleteMapping("cart/clear/{userId}")
	public void clearCart(@PathVariable Long userId , @RequestHeader("X-Internal-Token") String internalToken);
}
