# JPA Entity 연관관계 정리

## 목차
1. [연관관계의 핵심 개념](#1-연관관계의-핵심-개념)
2. [@ManyToOne / @OneToMany — 상품과 카테고리](#2-manytoone--onetomany--상품과-카테고리)
3. [@OneToOne — 상품과 상세정보](#3-onetoone--상품과-상세정보)
4. [@ManyToMany — 상품과 태그](#4-manytomany--상품과-태그)
5. [공통 주의사항](#5-공통-주의사항)

---

## 1. 연관관계의 핵심 개념

### Owning Side vs Inverse Side

JPA의 양방향 관계에서 가장 중요한 개념입니다.

```
Owning Side (주인)   : FK 컬럼을 실제로 가진 쪽. DB 변경을 담당.
Inverse Side (거울)  : mappedBy로 선언. 조회 전용. DB에 영향 없음.
```

> **핵심 규칙**: DB에 반영하려면 반드시 **Owning Side** 에 값을 설정해야 합니다.
> Inverse Side만 설정하면 DB에 저장되지 않습니다.

### FK는 어느 테이블에?

| 관계 | FK 위치 |
|------|---------|
| `@ManyToOne` | Many 쪽 테이블 (`product.category_id`) |
| `@OneToOne` | Owning Side 테이블 (`product.product_detail_id`) |
| `@ManyToMany` | 별도 조인 테이블 (`product_tag`) |

### FetchType

| 옵션 | 동작 | 기본값 적용 대상 |
|------|------|-----------------|
| `LAZY` | 실제 접근 시점에 SQL 실행 (권장) | `@OneToMany`, `@ManyToMany` |
| `EAGER` | 조회 시 항상 함께 로드 (성능 주의) | `@ManyToOne`, `@OneToOne` |

> **모든 연관관계에 `FetchType.LAZY`를 명시하는 것이 실무 권장사항입니다.**

---

## 2. @ManyToOne / @OneToMany — 상품과 카테고리

### 구조

```
category 테이블          product 테이블
┌────────────┐           ┌──────────────────────┐
│ id  │ name │           │ id │ name │category_id│
│  1  │전자제품│◄──────── │  1 │노트북│     1     │
│  2  │ 도서  │           │  2 │마우스│     1     │
└────────────┘           └──────────────────────┘
  Category (1)               Product (N)
```

- 카테고리 1개에 상품 여러 개 → **1:N 관계**
- FK(`category_id`)는 **N 쪽(product 테이블)** 에 위치

### 엔티티 코드

**Product.java — Owning Side (FK 보유)**
```java
@ManyToOne(fetch = FetchType.LAZY)   // 성능을 위해 반드시 LAZY
@JoinColumn(name = "category_id")    // product 테이블의 FK 컬럼명
private Category category;           // 이 필드가 FK를 직접 관리
```

**Category.java — Inverse Side (조회 전용)**
```java
@OneToMany(
    mappedBy = "category",           // Product.java의 필드명과 일치해야 함
    fetch = FetchType.LAZY,
    cascade = CascadeType.ALL        // Category 저장/삭제 시 Product도 함께
)
private List<Product> products = new ArrayList<>();

// 편의 메서드: 양쪽 참조를 한 번에 설정
public void addProduct(Product product) {
    products.add(product);           // Inverse Side 설정
    product.setCategory(this);       // Owning Side(FK) 설정 ← 이게 핵심!
}
```

### 단방향 vs 양방향

**단방향 (@ManyToOne만 선언)**
```java
// Product → Category 방향만 가능
Product laptop = new Product("노트북", electronics, ...);
laptop.getCategory().getName();  // 가능

// Category → Product 방향 불가능
// electronics.getProducts()  → 이 메서드 자체가 없음
```

**양방향 (@OneToMany 추가)**
```java
// 양방향 탐색 가능
electronics.getProducts();       // Category → Products
laptop.getCategory();            // Product → Category
```

### 테스트 코드

**실습1-A: @ManyToOne 단방향**
```java
// [1] Category 먼저 저장 (FK 참조 대상이 있어야 하므로)
Category electronics = new Category("전자제품");
categoryRepo.save(electronics);
em.flush();

// [2] Product 생성 — 생성자에서 Owning Side(FK) 설정
Product laptop = new Product("테스트 노트북", electronics,
        new BigDecimal("1500000"), "테스트용 노트북");
em.persist(laptop);
em.flush(); em.clear();          // 1차 캐시 초기화

// [3] 조회 검증
Product found = em.find(Product.class, laptop.getId());
assertEquals("전자제품", found.getCategory().getName());
```

**실습1-B: @OneToMany 양방향**
```java
Category electronics = new Category("전자제품");

// category는 null — addProduct()가 양쪽 참조를 동시에 설정
Product p1 = new Product("노트북", null, new BigDecimal("1500000"), "테스트");
Product p2 = new Product("마우스", null, new BigDecimal("30000"), "테스트");

electronics.addProduct(p1);  // products.add(p1) + p1.setCategory(electronics)
electronics.addProduct(p2);

// CascadeType.ALL → Category 저장 시 Product도 함께 INSERT
categoryRepo.save(electronics);
em.flush(); em.clear();

// JOIN FETCH로 N+1 문제 방지
Category found = categoryRepo.findByIdWithProducts(electronics.getId())
        .orElseThrow();
assertEquals(2, found.getProducts().size());
```

### 자주 하는 실수

```java
// ❌ 잘못된 예: Inverse Side만 설정 → DB에 FK 저장 안 됨
electronics.getProducts().add(laptop);  // category_id = NULL 로 저장됨

// ✅ 올바른 예: Owning Side 설정
laptop.setCategory(electronics);        // category_id = 1 로 저장됨

// ✅ 더 좋은 예: 편의 메서드로 양쪽 한 번에
electronics.addProduct(laptop);         // 양쪽 모두 설정
```

---

## 3. @OneToOne — 상품과 상세정보

### 구조

```
product 테이블                    product_detail 테이블
┌─────────────────────────────┐   ┌──────────────────────────────┐
│ id │ name    │pd_detail_id  │   │ id │ manufacturer │ warranty │
│  1 │MacBook  │      1       │──►│  1 │  Apple Inc.  │   1년    │
└─────────────────────────────┘   └──────────────────────────────┘
        Product (Owning)                 ProductDetail (Inverse)
```

- 상품 1개 ↔ 상세정보 1개 → **1:1 관계**
- FK(`product_detail_id`)는 **Owning Side(product 테이블)** 에 위치

### 엔티티 코드

**Product.java — Owning Side**
```java
@OneToOne(
    cascade = CascadeType.ALL,    // Product 저장/삭제 시 ProductDetail도 함께
    fetch = FetchType.LAZY,       // 기본값 EAGER를 LAZY로 오버라이드 (성능)
    orphanRemoval = true          // product.setProductDetail(null) 시 DB에서도 삭제
)
@JoinColumn(name = "product_detail_id")  // FK: product 테이블에 위치
private ProductDetail productDetail;
```

### cascade와 orphanRemoval

| 옵션 | 동작 |
|------|------|
| `CascadeType.ALL` | Product persist/merge/remove 시 ProductDetail도 자동 처리 |
| `orphanRemoval = true` | `product.setProductDetail(null)` 하면 ProductDetail 행도 DELETE |

### 테스트 코드

```java
// [1] ProductDetail을 별도 persist 없이 Product에 연결만 해도 저장됨 (cascade)
ProductDetail detail = new ProductDetail(
        "Apple Inc.", "1년 무상 서비스", "M3 Pro, 18GB RAM, 512GB SSD");

Product macbook = new Product("MacBook Pro", null, new BigDecimal("2990000"), "노트북");
macbook.setProductDetail(detail);   // CascadeType.ALL 적용

em.persist(macbook);                // macbook + detail 함께 INSERT
em.flush(); em.clear();

// [2] 조회 — productDetail은 LAZY이므로 접근 시 SELECT 실행
Product found = em.find(Product.class, macbook.getId());
assertEquals("Apple Inc.", found.getProductDetail().getManufacturer());

// [3] Product 삭제 → ProductDetail도 CascadeType.ALL로 함께 삭제
Long detailId = found.getProductDetail().getId();
em.remove(found);
em.flush();

assertNull(em.find(ProductDetail.class, detailId));  // detail도 사라짐
```

### @OneToOne 기본값 주의

```java
// @OneToOne의 fetch 기본값은 EAGER
// → Product 조회 시 항상 JOIN으로 ProductDetail도 가져옴 (불필요한 경우도 포함)
// → 반드시 LAZY로 명시할 것
@OneToOne(fetch = FetchType.LAZY)  // 명시 필수
```

---

## 4. @ManyToMany — 상품과 태그

### 구조

```
product 테이블     product_tag (조인 테이블)     tag 테이블
┌──────────────┐   ┌──────────────────────┐   ┌─────────────┐
│ id │  name   │   │ product_id │ tag_id  │   │ id │  name  │
│  1 │에어팟 프로│──►│     1      │    1   │◄──│  1 │ 신상품  │
│  2 │클린코드  │   │     1      │    2   │◄──│  2 │베스트셀러│
└──────────────┘   │     2      │    2   │   │  3 │  할인   │
                   │     2      │    3   │   └─────────────┘
                   └──────────────────────┘
```

- 상품 여러 개 ↔ 태그 여러 개 → **N:M 관계**
- 조인 테이블(`product_tag`)로 N:M을 두 개의 1:N으로 분해

### 엔티티 코드

**Product.java — Owning Side**
```java
@ManyToMany(
    fetch = FetchType.LAZY,
    cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    // ⚠️ CascadeType.REMOVE 절대 금지!
    //    태그는 여러 상품이 공유 → Product 삭제 시 태그까지 삭제되면 안 됨
)
@JoinTable(
    name = "product_tag",                              // 조인 테이블명
    joinColumns = @JoinColumn(name = "product_id"),    // 이 엔티티 FK
    inverseJoinColumns = @JoinColumn(name = "tag_id")  // 상대 엔티티 FK
)
private List<Tag> tags = new ArrayList<>();

// 편의 메서드
public void addTag(Tag tag) { tags.add(tag); }
```

**Tag.java**
```java
@Entity
@EqualsAndHashCode(of = "id")  // ← 필수! remove() 시 id로 동등성 비교
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)     // 태그명 중복 불가
    private String name;
}
```

### @EqualsAndHashCode 가 필요한 이유

```java
em.flush(); em.clear();  // 1차 캐시 초기화 → tagNew는 detached 상태

// List.remove()는 내부적으로 equals()로 비교
foundP1.getTags().remove(tagNew);

// @EqualsAndHashCode 없을 때: Object 기본 동등성(참조 비교) 사용
// detached tagNew ≠ 컬렉션 안의 managed Tag → 제거 실패 ❌

// @EqualsAndHashCode(of = "id") 있을 때: id 값으로 비교
// 같은 id → 같은 객체로 판단 → 제거 성공 ✅
```

### CascadeType.REMOVE를 쓰면 안 되는 이유

```
에어팟 프로  ──┬──► 신상품
              └──► 베스트셀러 ◄──┐
클린코드     ──┬──► 베스트셀러 ──┘  (태그를 두 상품이 공유)
              └──► 할인

에어팟 프로 삭제 시 CascadeType.REMOVE가 있다면:
  → 신상품 태그 삭제 ✅
  → 베스트셀러 태그 삭제 ❌  (클린코드도 참조 중인데 삭제됨!)
```

### 테스트 코드

```java
// [1] init.sql에 이미 삽입된 태그 조회 (tag.name은 UNIQUE → 중복 INSERT 불가)
Tag tagNew      = tagRepo.findByName("신상품").orElseThrow();
Tag tagBest     = tagRepo.findByName("베스트셀러").orElseThrow();
Tag tagDiscount = tagRepo.findByName("할인").orElseThrow();

// [2] Product 생성 후 태그 추가
Product p1 = new Product("에어팟 프로", null, new BigDecimal("359000"), "무선 이어폰");
p1.addTag(tagNew);
p1.addTag(tagBest);

em.persist(p1);          // product_tag에 (p1.id, tagNew.id), (p1.id, tagBest.id) INSERT
em.flush(); em.clear();

// [3] JOIN FETCH로 조회 (LAZY 컬렉션을 트랜잭션 안에서 초기화)
Product foundP1 = em.createQuery(
        "SELECT DISTINCT p FROM Product p JOIN FETCH p.tags WHERE p.id = :id",
        Product.class)
    .setParameter("id", p1.getId())
    .getSingleResult();

assertEquals(2, foundP1.getTags().size());

// [4] 태그 제거 — @EqualsAndHashCode(of = "id") 덕분에 가능
foundP1.getTags().remove(tagNew);
em.flush();              // product_tag에서 해당 행 DELETE

assertEquals(1, foundP1.getTags().size());
```

---

## 5. 공통 주의사항

### LazyInitializationException

LAZY 연관관계는 트랜잭션 안에서만 초기화할 수 있습니다.

```java
// ❌ 트랜잭션 종료 후 LAZY 접근 → LazyInitializationException
Product product = productService.findById(1L);  // 트랜잭션 종료
product.getCategory().getName();                // 예외 발생!

// ✅ 해결: Repository에서 JOIN FETCH로 미리 로드
"SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id = :id"
```

### @ToString 무한순환

양방향 관계에서 `@ToString`이 서로를 호출하면 StackOverflowError가 발생합니다.

```java
// ❌ 위험: Product.toString() → category.toString() → products.toString()
//         → Product.toString() → ... (무한순환)

// ✅ 안전: 연관 필드를 @ToString에서 제외
@ToString(exclude = {"category", "productDetail", "tags"})  // Product
@ToString(exclude = "products")                              // Category
```

### 관계별 설정 요약

| 관계 | Owning Side | Inverse Side | 조인 테이블 | cascade 권장 |
|------|-------------|--------------|-------------|-------------|
| `@ManyToOne` / `@OneToMany` | `@ManyToOne` 쪽 (FK 보유) | `@OneToMany(mappedBy=)` | 없음 | Inverse Side에 ALL |
| `@OneToOne` | FK 보유 쪽 | `mappedBy` 쪽 | 없음 | Owning Side에 ALL + orphanRemoval |
| `@ManyToMany` | `@JoinTable` 선언 쪽 | `mappedBy` 쪽 | 별도 조인 테이블 | PERSIST, MERGE만 (REMOVE 금지) |
