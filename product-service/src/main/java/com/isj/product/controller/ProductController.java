package com.isj.product.controller;

import com.isj.common.dto.ApiResponse;
import com.isj.product.dto.ProductRequest;
import com.isj.product.dto.ProductResponse;
import com.isj.product.dto.StockRequest;
import com.isj.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok("Product created", productService.createProduct(request));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.ok(productService.getProduct(productId));
    }

    @GetMapping
    public ApiResponse<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(productService.getProducts(category, search, pageable));
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.updateProduct(productId, request));
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
    }

    @PutMapping("/{productId}/stock/decrease")
    public ApiResponse<Void> decreaseStock(
            @PathVariable Long productId,
            @RequestBody StockRequest request) {
        productService.decreaseStock(productId, request.getQuantity());
        return ApiResponse.ok(null);
    }

    @PutMapping("/{productId}/stock/increase")
    public ApiResponse<Void> increaseStock(
            @PathVariable Long productId,
            @RequestBody StockRequest request) {
        productService.increaseStock(productId, request.getQuantity());
        return ApiResponse.ok(null);
    }
}
