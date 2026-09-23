package com.ceylonroots.repository;

import com.ceylonroots.model.Order;
import com.ceylonroots.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerOrderByCreatedAtDesc(User buyer);
    List<Order> findAllByOrderByCreatedAtDesc();
    Optional<Order> findByTrackingCodeIgnoreCase(String trackingCode);
    Optional<Order> findByOrderCode(String orderCode);
}
