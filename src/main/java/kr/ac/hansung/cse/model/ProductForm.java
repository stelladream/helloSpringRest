package kr.ac.hansung.cse.model;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 상품 등록/수정 요청 DTO (Data Transfer Object)
 *
 * REST API에서 @RequestBody로 수신하는 JSON을 바인딩합니다.
 *
 * [DTO를 사용하는 이유]
 * 1. 관심사 분리: JPA 엔티티(Product)는 DB 매핑 담당,
 *                DTO(ProductForm)는 API 요청 데이터 처리 담당
 * 2. 보안(Mass Assignment 방지): 클라이언트가 엔티티의
 *    민감한 필드(id 등)를 직접 수정하는 것을 방지
 * 3. Bean Validation 분리: DB 제약과 API 입력 검증 규칙을 독립 관리
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductForm {

    @NotBlank(message = "상품명은 필수 입력 항목입니다.")
    @Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
    private String name;

    @Size(max = 50, message = "카테고리는 50자 이하여야 합니다.")
    private String category;

    @NotNull(message = "가격은 필수 입력 항목입니다.")
    @DecimalMin(value = "0", inclusive = true, message = "가격은 0원 이상이어야 합니다.")
    @Digits(integer = 8, fraction = 2, message = "가격은 최대 99,999,999원까지, 소수점 2자리 이하로 입력해 주세요.")
    private BigDecimal price;

    @Size(max = 1000, message = "상품 설명은 1,000자 이하여야 합니다.")
    private String description;

    /**
     * ProductForm → Product 엔티티 변환 (등록/수정 시 사용)
     * id는 DB가 자동 생성하거나 URL 경로 변수로 전달되므로 포함하지 않습니다.
     */
    public Product toEntity() {
        return new Product(this.name, null, this.price, this.description);
    }
}
