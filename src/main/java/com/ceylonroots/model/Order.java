package com.ceylonroots.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderCode;      // e.g. ORD-A1B2C3

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Enumerated(EnumType.STRING)
    private DestinationType destinationType;

    private String country;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    private Double subtotal;
    private Double shipping;
    private Double total;

    private String couponCode;   // null if no coupon applied
    private Double discountAmount; // amount subtracted from subtotal, null/0 if none

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, unique = true)
    private String trackingCode;   // e.g. CYN-X9Z8Y7

    // ---- embedded payment info (kept simple: one payment per order) ----
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private LocalDateTime paidAt;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("updatedAt ASC")
    private List<OrderStatusHistory> history = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private String assignedStaffName;
}
