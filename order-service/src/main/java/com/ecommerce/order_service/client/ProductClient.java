package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.ProductResponse;
import com.ecommerce.order_service.exception.ProductNotFoundException;
import com.ecommerce.order_service.exception.ProductServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductClient {

    private final RestClient productRestClient;

    public ProductResponse getProduct(Long productId){
        log.info("Calling Product Service for product {}", productId);

        try{
            return productRestClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ProductNotFoundException(productId);
                    })
                    .body(ProductResponse.class);
        }catch (RestClientException e){
            log.error("Product Service call failed for product {}: {}", productId, e.getMessage());
            throw new ProductServiceUnavailableException(
                    "Product Service is unavailable, please try again later", e);
        }
    }
}
