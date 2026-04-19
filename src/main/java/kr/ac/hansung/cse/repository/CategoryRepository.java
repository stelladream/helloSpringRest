package kr.ac.hansung.cse.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.ac.hansung.cse.model.Category;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepository {

    @PersistenceContext
    private EntityManager em;

    public Category save(Category category) {
        em.persist(category);
        return category;
    }

    public Optional<Category> findById(Long id) {
        return Optional.ofNullable(em.find(Category.class, id));
    }

    public List<Category> findAll() {
        return em.createQuery("SELECT c FROM Category c ORDER BY c.id", Category.class)
                .getResultList();
    }

    // 이름으로 카테고리 조회 (폼에서 선택한 카테고리명 → Category 엔티티 변환 시 사용)
    public Optional<Category> findByName(String name) {
        List<Category> result = em.createQuery(
                        "SELECT c FROM Category c WHERE c.name = :name", Category.class)
                .setParameter("name", name)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    // JOIN FETCH: N+1 문제 방지 (Category + Products 한 번에 로드)
    public Optional<Category> findByIdWithProducts(Long id) {
        List<Category> result = em.createQuery(
                        "SELECT DISTINCT c FROM Category c JOIN FETCH c.products WHERE c.id = :id",
                        Category.class)
                .setParameter("id", id)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
}

