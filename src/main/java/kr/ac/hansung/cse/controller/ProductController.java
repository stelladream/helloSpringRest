package kr.ac.hansung.cse.controller;

import jakarta.validation.Valid;
import kr.ac.hansung.cse.dto.ApiResponse;
import kr.ac.hansung.cse.dto.ProductResponse;
import kr.ac.hansung.cse.exception.ProductNotFoundException;
import kr.ac.hansung.cse.model.ProductForm;
import kr.ac.hansung.cse.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * =====================================================================
 * ProductController - REST API 컨트롤러
 * =====================================================================
 *
 * @RestController = @Controller + @ResponseBody
 *   - 모든 메서드 반환값이 HTTP 응답 바디(JSON)로 직렬화됩니다.
 *   - View 이름 문자열 반환이 아닌 객체 반환 → Jackson이 JSON으로 변환합니다.
 *
 * [REST API 엔드포인트]
 * GET    /api/products          → 상품 전체 목록 조회
 * GET    /api/products/{id}     → 상품 단건 조회
 * POST   /api/products          → 상품 등록 (201 Created)
 * PUT    /api/products/{id}     → 상품 수정
 * DELETE /api/products/{id}     → 상품 삭제
 *
 * [응답 형식]
 * 성공: ApiResponse<T>  → { "success": true, "message": "...", "data": {...} }
 * 실패: ErrorResponse   → { "status": 4xx, "error": "...", "message": "..." }
 *       (GlobalExceptionHandler에서 처리)
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/products - 상품 전체 목록 조회
    // ─────────────────────────────────────────────────────────────────

    /**
     * 200 OK: 상품 목록 반환 (상품이 없으면 빈 배열), List<Product> → List<ProductResponse>
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> listProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/products/{id} - 상품 단건 조회
    // ─────────────────────────────────────────────────────────────────

    /**
     * 200 OK  : 상품 조회 성공
     * 404 Not Found : 해당 ID의 상품이 없음 (GlobalExceptionHandler 처리)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /api/products - 상품 등록
    // ─────────────────────────────────────────────────────────────────

    /**
     * @RequestBody : HTTP 요청 바디(JSON)를 ProductForm 객체로 역직렬화합니다.
     * @Valid       : ProductForm의 Bean Validation 어노테이션을 실행합니다.
     *               검증 실패 시 MethodArgumentNotValidException → GlobalExceptionHandler로 위임
     *
     * 201 Created : 상품 등록 성공
     * 400 Bad Request : 입력값 검증 실패
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductForm productForm) {

        ProductResponse savedProduct = productService.createProduct(productForm);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("상품이 등록되었습니다.", savedProduct));
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/products/{id} - 상품 수정
    // ─────────────────────────────────────────────────────────────────

    /**
     * 기존 상품의 모든 필드를 요청 바디 값으로 교체합니다. (PUT = 전체 교체)
     *
     * 200 OK        : 상품 수정 성공
     * 400 Bad Request : 입력값 검증 실패
     * 404 Not Found : 해당 ID의 상품이 없음
     */
// ProductController.java
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductForm productForm) {

        ProductResponse updatedProduct = productService.updateProduct(id, productForm);

        return ResponseEntity.ok(
                ApiResponse.success("상품이 수정되었습니다.", updatedProduct));
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE /api/products/{id} - 상품 삭제
    // ─────────────────────────────────────────────────────────────────

    /**
     * REST API는 HTML 폼 제약이 없으므로 HTTP DELETE 메서드를 사용합니다.
     *
     * 200 OK        : 상품 삭제 성공
     * 404 Not Found : 해당 ID의 상품이 없음
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.getProductById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("상품이 삭제되었습니다."));
    }
}
