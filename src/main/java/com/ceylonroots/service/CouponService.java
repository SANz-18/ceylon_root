package com.ceylonroots.service;

import com.ceylonroots.dto.CouponRequest;
import com.ceylonroots.exception.ApiException;
import com.ceylonroots.model.Coupon;
import com.ceylonroots.model.Currency;
import com.ceylonroots.model.DiscountType;
import com.ceylonroots.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public record DiscountResult(Coupon coupon, double discountAmount) {}

    // ==== admin CRUD =========================================================
    public List<Coupon> findAll() {
        return couponRepository.findAllByOrderByCreatedAtDesc();
    }

    public Coupon create(CouponRequest req) {
        String code = req.getCode().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException("A coupon with that code already exists.", HttpStatus.CONFLICT);
        }
        validateShape(req);

        Coupon coupon = Coupon.builder()
                .code(code)
                .description(req.getDescription())
                .discountType(req.getDiscountType())
                .percentage(req.getPercentage())
                .fixedUsd(req.getFixedUsd())
                .fixedLkr(req.getFixedLkr())
                .minOrderUsd(req.getMinOrderUsd())
                .minOrderLkr(req.getMinOrderLkr())
                .maxUses(req.getMaxUses())
                .usedCount(0)
                .active(true)
                .expiresAt(req.getExpiresAt())
                .createdAt(LocalDateTime.now())
                .build();
        return couponRepository.save(coupon);
    }

    public Coupon update(Long id, CouponRequest req) {
        Coupon coupon = get(id);
        String code = req.getCode().trim().toUpperCase();
        if (!code.equals(coupon.getCode()) && couponRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException("A coupon with that code already exists.", HttpStatus.CONFLICT);
        }
        validateShape(req);

        coupon.setCode(code);
        coupon.setDescription(req.getDescription());
        coupon.setDiscountType(req.getDiscountType());
        coupon.setPercentage(req.getPercentage());
        coupon.setFixedUsd(req.getFixedUsd());
        coupon.setFixedLkr(req.getFixedLkr());
        coupon.setMinOrderUsd(req.getMinOrderUsd());
        coupon.setMinOrderLkr(req.getMinOrderLkr());
        coupon.setMaxUses(req.getMaxUses());
        coupon.setExpiresAt(req.getExpiresAt());
        return couponRepository.save(coupon);
    }

    public Coupon toggleActive(Long id) {
        Coupon coupon = get(id);
        coupon.setActive(!coupon.isActive());
        return couponRepository.save(coupon);
    }

    public void delete(Long id) {
        couponRepository.delete(get(id));
    }

    private void validateShape(CouponRequest req) {
        if (req.getDiscountType() == DiscountType.PERCENTAGE) {
            if (req.getPercentage() == null || req.getPercentage() <= 0 || req.getPercentage() > 100) {
                throw new ApiException("Percentage must be between 1 and 100.", HttpStatus.BAD_REQUEST);
            }
        } else {
            boolean noAmounts = (req.getFixedUsd() == null || req.getFixedUsd() <= 0)
                    && (req.getFixedLkr() == null || req.getFixedLkr() <= 0);
            if (noAmounts) {
                throw new ApiException("Provide a fixed discount amount in USD and/or LKR.", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private Coupon get(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ApiException("Coupon not found.", HttpStatus.NOT_FOUND));
    }

    // ==== validate / apply ===================================================

    /** Read-only check used by the cart preview — does NOT consume a usage slot. */
    public DiscountResult preview(String code, double subtotal, Currency currency) {
        Coupon coupon = checkValid(code, subtotal, currency);
        return new DiscountResult(coupon, computeDiscount(coupon, subtotal, currency));
    }

    /** Re-validates and consumes a usage slot — called during actual checkout, inside the order transaction. */
    @Transactional
    public DiscountResult applyAndConsume(String code, double subtotal, Currency currency) {
        Coupon coupon = checkValid(code, subtotal, currency);
        double discount = computeDiscount(coupon, subtotal, currency);
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
        return new DiscountResult(coupon, discount);
    }

    private Coupon checkValid(String code, double subtotal, Currency currency) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ApiException("Invalid coupon code.", HttpStatus.NOT_FOUND));

        if (!coupon.isActive()) {
            throw new ApiException("This coupon is no longer active.", HttpStatus.BAD_REQUEST);
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException("This coupon has expired.", HttpStatus.BAD_REQUEST);
        }
        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new ApiException("This coupon has reached its usage limit.", HttpStatus.BAD_REQUEST);
        }
        Double minOrder = currency == Currency.USD ? coupon.getMinOrderUsd() : coupon.getMinOrderLkr();
        if (minOrder != null && subtotal < minOrder) {
            throw new ApiException("This coupon requires a minimum order of " + formatMoney(minOrder, currency) + ".", HttpStatus.BAD_REQUEST);
        }
        return coupon;
    }

    private double computeDiscount(Coupon coupon, double subtotal, Currency currency) {
        double discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotal * (coupon.getPercentage() / 100.0);
        } else {
            Double fixed = currency == Currency.USD ? coupon.getFixedUsd() : coupon.getFixedLkr();
            if (fixed == null) {
                throw new ApiException("This coupon isn't available in " + currency + ".", HttpStatus.BAD_REQUEST);
            }
            discount = fixed;
        }
        return Math.min(discount, subtotal); // never discount below zero
    }

    private String formatMoney(double v, Currency currency) {
        return currency == Currency.USD ? String.format("$%,.2f", v) : String.format("Rs. %,.2f", v);
    }


}
