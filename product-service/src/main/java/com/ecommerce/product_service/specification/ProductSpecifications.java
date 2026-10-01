package com.ecommerce.product_service.specification;

import com.ecommerce.product_service.dto.ProductFilter;
import com.ecommerce.product_service.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ProductSpecifications {

    private ProductSpecifications() {
        // utility class, no instances
    }

    public static Specification<Product> fromFilter(ProductFilter filter) {
        List<Specification<Product>> specs = new ArrayList<>();

        if (hasText(filter.category())) {
            specs.add(hasCategory(filter.category()));
        }
        if (filter.minPrice() != null) {
            specs.add(priceAtLeast(filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            specs.add(priceAtMost(filter.maxPrice()));
        }
        if (hasText(filter.name())) {
            specs.add(nameContains(filter.name()));
        }
        if (Boolean.TRUE.equals(filter.inStock())) {
            specs.add(inStock());
        }

        // Combines everything with AND. No filters = return all products.
        return Specification.allOf(specs);
    }

    private static Specification<Product> hasCategory(String category) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase());
    }

    private static Specification<Product> priceAtLeast(BigDecimal minPrice) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    private static Specification<Product> priceAtMost(BigDecimal maxPrice) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    private static Specification<Product> nameContains(String name) {
        String pattern = "%" + escapeLike(name.trim().toLowerCase()) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\');
    }

    private static Specification<Product> inStock() {
        return (root, query, cb) -> cb.greaterThan(root.get("stockQuantity"), 0);
    }

    // Stops users from sending % or _ as wildcards
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}