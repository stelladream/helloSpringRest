# Bean Validation & Exception Handling 구현 가이드

> **프로젝트**: helloSpringMVC
> **기술 스택**: Spring MVC 7.0 · Hibernate Validator 8.0 · Thymeleaf 3.1 · Jakarta Validation API 3.x
> **대상**: 대학교 4학년 Spring MVC 실습

---

## 목차

1. [개요 및 추가 파일 목록](#1-개요-및-추가-파일-목록)
2. [Bean Validation](#2-bean-validation)
   - 2.1 DTO 패턴과 ProductForm
   - 2.2 Bean Validation 어노테이션
   - 2.3 컨트롤러 적용 (@Valid, BindingResult)
   - 2.4 Thymeleaf 오류 표시
   - 2.5 Bean Validation 전체 흐름
3. [Exception Handling](#3-exception-handling)
   - 3.1 커스텀 예외 클래스
   - 3.2 @ControllerAdvice 전역 예외 처리
   - 3.3 WebConfig 컴포넌트 스캔 설정
   - 3.4 에러 페이지 (error.html)
   - 3.5 예외 처리 전체 흐름
4. [계층별 방어 전략 (Defense in Depth)](#4-계층별-방어-전략-defense-in-depth)
5. [추가된 CRUD 엔드포인트](#5-추가된-crud-엔드포인트)

---

## 1. 개요 및 추가 파일 목록

이번 실습에서는 사용자 입력 데이터를 검증하는 **Bean Validation**과, 예외 상황을 일관되게 처리하는 **Exception Handling**을 추가했습니다.

### 변경 전 문제점

| 항목 | 변경 전 |
|------|---------|
| 폼 검증 | HTML `required` 속성만 사용 (클라이언트 전용, 우회 가능) |
| 예외 처리 | `IllegalArgumentException` 발생 시 Tomcat 기본 500 오류 페이지 |
| 예외 의미 | `IllegalArgumentException`이 모든 비즈니스 오류에 사용 (모호함) |
| 수정/삭제 | Controller에 엔드포인트 없음 |

### 추가/변경된 파일

```
src/main/java/kr/ac/hansung/cse/
  model/
    ProductForm.java          [신규] 폼 DTO - Bean Validation 어노테이션 적용
  exception/
    ProductNotFoundException.java   [신규] 커스텀 런타임 예외
    GlobalExceptionHandler.java     [신규] @ControllerAdvice 전역 예외 처리
  controller/
    ProductController.java    [수정] @Valid + BindingResult, 수정/삭제 엔드포인트 추가
  service/
    ProductService.java       [수정] updateProduct()에 가격 검증 추가
  config/
    WebConfig.java            [수정] exception 패키지 ComponentScan 추가

src/main/webapp/WEB-INF/views/
  productForm.html      [수정] th:classappend + th:errors로 검증 오류 표시
  productEditForm.html  [신규] 상품 수정 폼
  productList.html      [수정] 수정 링크 + 삭제 폼 컬럼 추가
  productDetail.html    [수정] 수정 버튼 + 삭제 폼 추가
  error.html            [신규] 전역 오류 페이지
```

---

## 2. Bean Validation

### 2.1 DTO 패턴과 ProductForm

기존 코드는 JPA 엔티티 `Product`를 폼 바인딩 객체로 직접 사용했습니다.
이를 별도의 **폼 DTO(Data Transfer Object)** 인 `ProductForm`으로 분리했습니다.

#### 왜 DTO를 분리하는가?

| 이유 | 설명 |
|------|------|
| **관심사 분리** | `Product`는 DB 매핑, `ProductForm`은 웹 레이어 데이터 처리를 담당 |
| **Mass Assignment 방지** | 클라이언트가 폼 전송으로 `id` 같은 민감 필드를 임의로 수정하는 공격 차단 |
| **검증 규칙 독립** | DB 제약 조건과 웹 폼 검증 규칙을 별도로 관리 |
| **생성자 보호** | `Product`는 JPA 요구사항으로 기본 생성자가 `PROTECTED` → 폼 바인딩 불가 |

#### ProductForm.java

```java
@Getter
@Setter
@NoArgsConstructor  // Spring MVC 폼 바인딩에 필수 (기본 생성자)
public class ProductForm {

    private Long id;  // 수정 시 사용, 등록 시 null

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

    // DTO → Entity 변환 (등록 시 사용)
    public Product toEntity() {
        return new Product(this.name, this.category, this.price, this.description);
    }

    // Entity → DTO 변환 (수정 폼 초기화 시 사용)
    public static ProductForm from(Product product) {
        ProductForm form = new ProductForm();
        form.id = product.getId();
        form.name = product.getName();
        form.category = product.getCategory();
        form.price = product.getPrice();
        form.description = product.getDescription();
        return form;
    }
}
```

---

### 2.2 Bean Validation 어노테이션

Bean Validation은 **JSR-380 표준**이며, 구현체로 **Hibernate Validator**를 사용합니다.
`pom.xml`에는 이미 두 의존성이 포함되어 있습니다.

```xml
<!-- Bean Validation 표준 API -->
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
</dependency>

<!-- Hibernate Validator: Bean Validation 구현체 -->
<dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
</dependency>
```

#### 어노테이션 비교표

| 어노테이션 | 적용 대상 | null | `""` | `" "` | 설명 |
|-----------|-----------|:----:|:----:|:-----:|------|
| `@NotNull` | 모든 타입 | ❌ | ✅ | ✅ | null만 거부 |
| `@NotEmpty` | String, Collection | ❌ | ❌ | ✅ | null + 빈 문자열 거부 |
| `@NotBlank` | String | ❌ | ❌ | ❌ | null + 빈 문자열 + 공백 거부 |
| `@Size(min, max)` | String, Collection | - | - | - | 길이/크기 범위 제한 |
| `@Min` / `@Max` | 정수형 | - | - | - | 정수 최솟값/최댓값 |
| `@DecimalMin` | BigDecimal, String | - | - | - | 소수 포함 최솟값 (`inclusive` 옵션) |
| `@Digits(integer, fraction)` | BigDecimal, String | - | - | - | 허용 자릿수 (정수부, 소수부) |
| `@Pattern(regexp)` | String | - | - | - | 정규 표현식 검증 |
| `@Email` | String | - | - | - | 이메일 형식 검증 |

> ❌ = 허용하지 않음, ✅ = 허용, - = 해당 없음

#### 각 필드의 검증 규칙 적용 이유

**`name` 필드 — `@NotBlank` + `@Size`**
```java
@NotBlank(message = "상품명은 필수 입력 항목입니다.")
@Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
private String name;
```
- 상품명은 공백만 입력해도 의미 없으므로 `@NotBlank` 사용 (`@NotNull`은 공백 허용)
- DB 컬럼이 `VARCHAR(100)` → `@Size(max=100)`으로 DB 저장 전 차단

**`price` 필드 — `@NotNull` + `@DecimalMin` + `@Digits`**
```java
@NotNull(message = "가격은 필수 입력 항목입니다.")
@DecimalMin(value = "0", inclusive = true, message = "가격은 0원 이상이어야 합니다.")
@Digits(integer = 8, fraction = 2, message = "...")
private BigDecimal price;
```
- `BigDecimal`은 문자열이 아니므로 `@NotBlank` 사용 불가 → `@NotNull` 사용
- `inclusive = true`: 0원 포함 허용 (무료 상품 등록 가능)
- `@Digits`로 DB 컬럼 `DECIMAL(10, 2)` 범위 초과 입력 사전 차단

---

### 2.3 컨트롤러 적용 (`@Valid`, `BindingResult`)

#### 핵심 변경 사항

```java
// 변경 전
@PostMapping("/create")
public String createProduct(@ModelAttribute Product product,
                            RedirectAttributes redirectAttributes) {
    productService.createProduct(product);
    return "redirect:/products";
}

// 변경 후
@PostMapping("/create")
public String createProduct(@Valid @ModelAttribute("productForm") ProductForm productForm,
                            BindingResult bindingResult,           // ← 반드시 바로 다음에
                            RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
        return "productForm";  // 오류 시 폼 재표시
    }
    productService.createProduct(productForm.toEntity());
    return "redirect:/products";
}
```

#### 주요 파라미터 설명

**`@Valid`**
- `ProductForm`에 선언된 Bean Validation 어노테이션들을 실행하도록 지시합니다.
- `jakarta.validation.Valid` 임포트 (Spring의 `@Validated`와 유사하나 그룹 기능 없음).

**`@ModelAttribute("productForm")`**
- HTTP 요청 파라미터를 `ProductForm` 객체에 자동 바인딩합니다.
- `"productForm"` 이름으로 Model에 자동 등록되어 Thymeleaf에서 `${productForm}`으로 접근 가능합니다.

**`BindingResult bindingResult`**
- 검증 결과(오류 목록)를 담는 객체입니다.
- **반드시 `@ModelAttribute` 파라미터 바로 다음에 선언해야 합니다** (Spring MVC 규칙).
- `BindingResult`가 없으면 검증 실패 시 `MethodArgumentNotValidException`이 발생합니다.

| `BindingResult` 주요 메서드 | 설명 |
|---------------------------|------|
| `hasErrors()` | 오류가 하나라도 있으면 `true` |
| `getErrorCount()` | 전체 오류 개수 |
| `getFieldError("name")` | 특정 필드의 첫 번째 오류 |
| `getAllErrors()` | 모든 오류 목록 |

---

### 2.4 Thymeleaf 오류 표시

검증 오류를 폼에 표시하는 두 가지 Thymeleaf 속성을 사용했습니다.

#### th:classappend — 오류 필드에 CSS 클래스 추가

```html
<input type="text"
       th:field="*{name}"
       th:classappend="${#fields.hasErrors('name')} ? 'field-error'"
       placeholder="상품명을 입력하세요" />
```

- `#fields`: Thymeleaf가 제공하는 폼 검증 유틸리티 객체
- `#fields.hasErrors('name')`: `name` 필드에 오류가 있으면 `true`
- 오류가 있을 때만 `field-error` CSS 클래스가 추가되어 빨간 테두리 표시

```css
.field-error { border-color: #e74c3c !important; }
```

#### th:errors — 오류 메시지 출력

```html
<span class="error-msg"
      th:if="${#fields.hasErrors('name')}"
      th:errors="*{name}">이름 오류 메시지</span>
```

- `th:if="${#fields.hasErrors('name')}"`: 오류가 없으면 이 요소 자체를 렌더링하지 않음
- `th:errors="*{name}"`: `@NotBlank`의 `message` 값이 이 자리에 출력됨
- 여러 오류가 있으면 `<br>`로 구분하여 모두 출력

#### 렌더링 결과 예시

빈 상품명으로 폼을 제출하면:

```html
<!-- 렌더링된 HTML -->
<input type="text" id="name" name="name" value=""
       class="field-error"
       placeholder="상품명을 입력하세요" />
<span class="error-msg">상품명은 필수 입력 항목입니다.</span>
```

---

### 2.5 Bean Validation 전체 흐름

```
사용자가 폼 제출 (POST /products/create)
         │
         ▼
  DispatcherServlet
         │
         ▼
  DataBinder: 폼 파라미터 → ProductForm 객체 바인딩
  (name, price, category, description → 각 필드에 설정)
         │
         ▼
  @Valid 트리거: Hibernate Validator 실행
  ┌─ @NotBlank(name) 검증
  ├─ @Size(max=100, name) 검증
  ├─ @NotNull(price) 검증
  ├─ @DecimalMin(price) 검증
  └─ @Digits(price) 검증
         │
    ┌────┴────┐
    │ 오류 있음 │                     │ 오류 없음 │
    ▼          ▼                     ▼
BindingResult에          ProductForm.toEntity() → Product 엔티티 생성
FieldError 저장                      │
    │                          productService.createProduct(product)
    │                                │
    ▼                          DB에 INSERT
return "productForm"                 │
(폼 재표시)                    return "redirect:/products"
    │                          (PRG 패턴)
    ▼
Thymeleaf 렌더링:
  th:classappend → 오류 필드 빨간 테두리
  th:errors      → 오류 메시지 출력
```

---

## 3. Exception Handling

### 3.1 커스텀 예외 클래스

기존 코드는 `IllegalArgumentException`을 모든 비즈니스 오류에 사용했습니다.
도메인 특화 예외 클래스인 `ProductNotFoundException`을 새로 만들었습니다.

```java
// exception/ProductNotFoundException.java
public class ProductNotFoundException extends RuntimeException {

    private final Long productId;  // 어떤 ID를 조회하다 실패했는지 기록

    public ProductNotFoundException(Long id) {
        super("존재하지 않는 상품 ID: " + id);
        this.productId = id;
    }

    public Long getProductId() {
        return productId;
    }
}
```

#### 커스텀 예외를 만드는 이유

| 이유 | 설명 |
|------|------|
| **의미 명확화** | `IllegalArgumentException` → `ProductNotFoundException`: 오류 원인이 명확 |
| **타입별 처리** | `@ExceptionHandler`에서 예외 종류에 따라 다른 처리 (다른 HTTP 상태 코드, 다른 메시지) |
| **도메인 정보 보존** | `productId` 필드로 어떤 ID가 문제였는지 핸들러에서 활용 가능 |

#### RuntimeException을 상속하는 이유

```
Throwable
 ├── Error              (JVM 수준 오류 - 개발자가 처리 불가)
 └── Exception
      ├── IOException           (검사 예외: try-catch 강제)
      └── RuntimeException      (비검사 예외: try-catch 강제 없음)
           └── ProductNotFoundException  ← 여기에 위치
```

| 구분 | 검사 예외 (Exception) | 비검사 예외 (RuntimeException) |
|------|----------------------|-------------------------------|
| try-catch 강제 | O (컴파일 오류) | X |
| Spring @Transactional 롤백 | 수동 설정 필요 | 자동 롤백 |
| Spring 관례 | 인프라 오류 (IOException 등) | 비즈니스/도메인 오류 |

> Spring 프로젝트에서는 비즈니스 예외를 `RuntimeException` 계열로 만드는 것이 관례입니다.

#### 컨트롤러에서의 사용

```java
// 변경 전
Product product = productService.getProductById(id)
    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품 ID: " + id));

// 변경 후
Product product = productService.getProductById(id)
    .orElseThrow(() -> new ProductNotFoundException(id));
//                      ↑ 커스텀 예외: 타입 정보 + productId 정보 포함
```

---

### 3.2 @ControllerAdvice 전역 예외 처리

`GlobalExceptionHandler`는 **모든 컨트롤러**에서 발생하는 예외를 한 곳에서 처리합니다.

```java
// exception/GlobalExceptionHandler.java
@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. 404: 상품 미발견
    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleProductNotFound(ProductNotFoundException ex, Model model) {
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorTitle", "상품을 찾을 수 없습니다");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorDetail",
            "요청하신 상품 ID (" + ex.getProductId() + ")에 해당하는 상품이 존재하지 않습니다.");
        return "error";
    }

    // 2. 400: 잘못된 입력값 (서비스 레이어 비즈니스 검증 실패)
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        model.addAttribute("errorCode", "400");
        model.addAttribute("errorTitle", "잘못된 요청");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorDetail", "입력하신 데이터를 확인하고 다시 시도해 주세요.");
        return "error";
    }

    // 3. 500: 데이터베이스 오류
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleDataAccessException(DataAccessException ex, Model model) {
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorTitle", "데이터베이스 오류");
        model.addAttribute("errorMessage", "데이터베이스 처리 중 오류가 발생했습니다.");
        model.addAttribute("errorDetail", "잠시 후 다시 시도해 주세요.");
        return "error";
    }

    // 4. 500: 나머지 모든 예외 (최종 안전망)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model) {
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorTitle", "서버 오류");
        model.addAttribute("errorMessage", "예상치 못한 오류가 발생했습니다.");
        model.addAttribute("errorDetail", "잠시 후 다시 시도해 주세요.");
        return "error";
    }
}
```

#### @ControllerAdvice 어노테이션

- AOP(관점 지향 프로그래밍) 기반으로 동작합니다.
- 컨트롤러마다 `try-catch`를 반복 작성하지 않아도 됩니다.
- `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody` (REST API용)

#### @ExceptionHandler 어노테이션

- 처리할 예외 타입을 `value`에 지정합니다.
- 파라미터로 예외 객체와 `Model`을 받을 수 있습니다.

#### @ResponseStatus 어노테이션

- 뷰를 반환하더라도 HTTP 응답 코드를 지정한 값으로 설정합니다.
- 없으면 기본값 `200 OK`로 응답됩니다.

#### 예외 처리 우선순위

예외가 발생하면 Spring은 **더 구체적인 타입**의 핸들러를 먼저 실행합니다.

```
ProductNotFoundException 발생 시:
  1순위: handleProductNotFound()  ← ProductNotFoundException 핸들러
  2순위: handleGenericException() ← Exception 핸들러 (해당 없음)

DataAccessException 발생 시:
  1순위: handleDataAccessException()  ← DataAccessException 핸들러
  2순위: handleGenericException()     ← 해당 없음

NullPointerException 발생 시:
  1순위: (해당 핸들러 없음)
  2순위: handleGenericException()     ← Exception의 서브클래스이므로 처리
```

---

### 3.3 WebConfig 컴포넌트 스캔 설정

`@ControllerAdvice`는 `@Component`의 특수화입니다. 따라서 Spring의 컴포넌트 스캔 범위 안에 포함되어야 빈으로 등록됩니다.

```java
// 변경 전
@ComponentScan(basePackages = "kr.ac.hansung.cse.controller")

// 변경 후
@ComponentScan(basePackages = {
    "kr.ac.hansung.cse.controller",
    "kr.ac.hansung.cse.exception"   // GlobalExceptionHandler 스캔을 위해 추가
})
```

> **왜 Servlet Context(WebConfig)에 추가하는가?**
> `@ControllerAdvice`는 웹 요청 처리 중 발생한 예외를 다루므로,
> Root Context(DbConfig)가 아닌 Servlet Context(WebConfig)에서 스캔해야 합니다.

---

### 3.4 에러 페이지 (error.html)

`GlobalExceptionHandler`의 모든 핸들러가 `"error"` 뷰 이름을 반환하며,
Thymeleaf가 `/WEB-INF/views/error.html`을 렌더링합니다.

```html
<!-- views/error.html 핵심 부분 -->
<p class="error-code" th:text="${errorCode}">500</p>
<h1 class="error-title" th:text="${errorTitle}">오류가 발생했습니다</h1>

<div class="error-box">
    <p th:if="${errorMessage}">
        <span class="error-label">오류 내용: </span>
        <span th:text="${errorMessage}">오류 메시지</span>
    </p>
    <p th:if="${errorDetail}">
        <span class="error-label">안내: </span>
        <span th:text="${errorDetail}">상세 안내</span>
    </p>
</div>
```

Model에 담기는 데이터:

| 키 | 예시 값 | 설명 |
|----|---------|------|
| `errorCode` | `"404"` | HTTP 상태 코드 문자열 |
| `errorTitle` | `"상품을 찾을 수 없습니다"` | 사람이 읽기 좋은 오류 제목 |
| `errorMessage` | `"존재하지 않는 상품 ID: 999"` | 오류 내용 요약 |
| `errorDetail` | `"요청하신 상품 ID (999)에..."` | 사용자 안내 문구 |

---

### 3.5 예외 처리 전체 흐름

```
GET /products/999 요청 (존재하지 않는 ID)
         │
         ▼
  ProductController.productDetail(id=999)
         │
         ▼
  productService.getProductById(999)
  → Optional.empty() 반환
         │
         ▼
  .orElseThrow(() -> new ProductNotFoundException(999))
  → ProductNotFoundException 발생!
         │
         ▼
  DispatcherServlet이 예외를 캐치
         │
         ▼
  HandlerExceptionResolver가 @ControllerAdvice 탐색
         │
         ▼
  GlobalExceptionHandler.handleProductNotFound() 실행
  → model.addAttribute("errorCode", "404")
  → model.addAttribute("errorTitle", "상품을 찾을 수 없습니다")
  → model.addAttribute("errorMessage", "존재하지 않는 상품 ID: 999")
  → model.addAttribute("errorDetail", "요청하신 상품 ID (999)에 해당하는 상품이 존재하지 않습니다.")
  → return "error"
         │
         ▼
  @ResponseStatus(HttpStatus.NOT_FOUND) → HTTP 404
         │
         ▼
  ThymeleafViewResolver → error.html 렌더링
         │
         ▼
  브라우저에 404 오류 페이지 표시
```

---

## 4. 계층별 방어 전략 (Defense in Depth)

입력 검증을 여러 계층에 나눠 적용하는 **다층 방어** 전략을 사용했습니다.

```
[ 계층 1: 클라이언트 ]
  HTML input의 type="number", min="0"
  → 브라우저가 기본 UI 차원에서 막아줌
  → 단점: 개발자 도구나 curl로 우회 가능

       ↓ 우회 시

[ 계층 2: 웹 레이어 (Controller) ]
  @Valid + Bean Validation
  → @NotBlank, @NotNull, @DecimalMin 등 어노테이션 검증
  → 실패 시 폼에 오류 메시지 표시 (사용자 친화적)
  → 단점: 컨트롤러를 통하지 않는 Service 직접 호출은 검증 안됨

       ↓ 우회 시

[ 계층 3: 서비스 레이어 (Service) ]
  createProduct(), updateProduct() 내 명시적 검증
  → "가격 < 0이면 IllegalArgumentException 발생"
  → 어떤 클라이언트(웹/API/배치)가 호출해도 비즈니스 규칙 보장
  → GlobalExceptionHandler가 400 Bad Request로 처리

       ↓ 우회 시

[ 계층 4: 데이터베이스 ]
  NOT NULL, DECIMAL(10,2) 등 DB 제약 조건
  → DataAccessException 발생
  → GlobalExceptionHandler가 500으로 처리
```

> **핵심 원칙**: 서비스 레이어는 "웹에서 오든, API에서 오든, 테스트에서 오든"
> 항상 동일한 비즈니스 규칙을 보장해야 합니다.

---

## 5. 추가된 CRUD 엔드포인트

Bean Validation 적용과 함께 미구현 상태였던 수정·삭제 기능도 추가했습니다.

### 전체 엔드포인트 목록

| HTTP Method | URL | 설명 | 검증 | 예외 처리 |
|-------------|-----|------|:----:|:--------:|
| GET | `/products` | 상품 목록 | - | - |
| GET | `/products/{id}` | 상품 상세 | - | `ProductNotFoundException` |
| GET | `/products/create` | 등록 폼 표시 | - | - |
| **POST** | `/products/create` | **등록 처리** | **@Valid** | `ProductNotFoundException` |
| GET | `/products/{id}/edit` | 수정 폼 표시 | - | `ProductNotFoundException` |
| **POST** | `/products/{id}/edit` | **수정 처리** | **@Valid** | `ProductNotFoundException` |
| POST | `/products/{id}/delete` | 삭제 처리 | - | `ProductNotFoundException` |

### HTML 폼의 HTTP 메서드 제한

HTML `<form>` 태그는 `GET`과 `POST`만 지원합니다.
따라서 수정과 삭제 모두 `POST` 방식을 사용합니다.
(REST API에서는 각각 `PUT/PATCH`, `DELETE`를 사용하는 것이 표준)

```html
<!-- 삭제 처리: POST /products/{id}/delete -->
<form th:action="@{/products/{id}/delete(id=${product.id})}"
      method="post"
      onsubmit="return confirm('정말로 삭제하시겠습니까?');">
    <button type="submit">삭제</button>
</form>
```

### JPA 엔티티 수정 시 준영속(Detached) 상태

수정 처리에서 중요한 JPA 개념이 적용됩니다.

```java
@PostMapping("/{id}/edit")
public String updateProduct(@PathVariable Long id,
                            @Valid @ModelAttribute("productForm") ProductForm productForm,
                            BindingResult bindingResult, ...) {

    if (bindingResult.hasErrors()) {
        return "productEditForm";
    }

    // ① readOnly 트랜잭션에서 조회 → 트랜잭션 종료 후 준영속(Detached) 상태
    Product product = productService.getProductById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

    // ② 준영속 엔티티의 필드를 폼 데이터로 업데이트
    //    (영속성 컨텍스트가 없으므로 DB에 바로 반영되지 않음)
    product.setName(productForm.getName());
    product.setPrice(productForm.getPrice());

    // ③ merge(): 새 트랜잭션에서 준영속 엔티티를 영속 상태로 전환
    //    → Hibernate가 SELECT + 변경 감지 + UPDATE SQL 자동 실행
    productService.updateProduct(product);
}
```

**엔티티 생명주기 요약:**

```
new Product()     → 비영속(Transient)
    │ persist()
    ▼
영속(Persistent)   ← 영속성 컨텍스트가 관리 (변경 감지 작동)
    │ 트랜잭션 종료
    ▼
준영속(Detached)   ← 관리 중단 (setter 호출해도 DB 반영 안됨)
    │ merge()
    ▼
영속(Persistent)   ← 다시 관리 시작 → 트랜잭션 커밋 시 UPDATE
```
