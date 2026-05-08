package br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "account_holders")
@Getter
@Setter
public class AccountHolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String document;

    /** Referência ao {@code users.id} após cadastro; opcional para titulares não ligados a usuário. */
    @Column(name = "user_id", unique = true)
    private Long userId;
}
