package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_discounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// André Vinícius Barros Macambira
public class OrderDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String ruleName;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
}