package com.ceylonroots.dto;

import com.ceylonroots.model.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class CouponValidateRequest {
    @NotBlank
    private String code;

    @NotNull @PositiveOrZero
    private Double subtotal;

    @NotNull
    private Currency currency;
}
