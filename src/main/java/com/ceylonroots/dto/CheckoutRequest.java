package com.ceylonroots.dto;

import com.ceylonroots.model.Currency;
import com.ceylonroots.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {

    @NotEmpty
    @Valid
    private List<CartLine> items;

    @NotNull
    private Currency currency; // USD => international order, LKR => local order

    @NotNull
    private PaymentMethod paymentMethod;

    private String couponCode; // optional

    /** Only required when paymentMethod == CARD. Validated server-side (Luhn, expiry, CVV). */
    private CardDetails card;

    @Data
    public static class CartLine {
        @NotNull
        private Long productId;

        @NotNull @Positive
        private Integer qty;
    }

    @Data
    public static class CardDetails {
        private String number;
        private String holderName;
        private String expiry; // MM/YY
        private String cvv;
    }
}
