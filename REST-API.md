# Product REST API 문서

## 개요

| 항목 | 내용 |
|---|---|
| Base URL | `/api/products` |
| 데이터 형식 | JSON (`Content-Type: application/json`) |
| 프레임워크 | Spring 7.0 (Spring Boot 미사용) |
| 배포 방식 | WAR → 외부 Tomcat |

---

## 공통 응답 형식

### 성공 응답

```json
{
  "success": true,
  "message": "성공 메시지 (선택)",
  "data": { ... }
}
```

### 에러 응답

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "에러 상세 메시지",
  "fieldErrors": null
}
```

### 유효성 검사 실패 응답 (400)

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "입력값 검증에 실패했습니다.",
  "fieldErrors": [
    { "field": "name", "message": "상품명은 필수 입력 항목입니다." },
    { "field": "price", "message": "가격은 필수 입력 항목입니다." }
  ]
}
```

---

## 엔드포인트 목록

| 메서드 | URL | 설명 | 상태 코드 |
|---|---|---|---|
| GET | `/api/products` | 상품 전체 목록 조회 | 200 |
| GET | `/api/products/{id}` | 상품 단건 조회 | 200, 404 |
| POST | `/api/products` | 상품 등록 | 201, 400 |
| PUT | `/api/products/{id}` | 상품 수정 | 200, 400, 404 |
| DELETE | `/api/products/{id}` | 상품 삭제 | 200, 404 |

---

## 엔드포인트 상세

### 1. 상품 목록 조회

```
GET /api/products
```

**요청 헤더**
```
Accept: application/json
```

**응답 예시 (200 OK)**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "name": "노트북",
      "category": "전자제품",
      "price": 1500000,
      "description": "가성비 노트북"
    },
    {
      "id": 2,
      "name": "마우스",
      "category": "전자제품",
      "price": 50000,
      "description": "무선 마우스"
    }
  ]
}
```

> 상품이 없으면 `data`는 빈 배열 `[]`로 반환됩니다.

---

### 2. 상품 단건 조회

```
GET /api/products/{id}
```

**경로 변수**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| id | Long | Y | 상품 ID |

**응답 예시 (200 OK)**
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 1,
    "name": "노트북",
    "category": "전자제품",
    "price": 1500000,
    "description": "가성비 노트북"
  }
}
```

**응답 예시 (404 Not Found)**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "존재하지 않는 상품 ID: 999",
  "fieldErrors": null
}
```

---

### 3. 상품 등록

```
POST /api/products
Content-Type: application/json
```

**요청 바디**

| 필드 | 타입 | 필수 | 제약 조건 | 설명 |
|---|---|---|---|---|
| name | String | Y | 1~100자, 공백 불가 | 상품명 |
| category | String | N | 50자 이하 | 카테고리 이름 (DB에 존재해야 함) |
| price | BigDecimal | Y | 0 이상, 최대 99,999,999 | 가격 (소수점 2자리까지) |
| description | String | N | 1000자 이하 | 상품 설명 |

**요청 예시**
```json
{
  "name": "노트북",
  "category": "전자제품",
  "price": 1500000,
  "description": "가성비 좋은 15인치 노트북"
}
```

**응답 예시 (201 Created)**
```json
{
  "success": true,
  "message": "상품이 등록되었습니다.",
  "data": {
    "id": 3,
    "name": "노트북",
    "category": "전자제품",
    "price": 1500000,
    "description": "가성비 좋은 15인치 노트북"
  }
}
```

**응답 예시 (400 Bad Request - 유효성 검사 실패)**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "입력값 검증에 실패했습니다.",
  "fieldErrors": [
    {
      "field": "name",
      "message": "상품명은 필수 입력 항목입니다."
    }
  ]
}
```

---

### 4. 상품 수정

```
PUT /api/products/{id}
Content-Type: application/json
```

기존 상품의 모든 필드를 요청 바디 값으로 전체 교체합니다.

**경로 변수**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| id | Long | Y | 수정할 상품 ID |

**요청 바디** (등록과 동일한 형식)

```json
{
  "name": "노트북 Pro",
  "category": "전자제품",
  "price": 2000000,
  "description": "업그레이드된 고성능 노트북"
}
```

**응답 예시 (200 OK)**
```json
{
  "success": true,
  "message": "상품이 수정되었습니다.",
  "data": {
    "id": 1,
    "name": "노트북 Pro",
    "category": "전자제품",
    "price": 2000000,
    "description": "업그레이드된 고성능 노트북"
  }
}
```

**에러 응답**
- `400 Bad Request`: 유효성 검사 실패
- `404 Not Found`: 해당 ID의 상품이 없음

---

### 5. 상품 삭제

```
DELETE /api/products/{id}
```

**경로 변수**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| id | Long | Y | 삭제할 상품 ID |

**응답 예시 (200 OK)**
```json
{
  "success": true,
  "message": "상품이 삭제되었습니다.",
  "data": null
}
```

**응답 예시 (404 Not Found)**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "존재하지 않는 상품 ID: 999",
  "fieldErrors": null
}
```

---

## HTTP 상태 코드 요약

| 코드 | 의미 | 발생 상황 |
|---|---|---|
| 200 OK | 요청 성공 | 조회, 수정, 삭제 성공 |
| 201 Created | 리소스 생성 성공 | 상품 등록 성공 |
| 400 Bad Request | 잘못된 요청 | 유효성 검사 실패, 비즈니스 규칙 위반 |
| 404 Not Found | 리소스 없음 | 존재하지 않는 상품 ID 요청 |
| 500 Internal Server Error | 서버 오류 | DB 오류, 예상치 못한 예외 |

---

## cURL 테스트 예시

```bash
# 목록 조회
curl -X GET http://localhost:8080/api/products \
  -H "Accept: application/json"

# 단건 조회
curl -X GET http://localhost:8080/api/products/1 \
  -H "Accept: application/json"

# 상품 등록
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"노트북","category":"전자제품","price":1500000,"description":"가성비 노트북"}'

# 상품 수정
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"노트북 Pro","category":"전자제품","price":2000000,"description":"고성능 노트북"}'

# 상품 삭제
curl -X DELETE http://localhost:8080/api/products/1
```

---

## 아키텍처

```
HTTP 요청 (JSON)
    ↓
DispatcherServlet
    ↓
ProductController (@RestController)
  - @RequestBody → Jackson 역직렬화
  - @Valid → Bean Validation
    ↓
ProductService (@Transactional)
    ↓
ProductRepository (EntityManager / JPQL)
    ↓
MySQL (Hibernate 7.0)
    ↓
ProductResponse DTO → Jackson 직렬화 → JSON 응답
```

### 관련 클래스

| 클래스 | 역할 |
|---|---|
| `ProductController` | REST 엔드포인트, 요청/응답 매핑 |
| `ProductService` | 비즈니스 로직, 트랜잭션 관리 |
| `ProductRepository` | JPA EntityManager 기반 CRUD |
| `ProductForm` | 요청 바디 DTO (Bean Validation 포함) |
| `ProductResponse` | 응답 DTO (엔티티 직접 노출 방지) |
| `ApiResponse<T>` | 공통 성공 응답 래퍼 |
| `ErrorResponse` | 공통 에러 응답 DTO |
| `GlobalExceptionHandler` | `@RestControllerAdvice` 전역 예외 처리 |
