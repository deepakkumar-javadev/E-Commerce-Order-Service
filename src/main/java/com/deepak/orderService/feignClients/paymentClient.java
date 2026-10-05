package com.deepak.orderService.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.deepak.orderService.Dto.PaymentRequestDto;
import com.deepak.orderService.Dto.PaymentResponseDto;
import com.deepak.orderService.config.FeignConfig;

@FeignClient(name = "ECOM-PAYMENT-SERVICE" ,url= "http://localhost:8087",configuration = FeignConfig.class)
public interface paymentClient {

	@PostMapping("/payments/createOrder")
	public PaymentResponseDto createPaymentOrder( @RequestBody PaymentRequestDto request);
	
	@GetMapping("payments/get/{orderId}")
	public PaymentResponseDto getPayment(@PathVariable Long orderId);
	
}
