package com.ceylonroots.service;

import com.ceylonroots.dto.ProductRequest;
import com.ceylonroots.exception.ApiException;
import com.ceylonroots.model.Product;
import com.ceylonroots.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ApiException("Product not found.", HttpStatus.NOT_FOUND));
    }

    public Product create(ProductRequest req) {
        Product p = Product.builder()
                .grade(req.getGrade())
                .name(req.getName())
                .description(req.getDescription())
                .priceUsd(req.getPriceUsd())
                .priceLkr(req.getPriceLkr())
                .stockKg(req.getStockKg())
                .build();
        return productRepository.save(p);
    }

    public Product update(Long id, ProductRequest req) {
        Product p = findById(id);
        p.setGrade(req.getGrade());
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setPriceUsd(req.getPriceUsd());
        p.setPriceLkr(req.getPriceLkr());
        p.setStockKg(req.getStockKg());
        return productRepository.save(p);
    }

    public void delete(Long id) {
        productRepository.delete(findById(id));
    }
}
