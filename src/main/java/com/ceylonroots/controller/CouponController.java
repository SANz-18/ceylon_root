package com.ceylonroots.controller;

import com.ceylonroots.dto.CouponRequest;
import com.ceylonroots.dto.CouponValidateRequest;
import com.ceylonroots.model.Coupon;
import com.ceylonroots.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ---- admin management (restricted via SecurityConfig URL rules) ----
    @GetMapping
    public List<Coupon> all() {
        return couponService.findAll();
    }

    @PostMapping
    public Coupon create(@Valid @RequestBody CouponRequest req) {
        return couponService.create(req);
    }

    @PutMapping("/{id}")
    public Coupon update(@PathVariable Long id, @Valid @RequestBody CouponRequest req) {
        return couponService.update(id, req);
    }

    @PutMapping("/{id}/toggle-active")
    public Coupon toggleActive(@PathVariable Long id) {
        return couponService.toggleActive(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        couponService.delete(id);
    }

    // ---- buyer-facing: check a code against the current cart before checkout ----
    @PostMapping("/validate")
    public Map<String, Object> validate(@Valid @RequestBody CouponValidateRequest req) {
        CouponService.DiscountResult result = couponService.preview(req.getCode(), req.getSubtotal(), req.getCurrency());
        return Map.of(
                "code", result.coupon().getCode(),
                "description", result.coupon().getDescription() == null ? "" : result.coupon().getDescription(),
                "discountAmount", result.discountAmount()
        );
    }


}
