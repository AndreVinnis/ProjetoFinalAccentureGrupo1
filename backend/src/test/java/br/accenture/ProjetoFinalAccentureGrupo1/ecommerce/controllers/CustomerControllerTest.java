package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.JwtAuthenticationFilter;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CustomerResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.UpdateCustomerRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.CustomerTier;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CustomerNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.CustomerService;
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

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CustomerController - Web Layer Tests")
class CustomerControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CustomerService customerService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String EMAIL = "ana@email.com";

    private CustomerResponse customerResponse() {
        return new CustomerResponse(
                1L, 10L, 3, CustomerTier.BRONZE,
                "Rua A, 123", "11999999999", Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    @Test
    @DisplayName("GET /customers/me retorna 200 com o cliente autenticado")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyCustomer_returns200() throws Exception {
        when(customerService.findMyCustomer(EMAIL)).thenReturn(customerResponse());

        mockMvc.perform(get("/customers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.tier").value("BRONZE"))
                .andExpect(jsonPath("$.shippingAddress").value("Rua A, 123"));
    }

    @Test
    @DisplayName("GET /customers/me retorna 404 quando customer não existe")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyCustomer_returns404WhenMissing() throws Exception {
        when(customerService.findMyCustomer(EMAIL))
                .thenThrow(new CustomerNotFoundException(1L));

        mockMvc.perform(get("/customers/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /customers/me atualiza endereço e telefone")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void updateMyCustomer_returns200() throws Exception {
        UpdateCustomerRequest request = new UpdateCustomerRequest("Rua Nova, 999", "11988888888");
        CustomerResponse updated = new CustomerResponse(
                1L, 10L, 3, CustomerTier.BRONZE,
                "Rua Nova, 999", "11988888888", Instant.parse("2026-01-01T00:00:00Z")
        );
        when(customerService.updateMyCustomer(EMAIL, "Rua Nova, 999", "11988888888"))
                .thenReturn(updated);

        mockMvc.perform(put("/customers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingAddress").value("Rua Nova, 999"))
                .andExpect(jsonPath("$.phone").value("11988888888"));
    }

    @Test
    @DisplayName("PUT /customers/me retorna 400 quando payload é inválido (campos em branco)")
    void updateMyCustomer_returns400WhenInvalid() throws Exception {
        String invalid = "{ \"shippingAddress\": \"\", \"phone\": \"\" }";

        mockMvc.perform(put("/customers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }
}
