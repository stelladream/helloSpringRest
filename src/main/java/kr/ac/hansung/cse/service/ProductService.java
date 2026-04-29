package kr.ac.hansung.cse.service;

import kr.ac.hansung.cse.dto.ProductResponse;
import kr.ac.hansung.cse.exception.ProductNotFoundException;
import kr.ac.hansung.cse.model.Category;
import kr.ac.hansung.cse.model.Product;
import kr.ac.hansung.cse.model.ProductForm;
import kr.ac.hansung.cse.repository.CategoryRepository;
import kr.ac.hansung.cse.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * =====================================================================
 * ProductService - 비즈니스 로직 계층 (Service Layer)
 * =====================================================================
 *
 * Service 계층의 역할:
 *   - 비즈니스 규칙(Business Logic)을 담당합니다.
 *   - Controller와 Repository 사이를 중재합니다.
 *   - 여러 Repository 호출을 조합하는 복합 작업을 처리합니다.
 *   - 트랜잭션 경계를 정의합니다.
 *
 * @Service: @Component의 특수화입니다.
 *   - Spring이 이 클래스를 빈으로 등록합니다.
 *   - 비즈니스 로직 담당 클래스임을 의미적으로 명확히 표현합니다.
 *
 * [의존성 주입(Dependency Injection)]
 * 이 클래스는 ProductRepository에 의존합니다.
 * Spring IoC 컨테이너가 ProductRepository 빈을 생성하여 주입해 줍니다.
 *
 * 생성자 주입(Constructor Injection)을 권장하는 이유:
 *   1. 의존성이 명시적으로 드러납니다.
 *   2. 불변(final) 필드로 선언 가능합니다.
 *   3. 단위 테스트 시 목(Mock) 객체 주입이 용이합니다.
 *   4. 순환 의존성을 컴파일 타임에 감지할 수 있습니다.
 *
 * [@Transactional 상세 설명]
 * 클래스 레벨에 선언 시 모든 public 메서드에 트랜잭션이 적용됩니다.
 *
 * 트랜잭션 전파(Propagation):
 *   - REQUIRED (기본값): 기존 트랜잭션이 있으면 참여, 없으면 새로 시작
 *   - REQUIRES_NEW: 항상 새 트랜잭션 시작 (기존 트랜잭션 일시 중단)
 *   - SUPPORTS: 트랜잭션이 있으면 참여, 없으면 없이 실행
 *
 * readOnly = true:
 *   - 읽기 전용 최적화: Hibernate의 더티 체킹(변경 감지)을 비활성화합니다.
 *   - DB 드라이버 레벨에서 읽기 전용 연결로 설정할 수 있어 성능이 향상됩니다.
 *   - 읽기 메서드에는 반드시 readOnly = true를 명시하는 것을 권장합니다.
 */
@Service
@Transactional(readOnly = true) // 클래스 기본값: 읽기 전용 트랜잭션
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * 카테고리 이름(String) → Category 엔티티 변환
     * 폼에서 받은 카테고리 이름을 DB에서 조회하여 엔티티로 변환합니다.
     * 비즈니스 로직이므로 Service 계층에 위치합니다.
     */
    public Category resolveCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return null;
        return categoryRepository.findByName(categoryName).orElse(null);
    }

    /**
     * 모든 상품 조회 - 트랜잭션 안에서 DTO로 변환하여 반환
     * LAZY 연관관계(category 등) 접근이 트랜잭션 범위 내에서 이루어집니다.
     */
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    /**
     * ID로 상품 조회 - 트랜잭션 안에서 DTO로 변환하여 반환
     */
    public Optional<ProductResponse> getProductById(Long id) {
        return productRepository.findById(id).map(ProductResponse::from);
    }

    /**
     * 새 상품 등록
     * ProductForm을 받아 서비스 내부에서 엔티티 변환 및 category resolve를 처리합니다.
     * 모든 LAZY 필드 접근이 동일한 트랜잭션 안에서 이루어집니다.
     */
    @Transactional
    public ProductResponse createProduct(ProductForm productForm) {
        Product product = productForm.toEntity();
        product.setCategory(resolveCategory(productForm.getCategory()));
        if (product.getPrice() != null && product.getPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("상품 가격은 0 이상이어야 합니다.");
        }
        return ProductResponse.from(productRepository.save(product));
    }

    /**
     * 상품 수정 - 트랜잭션 안에서 수정 후 DTO로 변환하여 반환
     * category resolve와 DTO 변환이 모두 같은 트랜잭션 안에서 이루어집니다.
     */
    @Transactional
    public ProductResponse updateProduct(Long id, ProductForm productForm) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setName(productForm.getName());
        product.setCategory(resolveCategory(productForm.getCategory()));
        product.setPrice(productForm.getPrice());
        product.setDescription(productForm.getDescription());

        return ProductResponse.from(productRepository.update(product));
    }

    /**
     * 상품 삭제
     */
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.delete(id);
    }
}
