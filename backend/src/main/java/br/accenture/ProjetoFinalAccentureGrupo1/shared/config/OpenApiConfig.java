package br.accenture.ProjetoFinalAccentureGrupo1.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;
// Autor: André Vinícius Barros Macambira
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Projeto Final Accenture — Grupo 1",
                version = "0.0.1",
                description = """
            API integrando módulos de e-commerce e banking.
            
            Para acessar endpoints protegidos:
            1. Faça login em POST /auth/login
            2. Copie o token retornado
            3. Clique em "Authorize" e cole o token (sem o prefixo "Bearer ")
            """,
                contact = @Contact(name = "Grupo 1 — Academia Java Accenture")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
