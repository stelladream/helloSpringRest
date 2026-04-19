package kr.ac.hansung.cse.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================
 * Product - JPA 엔티티 클래스 (도메인 모델)
 * =====================================================================
 *
 * JPA(Java Persistence API)는 자바 객체와 관계형 데이터베이스 테이블을
 * 매핑(ORM: Object-Relational Mapping)하는 표준 명세입니다.
 * Hibernate가 이 표준을 구현합니다.
 *
 * [MVC에서 Model의 역할]
 * - 비즈니스 데이터를 표현하는 클래스입니다.
 * - Controller → Service → Repository를 거쳐 DB에서 가져온 데이터가
 *   이 객체 형태로 View(Thymeleaf)에 전달됩니다.
 *
 * [JPA 어노테이션 설명]
 * @Entity  : 이 클래스가 JPA 관리 대상(엔티티)임을 선언합니다.
 *            Hibernate가 이 클래스를 DB 테이블과 매핑합니다.
 * @Table   : 매핑할 DB 테이블 이름을 지정합니다. (생략 시 클래스 이름 사용)
 *
 * [Lombok 어노테이션 설명]
 * @Getter  : 모든 필드의 getter 메서드를 자동 생성합니다.
 * @Setter  : 모든 필드의 setter 메서드를 자동 생성합니다.
 * @ToString: toString() 메서드를 자동 생성합니다.
 *            exclude로 특정 필드를 제외할 수 있습니다.
 * @NoArgsConstructor: 매개변수 없는 기본 생성자를 자동 생성합니다.
 *            access = AccessLevel.PROTECTED: JPA 요구사항에 맞게
 *            protected 접근 제한자를 사용합니다.
 * @AllArgsConstructor: 모든 필드를 받는 생성자를 자동 생성합니다.
 *            exclude로 id를 제외하면 일반 생성자는 직접 작성해야 하므로
 *            여기서는 id를 포함한 전체 생성자를 생성합니다.
 */
@Entity
@Table(name = "product")
@Getter
@Setter
@ToString(exclude = {"description", "category", "productDetail", "tags"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    /**
     * @Id        : 기본 키(Primary Key) 필드 지정
     * @GeneratedValue : 기본 키 생성 전략 지정
     *   - GenerationType.IDENTITY : DB의 AUTO_INCREMENT를 사용합니다.
     *     INSERT 후 DB가 생성한 키 값을 Hibernate가 가져옵니다.
     *   - GenerationType.SEQUENCE : 시퀀스 객체 사용 (Oracle 등)
     *   - GenerationType.AUTO     : DB 방언에 따라 자동 선택
     * @Column    : 매핑할 DB 컬럼 정보 지정
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 상품명
     * nullable = false → DB에서 NOT NULL 제약 조건
     * length = 100    → VARCHAR(100)
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 카테고리 (예: 전자제품, 식품, 의류 등)
     */
    // ── ① @ManyToOne: 카테고리와 연관관계 (Owning Side) ─────────────────
    @ManyToOne(fetch = FetchType.LAZY)           // 성능을 위해 반드시 LAZY
    @JoinColumn(name = "category_id")            // FK 컬럼명: product.category_id
    private Category category;                   // 이 필드가 FK를 직접 관리


    /**
     * 가격
     * BigDecimal: 금액처럼 정밀한 소수 계산이 필요할 때 사용합니다.
     *   - double/float는 부동소수점 오차가 발생하므로 금액에 부적합합니다.
     *     예) 0.1 + 0.2 = 0.30000000000000004 (double 오차)
     *   - BigDecimal은 정확한 10진수 연산을 보장합니다.
     *   - Hibernate 6.x: double 타입에는 scale 지정 불가 → BigDecimal 사용 필수
     * precision = 10 : 전체 자릿수 (정수부 8자리 + 소수부 2자리)
     * scale = 2      : 소수점 이하 자릿수
     * → DB에서 DECIMAL(10, 2) 타입으로 매핑됩니다.
     */
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * 상품 설명 (긴 텍스트)
     * @Lob: Large Object를 의미하며 TEXT, CLOB 등으로 매핑됩니다.
     */
    @Lob
    @Column(name = "description")
    private String description;

    // ── @OneToOne: 상세 정보 (Owning Side, FK: product.product_detail_id) ─
    @OneToOne(cascade = CascadeType.ALL,           // 상세정보 함께 저장/삭제
            fetch = FetchType.LAZY,              // EAGER 기본값 → LAZY 오버라이드
            orphanRemoval = true)                // Product에서 연결 해제 시 자동 삭제
    @JoinColumn(name = "product_detail_id")        // FK: product 테이블에 위치
    private ProductDetail productDetail;


    // ── @ManyToMany: 태그 (Owning Side) ──────────────────────────────────
    @ManyToMany(fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}) // REMOVE 절대 금지!
    @JoinTable(
            name = "product_tag",                         // 조인 테이블명
            joinColumns = @JoinColumn(name = "product_id"),// 이 엔티티 FK
            inverseJoinColumns = @JoinColumn(name = "tag_id") // 상대 엔티티 FK
    )
    private List<Tag> tags = new ArrayList<>();

    // 편의 메서드
    public void addTag(Tag tag) { tags.add(tag); }


    /**
     * 새 Product를 생성할 때 사용하는 생성자
     * id는 DB가 자동 생성하므로 포함하지 않습니다.
     */
    public Product(String name, Category category, BigDecimal price, String description) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
    }
}
