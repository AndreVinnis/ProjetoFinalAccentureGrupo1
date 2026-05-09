package br.accenture.ProjetoFinalAccentureGrupo1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
//@EnableJpaRepositories(basePackages = {"br.accenture.ProjetoFinalAccentureGrupo1", "com.grupo.projeto.banking"})
//@EntityScan(basePackages = {"br.accenture.ProjetoFinalAccentureGrupo1", "com.grupo.projeto.banking"})
//@ComponentScan(basePackages = {"br.accenture.ProjetoFinalAccentureGrupo1", "com.grupo.projeto.banking"})
public class ProjetoFinalAccentureGrupo1Application {

	public static void main(String[] args) {
		SpringApplication.run(ProjetoFinalAccentureGrupo1Application.class, args);
	}

}