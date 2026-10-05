package com.deepak.orderService.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.deepak.orderService.entity.Order;
import com.deepak.orderService.entity.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long>{

	List<Order> findByUserid(Long userid);

	 Optional<Order> findById(Long orderid);
	    
	
    Optional<Order> findByOrderNumber(String orderNumber);
    
    Optional<Order> findFirstByUseridAndOrderstatusAndCreateAtAfterOrderByCreateAtDesc(
            Long userid, OrderStatus status, LocalDateTime after);
	
    
    // get list of orders
    List<Order> findByUseridOrderByCreateAtDesc(Long userid);
}
