package com.ceylonroots.repository;

import com.ceylonroots.model.Order;
import com.ceylonroots.model.RefundRequest;
import com.ceylonroots.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    List<RefundRequest> findByBuyerOrderByCreatedAtDesc(User buyer);
    List<RefundRequest> findAllByOrderByCreatedAtDesc();
    boolean existsByOrder(Order order);
}
