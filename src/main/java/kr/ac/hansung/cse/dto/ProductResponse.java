package kr.ac.hansung.cse.dto;

import kr.ac.hansung.cse.model.Product;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 상품 API 응답 DTO
 *
 * JPA 엔티티(Product)를 직접 직렬화하면 LAZY 연관관계에서
 * LazyInitializationException이 발생할 수 있습니다.
 * DTO로 변환하여 필요한 필드만 노출합니다.
 */
@Getter
public class ProductResponse {

    private final Long id;
    private final String name;
    private final String category;
    private final BigDecimal price;
    private final String description;

    private ProductResponse(Long id, String name, String category, BigDecimal price, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
    }

    /**
     * Product 엔티티 → ProductResponse DTO 변환
     * 트랜잭션 범위 내에서 호출해야 LAZY 필드에 접근할 수 있습니다.
     */
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getPrice(),
                product.getDescription()
        );
    }
}
