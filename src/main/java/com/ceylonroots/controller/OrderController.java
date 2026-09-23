package com.ceylonroots.controller;

import com.ceylonroots.dto.CheckoutRequest;
import com.ceylonroots.model.Order;
import com.ceylonroots.model.User;
import com.ceylonroots.service.CurrentUserProvider;
import com.ceylonroots.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/checkout")
    public Order checkout(@Valid @RequestBody CheckoutRequest req) {
        User buyer = currentUserProvider.get();
        return orderService.checkout(buyer, req);
    }

    @GetMapping
    public List<Order> myOrders() {
        User user = currentUserProvider.get();
        return orderService.findForUser(user);
    }

    @GetMapping("/{id}")
    public Order one(@PathVariable Long id) {
        User requester = currentUserProvider.get();
        Order order = orderService.findById(id);
        if (requester.getRole().name().equals("BUYER") && !order.getBuyer().getId().equals(requester.getId())) {
            throw new com.ceylonroots.exception.ApiException("You can only view your own orders.", org.springframework.http.HttpStatus.FORBIDDEN);
        }
        return order;
    }

    /** Public tracking lookup — no auth required, matches the "guest tracking" flow. */
    @GetMapping("/track/{code}")
    public Order track(@PathVariable String code) {
        return orderService.findByTrackingCode(code);
    }

    @GetMapping("/logistics")
    public List<Order> logisticsQueue() {
        return orderService.findLogisticsQueue();
    }

    @PostMapping("/{id}/advance")
    public Order advance(@PathVariable Long id) {
        User actor = currentUserProvider.get();
        return orderService.advance(id, actor);
    }

    @PostMapping("/{id}/cancel")
    public Order cancel(@PathVariable Long id) {
        User actor = currentUserProvider.get();
        return orderService.cancel(id, actor);
    }
}
