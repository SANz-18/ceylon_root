package com.ceylonroots.service;

import com.ceylonroots.exception.ApiException;
import com.ceylonroots.model.*;
import com.ceylonroots.repository.RefundRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final NotificationService notificationService;

    private static final int MAX_IMAGES = 3;
    private static final long MAX_IMAGE_BYTES = 3L * 1024 * 1024; // 3MB per image
    private static final List<String> ALLOWED_TYPES = List.of("image/png", "image/jpeg", "image/webp");

    @Transactional
    public RefundRequest submit(User buyer, Order order, String reason, List<MultipartFile> images) {
        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new ApiException("You can only request a refund for your own orders.", HttpStatus.FORBIDDEN);
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ApiException("Refunds can only be requested for delivered orders.", HttpStatus.BAD_REQUEST);
        }
        if (refundRequestRepository.existsByOrder(order)) {
            throw new ApiException("A refund request already exists for this order.", HttpStatus.CONFLICT);
        }
        if (reason == null || reason.isBlank()) {
            throw new ApiException("Please describe the reason for the refund.", HttpStatus.BAD_REQUEST);
        }
        if (images != null && images.size() > MAX_IMAGES) {
            throw new ApiException("You can attach at most " + MAX_IMAGES + " images.", HttpStatus.BAD_REQUEST);
        }

        RefundRequest request = RefundRequest.builder()
                .order(order).buyer(buyer).reason(reason.trim())
                .status(RefundStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        if (images != null) {
            for (MultipartFile file : images) {
                if (file.isEmpty()) continue;
                if (file.getSize() > MAX_IMAGE_BYTES) {
                    throw new ApiException("Each image must be under 3MB.", HttpStatus.BAD_REQUEST);
                }
                String contentType = file.getContentType();
                if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
                    throw new ApiException("Only PNG, JPEG or WEBP images are allowed.", HttpStatus.BAD_REQUEST);
                }
                try {
                    String base64 = Base64.getEncoder().encodeToString(file.getBytes());
                    request.getImages().add(RefundImage.builder()
                            .refundRequest(request)
                            .contentType(contentType)
                            .imageBase64(base64)
                            .build());
                } catch (IOException e) {
                    throw new ApiException("Could not read uploaded image.", HttpStatus.BAD_REQUEST);
                }
            }
        }

        RefundRequest saved = refundRequestRepository.save(request);

        notificationService.notifyRole(Role.ADMIN, "NEW_REFUND", "New refund request",
                buyer.getName() + " requested a refund on " + order.getOrderCode() + ".", order.getOrderCode());
        notificationService.notifyRole(Role.STAFF, "NEW_REFUND", "New refund request",
                buyer.getName() + " requested a refund on " + order.getOrderCode() + ".", order.getOrderCode());

        return saved;
    }

    public List<RefundRequest> findForUser(User user) {
        if (user.getRole() == Role.BUYER) {
            return refundRequestRepository.findByBuyerOrderByCreatedAtDesc(user);
        }
        return refundRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public RefundRequest approve(Long id, User actor, String note) {
        RefundRequest request = get(id);
        assertPending(request);

        request.setStatus(RefundStatus.APPROVED);
        request.setStaffNote(note);
        request.setReviewedBy(actor.getName());
        request.setReviewedAt(LocalDateTime.now());

        request.getOrder().setPaymentStatus(PaymentStatus.REFUNDED);

        RefundRequest saved = refundRequestRepository.save(request);

        notificationService.notify(saved.getBuyer(), "REFUND_APPROVED", "Refund approved",
                "Your refund request for " + saved.getOrder().getOrderCode() + " has been approved."
                        + (note != null && !note.isBlank() ? " Note: " + note : ""),
                saved.getOrder().getOrderCode());

        return saved;
    }

    @Transactional
    public RefundRequest reject(Long id, User actor, String note) {
        RefundRequest request = get(id);
        assertPending(request);

        request.setStatus(RefundStatus.REJECTED);
        request.setStaffNote(note);
        request.setReviewedBy(actor.getName());
        request.setReviewedAt(LocalDateTime.now());

        RefundRequest saved = refundRequestRepository.save(request);

        notificationService.notify(saved.getBuyer(), "REFUND_REJECTED", "Refund request declined",
                "Your refund request for " + saved.getOrder().getOrderCode() + " was not approved."
                        + (note != null && !note.isBlank() ? " Reason: " + note : ""),
                saved.getOrder().getOrderCode());

        return saved;
    }

    private void assertPending(RefundRequest request) {
        if (request.getStatus() != RefundStatus.PENDING) {
            throw new ApiException("This request has already been reviewed.", HttpStatus.BAD_REQUEST);
        }
    }

    private RefundRequest get(Long id) {
        return refundRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException("Refund request not found.", HttpStatus.NOT_FOUND));
    }
}
