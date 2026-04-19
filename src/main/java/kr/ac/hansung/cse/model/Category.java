package kr.ac.hansung.cse.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category")
@Getter
@Setter
@ToString(exclude = "products")          // products 제외: 양방향 무한순환 방지
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 요구 기본 생성자 (외부 직접 생성 방지)
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // ── ② 양방향 선언 시 추가 (처음에는 없어도 됨) ───────────────────
    @OneToMany(mappedBy = "category",         // Product.java의 category 필드명
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<Product> products = new ArrayList<>();

    // 편의 메서드: 양쪽 참조를 한 번에 설정
    public void addProduct(Product product) {
        products.add(product);
        product.setCategory(this);            // Owning Side(FK) 설정!
    }

    public Category(String name) { this.name = name; }
}
