package kr.ac.hansung.cse.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import kr.ac.hansung.cse.model.Product;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * =====================================================================
 * ProductRepository - 데이터 접근 계층 (Repository Layer)
 * =====================================================================
 *
 * Repository 패턴: 데이터 저장소(DB)에 대한 접근 로직을 캡슐화합니다.
 * Service 계층은 데이터가 어디서 오는지(DB, 캐시, 파일 등) 알 필요 없이
 * Repository 인터페이스만 사용합니다.
 *
 * @Repository : 다음 두 가지 역할을 합니다.
 *   1. @Component의 특수화 → Spring이 이 클래스를 빈으로 등록합니다.
 *   2. DataAccessException 변환기 활성화 →
 *      JDBC/JPA 예외를 Spring의 DataAccessException 계층으로 변환합니다.
 *      덕분에 서비스 계층이 특정 DB 기술의 예외에 의존하지 않아도 됩니다.
 *
 * [EntityManager 란?]
 * JPA의 핵심 인터페이스로 엔티티의 생명주기를 관리합니다.
 *
 * 엔티티 생명주기:
 *   Transient(비영속) → persist() → Managed(영속)
 *   Managed(영속)     → remove()  → Removed(삭제)
 *   Managed(영속)     → detach()  → Detached(준영속)
 *
 * [영속성 컨텍스트(Persistence Context)]
 * EntityManager가 관리하는 1차 캐시입니다.
 * 같은 트랜잭션 내에서 동일한 엔티티를 두 번 조회하면
 * DB가 아닌 영속성 컨텍스트에서 반환합니다.(1차 캐시)
 * 트랜잭션 종료 시 변경된 엔티티를 자동으로 DB에 반영합니다.(더티 체킹)
 */
@Repository
public class ProductRepository {

    /**
     * @PersistenceContext : Spring이 EntityManager를 주입해 주는 어노테이션입니다.
     *
     * 주의: EntityManager는 스레드 안전(Thread-safe)하지 않습니다.
     * 하지만 @PersistenceContext로 주입된 EntityManager는 실제로
     * 트랜잭션 범위의 프록시 객체입니다.
     * → 실제 EntityManager는 현재 트랜잭션에 바인딩된 것을 사용합니다.
     * → 덕분에 멀티스레드 환경에서도 안전하게 사용 가능합니다.
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 모든 상품 목록 조회
     *
     * JPQL(Java Persistence Query Language):
     *   - SQL과 유사하지만 테이블이 아닌 엔티티 클래스를 대상으로 합니다.
     *   - "FROM Product p" → product 테이블이 아닌 Product 엔티티를 조회
     *   - Hibernate가 실제 SQL로 변환하여 실행합니다.
     *
     * TypedQuery<Product>: 타입 안전한 쿼리 (ClassCastException 방지)
     */
    public List<Product> findAll() {
        // LEFT JOIN FETCH: LAZY인 category를 한 번의 쿼리로 함께 로드 (LazyInitializationException 방지)
        TypedQuery<Product> query = entityManager
                .createQuery("SELECT p FROM Product p LEFT JOIN FETCH p.category ORDER BY p.id ASC", Product.class);
        return query.getResultList();
    }

    /**
     * ID로 단일 상품 조회
     *
     * EntityManager.find():
     *   - 기본 키로 엔티티를 조회합니다.
     *   - 1차 캐시(영속성 컨텍스트)를 먼저 확인 후 없으면 DB 조회
     *   - 존재하지 않으면 null을 반환합니다.
     *
     * Optional<T>: null 반환 대신 Optional로 감싸서 null 처리를 명시적으로 강제합니다.
     *              Java 8+ 권장 패턴입니다.
     */
    public Optional<Product> findById(Long id) {
        // LEFT JOIN FETCH: em.find()는 JOIN FETCH 불가 → JPQL로 대체
        List<Product> result = entityManager
                .createQuery("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id = :id", Product.class)
                .setParameter("id", id)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * 상품 저장 (신규 생성)
     *
     * EntityManager.persist():
     *   - Transient 상태의 엔티티를 영속성 컨텍스트에 등록합니다.
     *   - 트랜잭션 커밋 시점에 INSERT SQL이 실행됩니다.
     *   - DB의 AUTO_INCREMENT 값이 product.id에 자동 설정됩니다.
     *
     * 이 메서드는 Service의 @Transactional 범위 안에서 호출됩니다.
     */
    public Product save(Product product) {
        entityManager.persist(product);
        return product;
    }

    /**
     * 상품 수정 (기존 데이터 업데이트)
     *
     * EntityManager.merge():
     *   - Detached 상태의 엔티티를 다시 영속 상태로 만들고 변경사항을 반영합니다.
     *   - 트랜잭션 커밋 시점에 UPDATE SQL이 실행됩니다.
     *   - merge()는 새로운 Managed 엔티티를 반환합니다(파라미터 객체와 다름).
     */
    public Product update(Product product) {
        return entityManager.merge(product);
    }

    /**
     * 상품 삭제
     *
     * EntityManager.remove():
     *   - Managed 상태의 엔티티를 Removed 상태로 변경합니다.
     *   - 트랜잭션 커밋 시점에 DELETE SQL이 실행됩니다.
     *   - 주의: Detached 엔티티는 먼저 merge()로 Managed 상태로 변환 필요
     */
    public void delete(Long id) {
        Product product = entityManager.find(Product.class, id);
        if (product != null) {
            entityManager.remove(product);
        }
    }
}
