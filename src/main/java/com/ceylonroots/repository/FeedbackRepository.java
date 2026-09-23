package com.ceylonroots.repository;

import com.ceylonroots.model.Feedback;
import com.ceylonroots.model.Order;
import com.ceylonroots.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByBuyerOrderByCreatedAtDesc(User buyer);
    List<Feedback> findAllByOrderByCreatedAtDesc();
    boolean existsByOrder(Order order);
}
