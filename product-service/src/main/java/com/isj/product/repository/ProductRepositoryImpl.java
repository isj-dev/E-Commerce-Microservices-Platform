package com.isj.product.repository;

import com.isj.product.domain.Product;
import com.isj.product.domain.QProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QProduct product = QProduct.product;

    @Override
    public Optional<Product> findProductById(Long productId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(product)
                        .where(product.id.eq(productId))
                        .fetchOne()
        );
    }

    @Override
    public Page<Product> searchProducts(String category, String search, Pageable pageable) {
        BooleanBuilder predicate = buildPredicate(category, search);

        List<Product> content = queryFactory
                .selectFrom(product)
                .where(predicate)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(product.createdAt.desc())
                .fetch();

        long total = Optional.ofNullable(
                queryFactory.select(product.count())
                        .from(product)
                        .where(predicate)
                        .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanBuilder buildPredicate(String category, String search) {
        BooleanBuilder builder = new BooleanBuilder();
        if (category != null && !category.isBlank()) {
            builder.and(product.category.eq(category));
        }
        if (search != null && !search.isBlank()) {
            builder.and(product.name.containsIgnoreCase(search));
        }
        return builder;
    }
}
