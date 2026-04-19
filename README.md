# Spring MVC 상품 관리 시스템 (교육용)

Spring Boot 없이 **순수 Spring MVC 7.x** 로 구축한 교육용 웹 애플리케이션입니다.

---

## 아키텍처 개요

```
Browser
  │
  │ HTTP Request (예: GET /products)
  ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Apache Tomcat 11.x                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │               DispatcherServlet (프론트 컨트롤러)          │   │
│  │  - 모든 요청의 단일 진입점                                  │   │
│  │  - HandlerMapping으로 적절한 Controller 탐색              │   │
│  │  - ViewResolver로 최종 HTML 렌더링                        │   │
│  └──────────────┬───────────────────────────────────────────┘   │
│                 │ 요청 위임                                       │
│  ┌──────────────▼───────────────────────────────────────────┐   │
│  │          Controller (웹 계층)                             │   │
│  │  - @RequestMapping 으로 URL과 메서드 연결                  │   │
│  │  - Service 호출 후 Model에 데이터 담기                     │   │
│  │  - 반환할 View 이름 결정                                   │   │
│  └──────────────┬───────────────────────────────────────────┘   │
│                 │ 비즈니스 로직 위임                               │
│  ┌──────────────▼───────────────────────────────────────────┐   │
│  │          Service (비즈니스 계층)                           │   │
│  │  - @Transactional 로 트랜잭션 경계 정의                    │   │
│  │  - 비즈니스 규칙 검증 (가격 > 0 등)                        │   │
│  │  - Repository 호출 조합                                   │   │
│  └──────────────┬───────────────────────────────────────────┘   │
│                 │ 데이터 접근 위임                                 │
│  ┌──────────────▼───────────────────────────────────────────┐   │
│  │          Repository (데이터 접근 계층)                     │   │
│  │  - @PersistenceContext EntityManager 주입                 │   │
│  │  - JPQL 쿼리 실행 (find, createQuery 등)                  │   │
│  └──────────────┬───────────────────────────────────────────┘   │
│                 │ SQL 실행                                        │
│  ┌──────────────▼───────────────────────────────────────────┐   │
│  │          EntityManager (JPA / Hibernate)                  │   │
│  │  - 영속성 컨텍스트(1차 캐시) 관리                           │   │
│  │  - JPQL → SQL 변환                                        │   │
│  │  - 더티 체킹(Dirty Checking) 으로 자동 UPDATE              │   │
│  └──────────────┬───────────────────────────────────────────┘   │
└─────────────────┼───────────────────────────────────────────────┘
                  │ JDBC
                  ▼
          MySQL 9.x (Docker)
```

---

## 프로젝트 구조

```
helloSpringMVC/
├── pom.xml                          # Maven 빌드 설정 및 의존성
├── docker-compose.yml               # MySQL + Tomcat 컨테이너 설정
├── init.sql                         # DB 초기화 스크립트 (테이블 생성 + 샘플 데이터)
├── mysql-data/                      # MySQL 데이터 파일 저장 폴더 (자동 생성, git 제외)
├── .gitignore
├── README.md
└── src/
    └── main/
        ├── java/kr/ac/hansung/cse/
        │   ├── config/
        │   │   ├── WebAppInitializer.java  # web.xml 대체 (Servlet 컨테이너 설정)
        │   │   ├── WebConfig.java          # MVC 설정 (ViewResolver, 정적 리소스)
        │   │   └── DbConfig.java           # JPA/DB 설정 (DataSource, EntityManagerFactory)
        │   ├── model/
        │   │   └── Product.java            # JPA 엔티티 (@Entity, @Table)
        │   ├── repository/
        │   │   └── ProductRepository.java  # 데이터 접근 (@PersistenceContext EntityManager)
        │   ├── service/
        │   │   └── ProductService.java     # 비즈니스 로직 (@Transactional)
        │   └── controller/
        │       └── ProductController.java  # 웹 요청 처리 (@Controller)
        └── webapp/
            └── WEB-INF/
                └── views/
                    ├── productList.html    # 상품 목록 (Thymeleaf)
                    ├── productDetail.html  # 상품 상세
                    └── productForm.html    # 상품 등록 폼
```

---

## 기술 스택

| 분류 | 기술 | 버전 |
|------|------|------|
| 언어 | Java | 21 (LTS) |
| 웹 프레임워크 | Spring MVC | 7.0.x |
| 뷰 엔진 | Thymeleaf | 3.1.x |
| ORM | Hibernate (JPA) | 7.0.x |
| 데이터베이스 | MySQL | 9.1 |
| 서블릿 컨테이너 | Apache Tomcat | 11.0 |
| 빌드 도구 | Maven | 3.x |
| 인프라 | Docker Compose | - |

---

## 실행 방법

### 사전 요구사항
- Java 21+
- Maven 3.6+
- Docker Desktop

### 1단계: WAR 파일 빌드

```bash
mvn clean package -DskipTests
```

빌드 성공 시 `target/ROOT.war` 파일이 생성됩니다.

### 2단계: Docker Compose로 실행

```bash
# 컨테이너 시작 (백그라운드)
docker compose up -d

# 로그 확인 (Ctrl+C로 종료)
docker compose logs -f
```

컨테이너가 시작되면 프로젝트 폴더 안에 `mysql-data/` 디렉터리가 자동으로 생성되고,
MySQL 데이터 파일이 그 안에 저장됩니다. 탐색기(또는 `ls`)로 직접 확인할 수 있습니다.

### 3단계: 브라우저 접속

```
http://localhost:8080/products
```

### 컨테이너 중지

```bash
# 컨테이너 중지 (mysql-data/ 폴더가 남아있으므로 데이터 보존)
docker compose down

# DB 데이터까지 초기화하려면 폴더를 직접 삭제
rm -rf ./mysql-data
```

---

## Spring Context 구조

```
Root ApplicationContext (DbConfig)
  ├── DataSource (MySQL 연결)
  ├── EntityManagerFactory (JPA 설정)
  ├── TransactionManager (@Transactional 처리)
  ├── ProductRepository (@Repository)
  └── ProductService (@Service)
       │ (부모-자식 관계)
       ▼
Servlet ApplicationContext (WebConfig) - DispatcherServlet 소유
  ├── ThymeleafViewResolver
  ├── SpringTemplateEngine
  └── ProductController (@Controller)
       │ 부모 컨텍스트의 ProductService를 참조 가능
```

---

## 의존성 주입(DI) 흐름

```
Spring IoC 컨테이너
  │
  ├── ProductRepository 빈 생성
  │     └── @PersistenceContext EntityManager 주입
  │
  ├── ProductService 빈 생성
  │     └── 생성자 주입: ProductRepository 주입
  │
  └── ProductController 빈 생성
        └── 생성자 주입: ProductService 주입
```

---

## 주요 URL

| HTTP Method | URL | 설명 | Controller 메서드 |
|-------------|-----|------|-------------------|
| GET | `/products` | 상품 목록 | `listProducts()` |
| GET | `/products/{id}` | 상품 상세 | `productDetail()` |
| GET | `/products/create` | 등록 폼 표시 | `showCreateForm()` |
| POST | `/products/create` | 상품 등록 처리 | `createProduct()` |

---

## 학습 포인트

1. **web.xml 없는 서블릿 설정**: `WebAppInitializer`가 `AbstractAnnotationConfigDispatcherServletInitializer`를 상속하여 Java 코드로 서블릿 등록
2. **두 개의 ApplicationContext**: Root(DB/Service)와 Servlet(Web/Controller) 컨텍스트의 부모-자식 관계
3. **@PersistenceContext**: 스레드 안전한 EntityManager 프록시 주입 방식
4. **@Transactional AOP**: 트랜잭션 경계를 비침투적으로 적용하는 AOP 기반 처리
5. **PRG 패턴**: POST 후 리다이렉트로 중복 제출 방지
6. **Thymeleaf 폼 바인딩**: `th:object`, `th:field`를 활용한 양방향 데이터 바인딩
