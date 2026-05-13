package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.OrderDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDiscountRepository extends JpaRepository<OrderDiscount, Long> {
}
