package com.ceylonroots.dto;

import com.ceylonroots.model.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponRequest {
    @NotBlank
    private String code;

    private String description;

    @NotNull
    private DiscountType discountType;

    private Double percentage;   // required if discountType == PERCENTAGE
    private Double fixedUsd;     // required if discountType == FIXED
    private Double fixedLkr;     // required if discountType == FIXED

    private Double minOrderUsd;
    private Double minOrderLkr;
    private Integer maxUses;
    private LocalDateTime expiresAt;
}
