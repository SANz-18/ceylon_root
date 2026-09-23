package com.ceylonroots.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code; // stored uppercase, e.g. "WELCOME10"

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    private Double percentage;   // used when discountType == PERCENTAGE, e.g. 10 = 10%
    private Double fixedUsd;     // used when discountType == FIXED
    private Double fixedLkr;     // used when discountType == FIXED

    private Double minOrderUsd;  // optional minimum subtotal to qualify (null = no minimum)
    private Double minOrderLkr;

    private Integer maxUses;     // optional usage cap (null = unlimited)

    @Builder.Default
    @Column(nullable = false)
    private Integer usedCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime expiresAt; // null = never expires

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
