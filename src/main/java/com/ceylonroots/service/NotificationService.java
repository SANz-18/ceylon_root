package com.ceylonroots.service;

import com.ceylonroots.exception.ApiException;
import com.ceylonroots.model.Notification;
import com.ceylonroots.model.Role;
import com.ceylonroots.model.User;
import com.ceylonroots.repository.NotificationRepository;
import com.ceylonroots.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private static final int MAX_RETURNED = 30;

    /** Notify a single recipient. */
    @Transactional
    public void notify(User recipient, String type, String title, String message, String orderCode) {
        notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .orderCode(orderCode)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /** Notify every active user with the given role — e.g. all logistics staff when a new order lands. */
    @Transactional
    public void notifyRole(Role role, String type, String title, String message, String orderCode) {
        List<User> recipients = userRepository.findAll().stream()
                .filter(u -> u.getRole() == role && u.isActive())
                .toList();
        for (User u : recipients) {
            notify(u, type, title, message, orderCode);
        }
    }

    public List<Notification> listForUser(User user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user, PageRequest.of(0, MAX_RETURNED));
    }

    public long unreadCount(User user) {
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    @Transactional
    public void markRead(Long id, User user) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ApiException("Notification not found.", HttpStatus.NOT_FOUND));
        if (!n.getRecipient().getId().equals(user.getId())) {
            throw new ApiException("You can only manage your own notifications.", HttpStatus.FORBIDDEN);
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(User user) {
        List<Notification> unread = notificationRepository.findByRecipientAndReadFalse(user);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
