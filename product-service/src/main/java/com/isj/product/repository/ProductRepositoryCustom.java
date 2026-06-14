package com.isj.product.repository;

import com.isj.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductRepositoryCustom {

    Optional<Product> findProductById(Long productId);

    /**
     * category, search 중 null인 조건은 무시하는 동적 쿼리.
     * 조건이 모두 null이면 전체 조회.
     */
    Page<Product> searchProducts(String category, String search, Pageable pageable);
}
