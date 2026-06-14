package com.isj.product.service;

import com.isj.common.exception.BusinessException;
import com.isj.common.exception.ErrorCode;
import com.isj.product.domain.Product;
import com.isj.product.dto.ProductRequest;
import com.isj.product.dto.ProductResponse;
import com.isj.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .build();
        return new ProductResponse(productRepository.save(product));
    }

    public ProductResponse getProduct(Long productId) {
        return new ProductResponse(findProductById(productId));
    }

    public Page<ProductResponse> getProducts(String category, String search, Pageable pageable) {
        return productRepository.searchProducts(category, search, pageable).map(ProductResponse::new);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        Product product = findProductById(productId);
        product.update(request.getName(), request.getDescription(), request.getPrice(),
                request.getCategory(), request.getImageUrl());
        return new ProductResponse(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        productRepository.delete(findProductById(productId));
    }

    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        Product product = findProductById(productId);
        try {
            product.decreaseStock(quantity);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    @Transactional
    public void increaseStock(Long productId, int quantity) {
        findProductById(productId).increaseStock(quantity);
    }

    private Product findProductById(Long productId) {
        return productRepository.findProductById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
