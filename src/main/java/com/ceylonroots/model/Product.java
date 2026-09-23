package com.ceylonroots.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String grade;      // e.g. "Alba"

    @Column(nullable = false)
    private String name;       // e.g. "Alba — 6mm Quills"

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Double priceUsd;

    @Column(nullable = false)
    private Double priceLkr;

    @Column(nullable = false)
    private Integer stockKg;
}
