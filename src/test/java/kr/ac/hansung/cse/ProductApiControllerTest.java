package kr.ac.hansung.cse;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ac.hansung.cse.controller.ProductController;
import kr.ac.hansung.cse.dto.ProductResponse;
import kr.ac.hansung.cse.exception.GlobalExceptionHandler;
import kr.ac.hansung.cse.exception.ProductNotFoundException;
import kr.ac.hansung.cse.model.Category;
import kr.ac.hansung.cse.model.Product;
import kr.ac.hansung.cse.model.ProductForm;
import kr.ac.hansung.cse.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * =====================================================================
 * ProductApiControllerTest - REST API 컨트롤러 단위 테스트
 * =====================================================================
 *
 * [테스트 전략]
 * - MockMvc standaloneSetup: 전체 Spring 컨텍스트 없이 컨트롤러만 테스트합니다.
 * - Mockito: ProductService를 목(Mock) 객체로 대체합니다.
 *   → DB 연결 없이 컨트롤러 로직(라우팅, 직렬화, 에러 처리)만 검증합니다.
 * - GlobalExceptionHandler도 함께 설정하여 예외 처리까지 통합 검증합니다.
 *
 * [테스트 대상 엔드포인트]
 * GET    /api/products
 * GET    /api/products/{id}
 * POST   /api/products
 * PUT    /api/products/{id}
 * DELETE /api/products/{id}
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Product REST API 단위 테스트")
class ProductApiControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    // ─────────────────────────────────────────────────────────────────
    // 테스트용 도메인 객체 생성 헬퍼
    // ─────────────────────────────────────────────────────────────────

    /**
     * Product 엔티티 생성 헬퍼
     * @NoArgsConstructor(access = PROTECTED) 우회를 위해
     * 공개 생성자 Product(name, category, price, description)를 사용합니다.
     */
     private Product buildProduct(Long id, String name, String categoryName,
                                 BigDecimal price, String description) {
        Category category = null;
        if (categoryName != null) {
            category = new Category(categoryName);
            category.setId(10L);
        }

        Product product = new Product(name, category, price, description);
        product.setId(id);
        return product;
    }

    /**
     * ProductResponse 생성 헬퍼 (서비스 mock 반환값용)
     */
    private ProductResponse buildResponse(Long id, String name, String categoryName,
                                          BigDecimal price, String description) {
        Product product = buildProduct(id, name, categoryName, price, description);
        return ProductResponse.from(product);
    }

    /**
     * 요청 바디로 사용할 ProductForm DTO 생성 헬퍼
     */
    private ProductForm buildForm(String name, String category, BigDecimal price, String description) {
        ProductForm form = new ProductForm();
        form.setName(name);
        form.setCategory(category);
        form.setPrice(price);
        form.setDescription(description);
        return form;
    }

    // =========================================================================
    // GET /api/products - 상품 목록 조회
    // =========================================================================

    @Nested
    @DisplayName("GET /api/products - 상품 목록 조회")
    class ListProducts {

        @Test
        @DisplayName("성공: 상품 2개 반환")
        void success_returnsTwoProducts() throws Exception {
            // given
            ProductResponse r1 = buildResponse(1L, "노트북", "전자제품", new BigDecimal("1500000"), "가성비 노트북");
            ProductResponse r2 = buildResponse(2L, "마우스", "전자제품", new BigDecimal("50000"), "무선 마우스");
            when(productService.getAllProducts()).thenReturn(List.of(r1, r2));

            // when & then
            mockMvc.perform(get("/api/products")
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("노트북"))
                    .andExpect(jsonPath("$.data[0].category").value("전자제품"))
                    .andExpect(jsonPath("$.data[1].id").value(2))
                    .andExpect(jsonPath("$.data[1].name").value("마우스"));
        }

        @Test
        @DisplayName("성공: 빈 목록 반환")
        void success_returnsEmptyList() throws Exception {
            // given
            when(productService.getAllProducts()).thenReturn(List.<ProductResponse>of());

            // when & then
            mockMvc.perform(get("/api/products")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }

    // =========================================================================
    // GET /api/products/{id} - 상품 단건 조회
    // =========================================================================

    @Nested
    @DisplayName("GET /api/products/{id} - 상품 단건 조회")
    class GetProduct {

        @Test
        @DisplayName("성공: 상품 정보 반환")
        void success_returnsProduct() throws Exception {
            // given
            ProductResponse response = buildResponse(1L, "노트북", "전자제품",
                    new BigDecimal("1500000"), "가성비 노트북");
            when(productService.getProductById(1L)).thenReturn(Optional.of(response));

            // when & then
            mockMvc.perform(get("/api/products/1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("노트북"))
                    .andExpect(jsonPath("$.data.category").value("전자제품"))
                    .andExpect(jsonPath("$.data.price").value(1500000))
                    .andExpect(jsonPath("$.data.description").value("가성비 노트북"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 ID → 404")
        void fail_productNotFound_returns404() throws Exception {
            // given
            when(productService.getProductById(999L)).thenReturn(Optional.empty());

            // when & then
            mockMvc.perform(get("/api/products/999")
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value(containsString("999")));
        }

        @Test
        @DisplayName("성공: 카테고리 없는 상품 (category = null)")
        void success_productWithoutCategory() throws Exception {
            // given
            ProductResponse response = buildResponse(2L, "기타 상품", null, new BigDecimal("10000"), "설명 없음");
            when(productService.getProductById(2L)).thenReturn(Optional.of(response));

            // when & then
            mockMvc.perform(get("/api/products/2")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.category").doesNotExist());
        }
    }

    // =========================================================================
    // POST /api/products - 상품 등록
    // =========================================================================

    @Nested
    @DisplayName("POST /api/products - 상품 등록")
    class CreateProduct {

        @Test
        @DisplayName("성공: 201 Created 반환")
        void success_returns201() throws Exception {
            // given
            ProductForm form = buildForm("노트북", "전자제품",
                    new BigDecimal("1500000"), "가성비 노트북");
            ProductResponse savedResponse = buildResponse(1L, "노트북", "전자제품",
                    new BigDecimal("1500000"), "가성비 노트북");

            when(productService.createProduct(any(ProductForm.class)))
                    .thenReturn(savedResponse);

            // when & then
            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("상품이 등록되었습니다."))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("노트북"))
                    .andExpect(jsonPath("$.data.price").value(1500000));
        }

        @Test
        @DisplayName("실패: 상품명 없음 → 400 Bad Request")
        void fail_noName_returns400() throws Exception {
            // given: name 필드 누락
            ProductForm form = buildForm(null, "전자제품",
                    new BigDecimal("1500000"), "설명");

            // when & then
            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));
        }

        @Test
        @DisplayName("실패: 가격 없음 → 400 Bad Request")
        void fail_noPrice_returns400() throws Exception {
            // given: price 필드 누락
            ProductForm form = buildForm("노트북", "전자제품", null, "설명");

            // when & then
            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("price")));
        }

        @Test
        @DisplayName("실패: 음수 가격 → 400 Bad Request")
        void fail_negativePrice_returns400() throws Exception {
            // given
            ProductForm form = buildForm("노트북", "전자제품",
                    new BigDecimal("-1000"), "설명");

            // when & then
            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("price")));
        }

        @Test
        @DisplayName("실패: 상품명 100자 초과 → 400 Bad Request")
        void fail_nameTooLong_returns400() throws Exception {
            // given: 101자 이름
            String longName = "A".repeat(101);
            ProductForm form = buildForm(longName, "전자제품",
                    new BigDecimal("1000"), "설명");

            // when & then
            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));
        }
    }

    // =========================================================================
    // PUT /api/products/{id} - 상품 수정
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/products/{id} - 상품 수정")
    class UpdateProduct {

        @Test
        @DisplayName("성공: 수정된 상품 반환")
        void success_returnsUpdatedProduct() throws Exception {
            // given
            ProductForm form = buildForm("노트북 Pro", "전자제품",
                    new BigDecimal("2000000"), "업그레이드된 노트북");

            ProductResponse updatedResponse = buildResponse(1L, "노트북 Pro", "전자제품",
                    new BigDecimal("2000000"), "업그레이드된 노트북");

            when(productService.updateProduct(eq(1L), any(ProductForm.class)))
                    .thenReturn(updatedResponse);

            // when & then
            mockMvc.perform(put("/api/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("상품이 수정되었습니다."))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("노트북 Pro"))
                    .andExpect(jsonPath("$.data.price").value(2000000));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 ID → 404")
        void fail_notFound_returns404() throws Exception {
            // given
            ProductForm form = buildForm("노트북", null,
                    new BigDecimal("1000000"), "설명");

            when(productService.updateProduct(eq(999L), any(ProductForm.class)))
                    .thenThrow(new ProductNotFoundException(999L));

            // when & then
            mockMvc.perform(put("/api/products/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("실패: 상품명 없음 → 400 Bad Request")
        void fail_validationError_returns400() throws Exception {
            // given: name 없음
            ProductForm form = buildForm(null, "전자제품",
                    new BigDecimal("1000000"), "설명");

            // when & then
            mockMvc.perform(put("/api/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));
        }
    }

    // =========================================================================
    // DELETE /api/products/{id} - 상품 삭제
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/products/{id} - 상품 삭제")
    class DeleteProduct {

        @Test
        @DisplayName("성공: 삭제 확인 메시지 반환")
        void success_returnsSuccessMessage() throws Exception {
            // given
            ProductResponse response = buildResponse(1L, "노트북", "전자제품",
                    new BigDecimal("1500000"), "설명");
            when(productService.getProductById(1L)).thenReturn(Optional.of(response));
            doNothing().when(productService).deleteProduct(1L);

            // when & then
            mockMvc.perform(delete("/api/products/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("상품이 삭제되었습니다."));

            verify(productService, times(1)).deleteProduct(1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 ID → 404")
        void fail_notFound_returns404() throws Exception {
            // given
            when(productService.getProductById(999L)).thenReturn(Optional.empty());

            // when & then
            mockMvc.perform(delete("/api/products/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"));

            verify(productService, never()).deleteProduct(anyLong());
        }
    }
}
