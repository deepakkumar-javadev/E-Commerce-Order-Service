package com.deepak.orderService.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.deepak.orderService.entity.OrderItem;

public interface OrderItemRepository   extends JpaRepository<OrderItem,Long>{

	  List<OrderItem> findByOrderOrderid(Long orderid);
}
