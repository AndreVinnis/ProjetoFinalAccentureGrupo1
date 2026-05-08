package br.accenture.ProjetoFinalAccentureGrupo1.notification.controller;

import br.accenture.ProjetoFinalAccentureGrupo1.notification.domain.EmailLog;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.dto.EmailLogResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.enums.EmailStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.repository.EmailLogRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/notifications/emails")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ECOMMERCE_ADMIN', 'BANKING_ADMIN')")
// Autor: André Vinícius Barros Macambira
public class AdminEmailController {

    private final EmailLogRepository emailLogRepository;


    // Aceita filtro opcional por status ou tipo.
    @GetMapping
    public Page<EmailLogResponse> list(
            @RequestParam(required = false) EmailStatus status,
            @RequestParam(required = false) String type,
            Pageable pageable
    ) {
        Page<EmailLog> page;
        if (status != null) {
            page = emailLogRepository.findByStatus(status, pageable);
        } else if (type != null) {
            page = emailLogRepository.findByType(type, pageable);
        } else {
            page = emailLogRepository.findAll(pageable);
        }
        return page.map(EmailLogResponse::from);
    }

    @GetMapping("/{id}")
    public EmailLogResponse getById(@PathVariable Long id) {
        return emailLogRepository.findById(id)
                .map(EmailLogResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "EmailLog não encontrado: " + id
                ));
    }
}
