package kr.ac.hansung.cse.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tag")
@Getter
@Setter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    public Tag(String name) { this.name = name; }
}

