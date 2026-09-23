package com.ceylonroots.controller;

import com.ceylonroots.dto.ChatMessageRequest;
import com.ceylonroots.model.ChatMessage;
import com.ceylonroots.model.User;
import com.ceylonroots.service.ChatService;
import com.ceylonroots.service.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserProvider currentUserProvider;

    // ---- buyer side: a single ongoing conversation with support ----

    @GetMapping("/my-messages")
    @PreAuthorize("hasRole('BUYER')")
    public List<ChatMessage> myMessages() {
        return chatService.getThreadAsBuyer(currentUserProvider.get());
    }

    @PostMapping("/my-messages")
    @PreAuthorize("hasRole('BUYER')")
    public ChatMessage sendMyMessage(@Valid @RequestBody ChatMessageRequest req) {
        return chatService.sendAsBuyer(currentUserProvider.get(), req.getContent());
    }

    @GetMapping("/my-unread-count")
    @PreAuthorize("hasRole('BUYER')")
    public Map<String, Long> myUnreadCount() {
        return Map.of("count", chatService.unreadCountForBuyer(currentUserProvider.get()));
    }

    // ---- staff/admin side: shared inbox across all buyers ----

    @GetMapping("/threads")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<Map<String, Object>> threads() {
        return chatService.listThreadsForStaff();
    }

    @GetMapping("/threads/{buyerId}/messages")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<ChatMessage> threadMessages(@PathVariable Long buyerId) {
        return chatService.getThreadAsStaff(buyerId);
    }

    @PostMapping("/threads/{buyerId}/messages")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ChatMessage sendThreadMessage(@PathVariable Long buyerId, @Valid @RequestBody ChatMessageRequest req) {
        return chatService.sendAsStaff(buyerId, currentUserProvider.get(), req.getContent());
    }
}
