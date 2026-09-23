package com.ceylonroots.repository;

import com.ceylonroots.model.Notification;
import com.ceylonroots.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);
    long countByRecipientAndReadFalse(User recipient);
    List<Notification> findByRecipientAndReadFalse(User recipient);
}
