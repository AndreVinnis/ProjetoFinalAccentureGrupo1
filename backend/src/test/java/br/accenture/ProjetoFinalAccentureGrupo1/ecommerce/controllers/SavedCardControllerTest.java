package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.JwtAuthenticationFilter;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.RegisterSavedCardRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.SavedCardResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.SavedCardAlreadyExistsException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.SavedCardNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.SavedCardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SavedCardController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SavedCardController - Web Layer Tests")
class SavedCardControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private SavedCardService savedCardService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String EMAIL = "ana@email.com";

    private SavedCardResponse cardResponse(Long id) {
        return new SavedCardResponse(id, "1234", "ANA SILVA", Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    @DisplayName("POST /ecommerce/cards registra cartão e retorna 201")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void registerCard_returns201() throws Exception {
        RegisterSavedCardRequest request = new RegisterSavedCardRequest(
                "4111111111111234", "123", 12, 2030, "ANA SILVA"
        );
        when(savedCardService.registerCard(eq(EMAIL), any(RegisterSavedCardRequest.class)))
                .thenReturn(cardResponse(1L));

        mockMvc.perform(post("/ecommerce/cards")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.last4Digits").value("1234"));
    }

    @Test
    @DisplayName("POST /ecommerce/cards retorna 409 quando cartão já está salvo")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void registerCard_returns409WhenAlreadyExists() throws Exception {
        RegisterSavedCardRequest request = new RegisterSavedCardRequest(
                "4111111111111234", "123", 12, 2030, "ANA SILVA"
        );
        when(savedCardService.registerCard(eq(EMAIL), any(RegisterSavedCardRequest.class)))
                .thenThrow(new SavedCardAlreadyExistsException());

        mockMvc.perform(post("/ecommerce/cards")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /ecommerce/cards retorna 400 quando payload inválido")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void registerCard_returns400WhenInvalid() throws Exception {
        String invalid = """
                {
                    "cardNumber": "1234",
                    "cvv": "abc",
                    "expirationMonth": 13,
                    "expirationYear": 1999,
                    "holderName": "ANA"
                }
                """;

        mockMvc.perform(post("/ecommerce/cards")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /ecommerce/cards retorna 200 com cartões do usuário")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void listMyCards_returns200() throws Exception {
        when(savedCardService.listMyCards(EMAIL)).thenReturn(List.of(
                cardResponse(1L), cardResponse(2L)
        ));

        mockMvc.perform(get("/ecommerce/cards").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    @DisplayName("DELETE /ecommerce/cards/{id} retorna 204")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/ecommerce/cards/1").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isNoContent());

        verify(savedCardService).delete(EMAIL, 1L);
    }

    @Test
    @DisplayName("DELETE /ecommerce/cards/{id} retorna 404 quando cartão não existe")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void delete_returns404WhenMissing() throws Exception {
        doThrow(new SavedCardNotFoundException(99L)).when(savedCardService).delete(EMAIL, 99L);

        mockMvc.perform(delete("/ecommerce/cards/99").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isNotFound());
    }
}
