package com.ceylonroots.controller;

import com.ceylonroots.dto.RefundDecisionRequest;
import com.ceylonroots.model.Order;
import com.ceylonroots.model.RefundRequest;
import com.ceylonroots.model.User;
import com.ceylonroots.service.CurrentUserProvider;
import com.ceylonroots.service.OrderService;
import com.ceylonroots.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final OrderService orderService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('BUYER')")
    public RefundRequest submit(@RequestParam Long orderId,
                                @RequestParam String reason,
                                @RequestParam(required = false) List<MultipartFile> images) {
        User buyer = currentUserProvider.get();
        Order order = orderService.findById(orderId);
        return refundService.submit(buyer, order, reason, images);
    }

    @GetMapping
    public List<RefundRequest> list() {
        return refundService.findForUser(currentUserProvider.get());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public RefundRequest approve(@PathVariable Long id, @RequestBody(required = false) RefundDecisionRequest req) {
        String note = req == null ? null : req.getNote();
        return refundService.approve(id, currentUserProvider.get(), note);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public RefundRequest reject(@PathVariable Long id, @RequestBody(required = false) RefundDecisionRequest req) {
        String note = req == null ? null : req.getNote();
        return refundService.reject(id, currentUserProvider.get(), note);
    }
}
