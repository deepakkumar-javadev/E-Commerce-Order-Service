package com.deepak.orderService.feignClients;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.deepak.orderService.Dto.InventoryResDto;
import com.deepak.orderService.config.FeignConfig;

@FeignClient(name = "ECOM-INVENTORY-SERVICE" ,url= "http://localhost:8084",configuration = FeignConfig.class)
public interface InventoryClient {

	// get inventories........
	
	@PostMapping("/stock/getInventories")
	public List<InventoryResDto> checkStock(@RequestBody List<String> skucodes);
	
	// reduce stocks
	
	@PutMapping("/stock/reducestock/{skuCode}")
	public void reduceStock(@PathVariable String skuCode, @RequestParam Integer quantity);
	
}
