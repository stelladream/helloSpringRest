package kr.ac.hansung.cse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.ac.hansung.cse.config.DbConfig;
import kr.ac.hansung.cse.model.Category;
import kr.ac.hansung.cse.model.Product;
import kr.ac.hansung.cse.model.ProductDetail;
import kr.ac.hansung.cse.model.Tag;
import kr.ac.hansung.cse.repository.CategoryRepository;
import kr.ac.hansung.cse.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DbConfig.class)
public class EntityRelationshipTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private TagRepository tagRepo;


    // ───────────────────────────────────────────────────────────────────
    // 실습 1-A: @ManyToOne 단방향
    // ───────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("실습1-A: @ManyToOne 단방향 - Product가 Category를 참조")
    public void test_ManyToOne_Unidirectional() {
        // [1] Category 저장 (먼저 저장해야 FK 참조 가능)
        Category electronics = new Category("전자제품");

        categoryRepo.save(electronics);
        em.flush();

        // [2] Product에 Category 설정 (FK 설정) - 생성자에서 Owning Side 설정
        Product laptop = new Product("테스트 노트북", electronics,
                new BigDecimal("1500000"), "테스트용 노트북");
        em.persist(laptop);
        em.flush(); em.clear();                   // 1차 캐시 초기화

        // [3] 저장된 Product 조회 → Category 확인
        Product found = em.find(Product.class, laptop.getId());
        assertNotNull(found.getCategory());
        assertEquals("전자제품", found.getCategory().getName());
        System.out.println("Category: " + found.getCategory().getName());
    }

    @Test
    @DisplayName("실습1-B: @OneToMany 양방향 - Category에서 Products 접근")
    public void test_OneToMany_Bidirectional() {
        Category electronics = new Category("전자제품");

        // category는 null로 생성 — addProduct()가 양쪽(product.category, category.products)을 동시에 설정
        Product p1 = new Product("노트북", null, new BigDecimal("1500000"), "테스트");
        Product p2 = new Product("마우스", null, new BigDecimal("30000"), "테스트");

        // addProduct() 편의 메서드 사용 → 양쪽 참조 동시 설정
        electronics.addProduct(p1);
        electronics.addProduct(p2);

        // CascadeType.ALL → Category 저장 시 Products도 함께 저장
        categoryRepo.save(electronics);
        em.flush(); em.clear();

        // JOIN FETCH로 Category + Products 한 번에 로드 (N+1 방지)
        Category found = categoryRepo.findByIdWithProducts(electronics.getId())
                .orElseThrow();
        assertEquals(2, found.getProducts().size());

        System.out.println("Products in '전자제품':");
        found.getProducts().forEach(p -> System.out.println("  - " + p.getName()));
    }

    @Test
    @DisplayName("실습2: @OneToOne - Product와 ProductDetail 함께 저장/조회/삭제")
    public void test_OneToOne() {
        // [1] ProductDetail 생성 및 Product에 연결
        ProductDetail detail = new ProductDetail(
                "Apple Inc.", "1년 무상 서비스", "M3 Pro, 18GB RAM, 512GB SSD");

        Product macbook = new Product("MacBook Pro", null,
                new BigDecimal("2990000"), "고성능 노트북");
        macbook.setProductDetail(detail);            // CascadeType.ALL 적용

        em.persist(macbook);                         // macbook + detail 함께 INSERT
        em.flush(); em.clear();

        // [2] 조회
        Product found = em.find(Product.class, macbook.getId());
        assertNotNull(found.getProductDetail());
        assertEquals("Apple Inc.", found.getProductDetail().getManufacturer());
        System.out.println("Manufacturer: " + found.getProductDetail().getManufacturer());

        // [3] 삭제: Product 삭제 → ProductDetail도 CascadeType.ALL로 함께 삭제
        Long detailId = found.getProductDetail().getId();
        em.remove(found);
        em.flush();

        assertNull(em.find(ProductDetail.class, detailId));
        System.out.println("ProductDetail도 함께 삭제됨: " + detailId);
    }

    // ───────────────────────────────────────────────────────────────────
// 실습 3: @ManyToMany
// ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("실습3: @ManyToMany - Product에 Tags 추가 및 조회")
    public void test_ManyToMany() {
        // [1] init.sql에서 이미 INSERT된 태그를 조회 (UNIQUE 제약으로 중복 INSERT 불가)
        Tag tagNew      = tagRepo.findByName("신상품").orElseThrow();
        Tag tagBest     = tagRepo.findByName("베스트셀러").orElseThrow();
        Tag tagDiscount = tagRepo.findByName("할인").orElseThrow();

        // [2] Product 생성 후 Tag 추가
        Product p1 = new Product("에어팟 프로", null, new BigDecimal("359000"), "무선 이어폰");
        Product p2 = new Product("클린코드",   null, new BigDecimal("33000"),  "로버트 마틴");

        p1.addTag(tagNew); p1.addTag(tagBest);          // 에어팟: 신상품, 베스트셀러
        p2.addTag(tagBest); p2.addTag(tagDiscount);     // 클린코드: 베스트셀러, 할인

        em.persist(p1);  em.persist(p2);               // product_tag에 자동 INSERT
        em.flush(); em.clear();

        // [3] JOIN FETCH로 조회 (LazyInitializationException 방지)
        Product foundP1 = em.createQuery(
                        "SELECT DISTINCT p FROM Product p JOIN FETCH p.tags WHERE p.id = :id",
                        Product.class)
                .setParameter("id", p1.getId())
                .getSingleResult();

        assertEquals(2, foundP1.getTags().size());
        System.out.println("에어팟 프로 태그:");
        foundP1.getTags().forEach(t -> System.out.println("  #" + t.getName()));

        // [4] 태그 하나만 제거
        foundP1.getTags().remove(tagNew);
        em.flush();   // product_tag에서 해당 행 DELETE
        assertEquals(1, foundP1.getTags().size());
    }

    @Test
    @DisplayName("통합: Category + ProductDetail + Tag 모두 적용")
    public void test_AllRelationships() {

        // ── [1] Category 생성 ──────────────────────────────────────────────
        Category electronics = categoryRepo.save(new Category("전자제품"));

        // ── [2] Tag 생성 ──────────────────────────────────────────────────
        Tag tagNew  = tagRepo.save(new Tag("신상품2"));
        Tag tagBest = tagRepo.save(new Tag("베스트2"));
        em.flush();

        // ── [3] Product 생성 (모든 연관관계 설정) ─────────────────────────
        ProductDetail detail = new ProductDetail("Samsung", "1년", "AMOLED 6.8인치");
        Product galaxy = new Product("갤럭시 S24", null,
                new BigDecimal("1550000"), "최신 플래그십");

        galaxy.setProductDetail(detail);    // @OneToOne
        galaxy.setCategory(electronics);    // @ManyToOne (FK 설정)
        galaxy.addTag(tagNew);              // @ManyToMany
        galaxy.addTag(tagBest);

        em.persist(galaxy);                 // cascade로 detail도 함께 저장
        em.flush(); em.clear();

        // ── [4] 전체 검증 ─────────────────────────────────────────────────
        Product found = em.createQuery(
                        "SELECT DISTINCT p FROM Product p "
                                + "JOIN FETCH p.tags "
                                + "WHERE p.id = :id", Product.class)
                .setParameter("id", galaxy.getId())
                .getSingleResult();

        // @ManyToOne 검증
        assertNotNull(found.getCategory());
        assertEquals("전자제품", found.getCategory().getName());

        // @OneToOne 검증
        assertNotNull(found.getProductDetail());
        assertEquals("Samsung", found.getProductDetail().getManufacturer());

        // @ManyToMany 검증
        assertEquals(2, found.getTags().size());

        System.out.println("=== 통합 테스트 결과 ===");
        System.out.println("카테고리: " + found.getCategory().getName());
        System.out.println("제조사:   " + found.getProductDetail().getManufacturer());
        System.out.println("태그: " + found.getTags().stream()
                .map(Tag::getName)
                .toList());
    }

}