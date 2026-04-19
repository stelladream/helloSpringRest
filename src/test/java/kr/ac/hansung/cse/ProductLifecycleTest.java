package kr.ac.hansung.cse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.ac.hansung.cse.config.DbConfig;
import kr.ac.hansung.cse.model.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DbConfig.class)
public class ProductLifecycleTest {

    @PersistenceContext
    private EntityManager em;

    @Test
    public void testLifecycle() {

        // ① NEW (Transient) — not yet associated with EntityManager

        Product product = new Product("Laptop", null, new BigDecimal("1299.00"), "노트북");

        // ② MANAGED (Persistent) — after persist()
        em.persist(product);
        assertNotNull(product.getId(), "id should be set after persist");

        // 1st-level cache: same object returned (no extra SQL)
        Product cached = em.find(Product.class, product.getId());
        assertSame(product, cached);   // same reference from cache


        // ③ Dirty Checking — just set the field; flush detects the change
        cached.setName("Updated Laptop");
        em.flush();                    // force UPDATE SQL (within tx)

        Product updated = em.find(Product.class, product.getId());
        assertEquals("Updated Laptop", updated.getName());

        // ④ DETACHED — EntityManager stops tracking this entity
        em.detach(updated);
        updated.setName("Will NOT be saved");  // Detached: change ignored by JPA
        em.flush();

        // Verify: DB still has "Updated Laptop"
        Product check = em.find(Product.class, product.getId());
        assertEquals("Updated Laptop", check.getName(),
                "Detached entity changes must NOT reach the DB");
    }
}



