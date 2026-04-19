package kr.ac.hansung.cse.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_detail")
@Getter
@Setter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;                         // Long 사용 (int 아님)

    @Column(name = "manufacturer", length = 200)
    private String manufacturer;

    @Column(name = "warranty", length = 100)
    private String warranty;

    @Lob
    @Column(name = "spec")
    private String spec;

    public ProductDetail(String manufacturer, String warranty, String spec) {
        this.manufacturer = manufacturer;
        this.warranty = warranty;
        this.spec = spec;
    }
}
