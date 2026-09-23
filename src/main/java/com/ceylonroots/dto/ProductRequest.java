package com.ceylonroots.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ProductRequest {
    @NotBlank
    private String grade;

    @NotBlank
    private String name;

    private String description;

    @NotNull @PositiveOrZero
    private Double priceUsd;

    @NotNull @PositiveOrZero
    private Double priceLkr;

    @NotNull @PositiveOrZero
    private Integer stockKg;
}
