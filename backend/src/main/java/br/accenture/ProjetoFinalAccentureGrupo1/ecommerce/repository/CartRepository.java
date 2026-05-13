package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Cart;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByCustomer_Id(Long customerId);

    List<Cart> findByStatusAndReservedAtBefore(CartStatus status, Instant threshold);
}
