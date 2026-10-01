package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.PageResponse;
import com.ecommerce.product_service.dto.ProductFilter;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.exception.DuplicateSkuException;
import com.ecommerce.product_service.exception.InvalidQueryParameterException;
import com.ecommerce.product_service.exception.ProductNotFoundException;
import com.ecommerce.product_service.repository.ProductRepository;
import com.ecommerce.product_service.specification.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private static final List<String> SORTABLE_FIELDS = List.of("name", "price", "stockQuantity", "createdAt");
    private static final int MAX_PAGE_SIZE = 50;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        // 1. Stop if the SKU is already taken
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }

        // 2. Build the entity from the request
        Product product = Product.builder()
                .name(request.name().trim())
                .description(request.description())
                .sku(request.sku())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .category(request.category().trim())
                .build();

        // 3. Save and return a safe response
        Product savedProduct = productRepository.save(product);
        return toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        // 1. Find the product, or 404
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // 2. Reject a SKU that belongs to a different product
        if (productRepository.existsBySkuAndIdNot(request.sku(), id)) {
            throw new DuplicateSkuException(request.sku());
        }

        // 3. Change the fields on the loaded entity
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setSku(request.sku());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(request.category().trim());

        // 4. Flush now so updatedAt is filled in before we build the response
        Product updated = productRepository.saveAndFlush(product);
        return toResponse(updated);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(ProductFilter filter,
                                                     int page, int size,
                                                     String sortBy, String direction) {
        validateFilter(filter);
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        Specification<Product> spec = ProductSpecifications.fromFilter(filter);

        Page<ProductResponse> result = productRepository.findAll(spec, pageable).map(this::toResponse);
        return PageResponse.from(result);
    }

    private void validateFilter(ProductFilter filter) {
        if (filter.minPrice() != null && filter.minPrice().signum() < 0) {
            throw new InvalidQueryParameterException("minPrice cannot be negative");
        }
        if (filter.maxPrice() != null && filter.maxPrice().signum() < 0) {
            throw new InvalidQueryParameterException("maxPrice cannot be negative");
        }
        if (filter.minPrice() != null && filter.maxPrice() != null
                && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw new InvalidQueryParameterException("minPrice cannot be greater than maxPrice");
        }
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        if (page < 0) {
            throw new InvalidQueryParameterException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidQueryParameterException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new InvalidQueryParameterException("sortBy must be one of " + SORTABLE_FIELDS);
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new InvalidQueryParameterException("direction must be 'asc' or 'desc'");
        }

        // Secondary sort by id keeps the order stable when values are equal
        Sort sort = Sort.by(sortDirection, sortBy).and(Sort.by("id"));
        return PageRequest.of(page, size, sort);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCategory(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}