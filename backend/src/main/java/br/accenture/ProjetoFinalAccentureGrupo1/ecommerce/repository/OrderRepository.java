package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository;

import java.time.Instant;
import java.util.List;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Order;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Order> findByStatusAndPaidAtBefore(OrderStatus status, Instant threshold);

    List<Order> findByStatusAndShippedAtBefore(OrderStatus status, Instant threshold);

    List<Order> findByStatus(OrderStatus status);
}
