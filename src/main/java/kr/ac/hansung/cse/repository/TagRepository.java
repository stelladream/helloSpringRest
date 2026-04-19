package kr.ac.hansung.cse.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.ac.hansung.cse.model.Tag;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TagRepository {

    @PersistenceContext
    private EntityManager em;

    public Tag save(Tag tag) { em.persist(tag); return tag; }

    public Optional<Tag> findById(Long id) {
        return Optional.ofNullable(em.find(Tag.class, id));
    }

    public Optional<Tag> findByName(String name) {
        List<Tag> result = em.createQuery(
                        "SELECT t FROM Tag t WHERE t.name = :name", Tag.class)
                .setParameter("name", name).getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
}

