package com.ceylonroots.controller;

import com.ceylonroots.model.Notification;
import com.ceylonroots.model.User;
import com.ceylonroots.service.CurrentUserProvider;
import com.ceylonroots.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public List<Notification> list() {
        return notificationService.listForUser(currentUserProvider.get());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notificationService.unreadCount(currentUserProvider.get()));
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        notificationService.markRead(id, currentUserProvider.get());
    }

    @PostMapping("/read-all")
    public void markAllRead() {
        notificationService.markAllRead(currentUserProvider.get());
    }
}
