package com.ceylonroots.service;

import com.ceylonroots.dto.FeedbackRequest;
import com.ceylonroots.exception.ApiException;
import com.ceylonroots.model.*;
import com.ceylonroots.repository.FeedbackRepository;
import com.ceylonroots.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final OrderRepository orderRepository;
    private final SentimentService sentimentService;
    private final NotificationService notificationService;

    public Feedback submit(User buyer, FeedbackRequest req) {
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new ApiException("Order not found.", HttpStatus.NOT_FOUND));

        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new ApiException("You can only review your own orders.", HttpStatus.FORBIDDEN);
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ApiException("You can only review delivered orders.", HttpStatus.BAD_REQUEST);
        }
        if (feedbackRepository.existsByOrder(order)) {
            throw new ApiException("You've already reviewed this order.", HttpStatus.CONFLICT);
        }

        SentimentService.SentimentResult result = sentimentService.analyze(req.getComment());

        Feedback feedback = Feedback.builder()
                .order(order)
                .buyer(buyer)
                .rating(req.getRating())
                .comment(req.getComment())
                .sentimentLabel(result.label())
                .sentimentScore(result.score())
                .matchedKeywords(String.join(",", result.matchedKeywords()))
                .createdAt(LocalDateTime.now())
                .build();

        Feedback saved = feedbackRepository.save(feedback);

        String flag = result.label() == SentimentLabel.NEGATIVE ? " (negative — may need attention)" : "";
        notificationService.notifyRole(Role.ADMIN, "NEW_FEEDBACK", "New review",
                buyer.getName() + " left a " + req.getRating() + "\u2605 review on " + order.getOrderCode() + flag + ".",
                order.getOrderCode());

        return saved;
    }

    public List<Feedback> findForUser(User user) {
        if (user.getRole() == Role.BUYER) {
            return feedbackRepository.findByBuyerOrderByCreatedAtDesc(user);
        }
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    public Feedback reply(Long feedbackId, String reply) {
        Feedback f = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ApiException("Feedback not found.", HttpStatus.NOT_FOUND));
        f.setAdminReply(reply);
        Feedback saved = feedbackRepository.save(f);

        notificationService.notify(saved.getBuyer(), "FEEDBACK_REPLY", "Ceylon Roots replied to your review",
                "You have a reply on your review for order " + saved.getOrder().getOrderCode() + ".",
                saved.getOrder().getOrderCode());

        return saved;
    }
}
