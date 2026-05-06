package br.accenture.ProjetoFinalAccentureGrupo1.auth.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);
}
