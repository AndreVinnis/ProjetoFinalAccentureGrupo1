package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
//Autor: Cainã Moura Araújo
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
