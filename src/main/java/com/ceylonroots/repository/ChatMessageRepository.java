package com.ceylonroots.repository;

import com.ceylonroots.model.ChatMessage;
import com.ceylonroots.model.Role;
import com.ceylonroots.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByBuyerOrderByCreatedAtAsc(User buyer);
    List<ChatMessage> findAllByOrderByCreatedAtDesc();

    // "thread" unread counters — who sent it vs. who's reading it
    List<ChatMessage> findByBuyerAndSender_RoleNotAndReadFalse(User buyer, Role senderRole);
    List<ChatMessage> findByBuyerAndSender_RoleAndReadFalse(User buyer, Role senderRole);
    long countByBuyerAndSender_RoleAndReadFalse(User buyer, Role senderRole);
}
