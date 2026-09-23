package com.ceylonroots.service;

import com.ceylonroots.dto.CheckoutRequest;
import com.ceylonroots.exception.ApiException;
import com.ceylonroots.model.*;
import com.ceylonroots.repository.OrderRepository;
import com.ceylonroots.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final CouponService couponService;

    private static final List<OrderStatus> STAGES = List.of(
            OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.PACKED,
            OrderStatus.SHIPPED, OrderStatus.IN_TRANSIT, OrderStatus.CUSTOMS, OrderStatus.DELIVERED
    );

    private static final Map<OrderStatus, String> STAGE_NOTES = Map.of(
            OrderStatus.PROCESSING, "Quills sorted, graded and weighed at the Matale facility.",
            OrderStatus.PACKED, "Sealed in moisture-proof export cartons.",
            OrderStatus.SHIPPED, "Handed to freight forwarder — container loaded.",
            OrderStatus.IN_TRANSIT, "In transit to destination port.",
            OrderStatus.CUSTOMS, "Cleared customs at destination.",
            OrderStatus.DELIVERED, "Delivered and signed for."
    );

    @Transactional
    public Order checkout(User buyer, CheckoutRequest req) {
        validatePayment(req);

        boolean international = req.getCurrency() == Currency.USD;
        double subtotal = 0;
        Order order = Order.builder()
                .orderCode(genCode("ORD"))
                .buyer(buyer)
                .destinationType(international ? DestinationType.INTERNATIONAL : DestinationType.LOCAL)
                .country(buyer.getCountry())
                .currency(req.getCurrency())
                .status(OrderStatus.CONFIRMED)
                .trackingCode(genCode("CYN"))
                .paymentMethod(req.getPaymentMethod())
                .paymentStatus(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .assignedStaffName("Nadeesha Perera")
                .build();

        for (CheckoutRequest.CartLine line : req.getItems()) {
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new ApiException("Product not found: " + line.getProductId(), HttpStatus.NOT_FOUND));

            if (product.getStockKg() < line.getQty()) {
                throw new ApiException("Not enough stock for " + product.getName() + ".", HttpStatus.BAD_REQUEST);
            }
            double priceEach = international ? product.getPriceUsd() : product.getPriceLkr();
            subtotal += priceEach * line.getQty();

            product.setStockKg(product.getStockKg() - line.getQty());
            productRepository.save(product);

            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .product(product)
                    .grade(product.getGrade())
                    .qty(line.getQty())
                    .priceEach(priceEach)
                    .build());
        }

        double shipping = international
                ? (req.getCurrency() == Currency.USD ? 85 : 25500)
                : (req.getCurrency() == Currency.USD ? 0 : 1200);

        double discount = 0;
        String appliedCode = null;
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            CouponService.DiscountResult result = couponService.applyAndConsume(req.getCouponCode(), subtotal, req.getCurrency());
            discount = result.discountAmount();
            appliedCode = result.coupon().getCode();
        }

        order.setSubtotal(subtotal);
        order.setShipping(shipping);
        order.setCouponCode(appliedCode);
        order.setDiscountAmount(discount);
        order.setTotal(subtotal - discount + shipping);

        order.getHistory().add(OrderStatusHistory.builder()
                .order(order).status(OrderStatus.CONFIRMED)
                .note("Order confirmed & payment received.")
                .updatedBy("system").updatedAt(LocalDateTime.now())
                .build());

        Order saved = orderRepository.save(order);

        notificationService.notify(buyer, "ORDER_CONFIRMED", "Order confirmed",
                "Your order " + saved.getOrderCode() + " has been confirmed and payment received.",
                saved.getOrderCode());

        int totalKg = saved.getItems().stream().mapToInt(OrderItem::getQty).sum();
        notificationService.notifyRole(Role.STAFF, "NEW_ORDER", "New order to process",
                buyer.getName() + " placed order " + saved.getOrderCode() + " (" + totalKg + "kg, " + saved.getCountry() + ").",
                saved.getOrderCode());
        notificationService.notifyRole(Role.ADMIN, "NEW_ORDER", "New order placed",
                buyer.getName() + " placed order " + saved.getOrderCode() + " — " + formatMoney(saved.getTotal(), saved.getCurrency()) + ".",
                saved.getOrderCode());

        return saved;
    }

    /** Simulated payment validation — never contacts a real card network. */
    private void validatePayment(CheckoutRequest req) {
        if (req.getPaymentMethod() != PaymentMethod.CARD) return;

        CheckoutRequest.CardDetails card = req.getCard();
        if (card == null) throw new ApiException("Card details are required.", HttpStatus.BAD_REQUEST);

        if (!luhnCheck(card.getNumber())) {
            throw new ApiException("Card number failed validation.", HttpStatus.BAD_REQUEST);
        }
        if (card.getHolderName() == null || card.getHolderName().isBlank()) {
            throw new ApiException("Cardholder name is required.", HttpStatus.BAD_REQUEST);
        }
        if (card.getExpiry() == null || !card.getExpiry().matches("\\d{2}/\\d{2}")) {
            throw new ApiException("Enter expiry as MM/YY.", HttpStatus.BAD_REQUEST);
        }
        String[] parts = card.getExpiry().split("/");
        int month = Integer.parseInt(parts[0]);
        int year = 2000 + Integer.parseInt(parts[1]);
        if (month < 1 || month > 12 || YearMonth.of(year, month).isBefore(YearMonth.now())) {
            throw new ApiException("Card has expired or month is invalid.", HttpStatus.BAD_REQUEST);
        }
        if (card.getCvv() == null || !card.getCvv().matches("\\d{3,4}")) {
            throw new ApiException("Enter a valid CVV.", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean luhnCheck(String number) {
        if (number == null) return false;
        String digits = number.replaceAll("\\D", "");
        if (digits.length() < 12) return false;
        int sum = 0;
        boolean alt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alt) { n *= 2; if (n > 9) n -= 9; }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    public List<Order> findForUser(User user) {
        return switch (user.getRole()) {
            case BUYER -> orderRepository.findByBuyerOrderByCreatedAtDesc(user);
            case ADMIN, STAFF -> orderRepository.findAllByOrderByCreatedAtDesc();
        };
    }

    public List<Order> findLogisticsQueue() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(o -> STAGES.contains(o.getStatus()) && o.getStatus() != OrderStatus.DELIVERED)
                .sorted((a, b) -> Integer.compare(STAGES.indexOf(a.getStatus()), STAGES.indexOf(b.getStatus())))
                .toList();
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ApiException("Order not found.", HttpStatus.NOT_FOUND));
    }

    public Order findByTrackingCode(String code) {
        return orderRepository.findByTrackingCodeIgnoreCase(code)
                .orElseThrow(() -> new ApiException("No shipment found for that tracking code.", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Order advance(Long orderId, User actor) {
        Order order = findById(orderId);
        int idx = STAGES.indexOf(order.getStatus());
        if (idx < 0 || idx >= STAGES.size() - 1) {
            throw new ApiException("Order cannot be advanced further.", HttpStatus.BAD_REQUEST);
        }
        OrderStatus next = STAGES.get(idx + 1);
        order.setStatus(next);
        order.getHistory().add(OrderStatusHistory.builder()
                .order(order).status(next)
                .note(STAGE_NOTES.getOrDefault(next, "Status updated."))
                .updatedBy(actor.getName()).updatedAt(LocalDateTime.now())
                .build());
        Order saved = orderRepository.save(order);

        notificationService.notify(saved.getBuyer(), "ORDER_STATUS", "Order " + prettyStatus(next),
                "Your order " + saved.getOrderCode() + " is now " + prettyStatus(next).toLowerCase() + ".",
                saved.getOrderCode());

        return saved;
    }

    @Transactional
    public Order cancel(Long orderId, User actor) {
        Order order = findById(orderId);
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new ApiException("This order can no longer be cancelled.", HttpStatus.BAD_REQUEST);
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.getHistory().add(OrderStatusHistory.builder()
                .order(order).status(OrderStatus.CANCELLED)
                .note("Order cancelled by staff.")
                .updatedBy(actor.getName()).updatedAt(LocalDateTime.now())
                .build());
        Order saved = orderRepository.save(order);

        notificationService.notify(saved.getBuyer(), "ORDER_CANCELLED", "Order cancelled",
                "Your order " + saved.getOrderCode() + " has been cancelled. Contact support if this is unexpected.",
                saved.getOrderCode());

        return saved;
    }

    private String genCode(String prefix) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(prefix).append("-");
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private String prettyStatus(OrderStatus status) {
        String[] parts = status.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) sb.append(p.substring(0, 1)).append(p.substring(1).toLowerCase()).append(" ");
        return sb.toString().trim();
    }

    private String formatMoney(double v, Currency currency) {
        return currency == Currency.USD ? String.format("$%,.2f", v) : String.format("Rs. %,.2f", v);
    }
}
