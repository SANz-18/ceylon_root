package com.ceylonroots.service;

import com.ceylonroots.exception.ApiException;
import com.ceylonroots.model.ChatMessage;
import com.ceylonroots.model.Role;
import com.ceylonroots.model.User;
import com.ceylonroots.repository.ChatMessageRepository;
import com.ceylonroots.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /** Buyer viewing their own thread — also marks staff replies as read. */
    @Transactional
    public List<ChatMessage> getThreadAsBuyer(User buyer) {
        markRead(buyer, Role.BUYER); // mark everything NOT sent by the buyer as read
        return chatMessageRepository.findByBuyerOrderByCreatedAtAsc(buyer);
    }

    /** Staff/admin opening a specific buyer's thread — marks the buyer's messages as read. */
    @Transactional
    public List<ChatMessage> getThreadAsStaff(Long buyerId) {
        User buyer = resolveBuyer(buyerId);
        List<ChatMessage> unread = chatMessageRepository.findByBuyerAndSender_RoleAndReadFalse(buyer, Role.BUYER);
        unread.forEach(m -> m.setRead(true));
        chatMessageRepository.saveAll(unread);
        return chatMessageRepository.findByBuyerOrderByCreatedAtAsc(buyer);
    }

    @Transactional
    public ChatMessage sendAsBuyer(User buyer, String content) {
        ChatMessage saved = save(buyer, buyer, content);
        notificationService.notifyRole(Role.STAFF, "NEW_CHAT_MESSAGE", "New chat message",
                buyer.getName() + " sent a message in live chat.", null);
        notificationService.notifyRole(Role.ADMIN, "NEW_CHAT_MESSAGE", "New chat message",
                buyer.getName() + " sent a message in live chat.", null);
        return saved;
    }

    @Transactional
    public ChatMessage sendAsStaff(Long buyerId, User staff, String content) {
        User buyer = resolveBuyer(buyerId);
        ChatMessage saved = save(buyer, staff, content);
        notificationService.notify(buyer, "NEW_CHAT_MESSAGE", "New message from support",
                staff.getName() + " replied in your live chat.", null);
        return saved;
    }

    private ChatMessage save(User buyer, User sender, String content) {
        if (content == null || content.isBlank()) {
            throw new ApiException("Message can't be empty.", HttpStatus.BAD_REQUEST);
        }
        ChatMessage message = ChatMessage.builder()
                .buyer(buyer).sender(sender).content(content.trim())
                .read(false).createdAt(LocalDateTime.now())
                .build();
        return chatMessageRepository.save(message);
    }

    /** Marks every message in the buyer's thread NOT sent by excludeSenderRole as read (i.e. the "incoming" side). */
    private void markRead(User buyer, Role excludeSenderRole) {
        List<ChatMessage> unread = chatMessageRepository.findByBuyerAndSender_RoleNotAndReadFalse(buyer, excludeSenderRole);
        unread.forEach(m -> m.setRead(true));
        chatMessageRepository.saveAll(unread);
    }

    /** Staff/admin inbox: one row per buyer who has an active conversation, newest activity first. */
    public List<Map<String, Object>> listThreadsForStaff() {
        List<ChatMessage> all = chatMessageRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, List<ChatMessage>> byBuyer = all.stream()
                .collect(Collectors.groupingBy(m -> m.getBuyer().getId(), LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> threads = new ArrayList<>();
        for (List<ChatMessage> msgs : byBuyer.values()) {
            ChatMessage latest = msgs.get(0); // already newest-first from the source query
            long unread = msgs.stream().filter(m -> m.getSender().getRole() == Role.BUYER && !m.isRead()).count();

            Map<String, Object> thread = new LinkedHashMap<>();
            thread.put("buyer", latest.getBuyer());
            thread.put("lastMessage", latest.getContent());
            thread.put("lastMessageAt", latest.getCreatedAt());
            thread.put("lastSenderRole", latest.getSender().getRole());
            thread.put("unreadCount", unread);
            threads.add(thread);
        }
        threads.sort((a, b) -> ((LocalDateTime) b.get("lastMessageAt")).compareTo((LocalDateTime) a.get("lastMessageAt")));
        return threads;
    }

    public long unreadCountForBuyer(User buyer) {
        return chatMessageRepository.countByBuyerAndSender_RoleAndReadFalse(buyer, Role.STAFF)
                + chatMessageRepository.countByBuyerAndSender_RoleAndReadFalse(buyer, Role.ADMIN);
    }

    private User resolveBuyer(Long buyerId) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ApiException("Buyer not found.", HttpStatus.NOT_FOUND));
        if (buyer.getRole() != Role.BUYER) {
            throw new ApiException("This user is not a buyer.", HttpStatus.BAD_REQUEST);
        }
        return buyer;
    }
}
