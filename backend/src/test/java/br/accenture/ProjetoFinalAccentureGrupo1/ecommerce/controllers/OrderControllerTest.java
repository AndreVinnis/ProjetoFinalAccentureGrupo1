package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.JwtAuthenticationFilter;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CheckoutCardRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.InstallmentOptionResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.OrderResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.OrderStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.PaymentMethod;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.OrderNotFound;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.OrderShippedException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.OrderService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderController - Web Layer Tests")
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OrderService orderService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String EMAIL = "ana@email.com";

    private OrderResponse orderResponse(Long id, OrderStatus status, PaymentMethod method) {
        return new OrderResponse(
                id, status, method,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"),
                List.of(),
                Instant.parse("2026-05-01T00:00:00Z"), null, null, null, null
        );
    }

    @Test
    @DisplayName("GET /ecommerce/orders retorna 200 com pedidos do usuário")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyOrders_returns200() throws Exception {
        when(orderService.findMyOrders(EMAIL))
                .thenReturn(List.of(orderResponse(1L, OrderStatus.PAID, PaymentMethod.PIX)));

        mockMvc.perform(get("/ecommerce/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(1))
                .andExpect(jsonPath("$[0].status").value("PAID"));
    }

    @Test
    @DisplayName("GET /ecommerce/orders/{id} retorna 200 com pedido específico")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyOrder_returns200() throws Exception {
        when(orderService.findMyOrder(42L, EMAIL))
                .thenReturn(orderResponse(42L, OrderStatus.SHIPPED, PaymentMethod.CREDIT_CARD));

        mockMvc.perform(get("/ecommerce/orders/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(42))
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    @DisplayName("GET /ecommerce/orders/{id} retorna 404 quando pedido não existe")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyOrder_returns404WhenMissing() throws Exception {
        when(orderService.findMyOrder(99L, EMAIL)).thenThrow(new OrderNotFound(99L));

        mockMvc.perform(get("/ecommerce/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /ecommerce/orders/checkout/pix retorna 200 com chave Pix")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void checkoutPix_returns200() throws Exception {
        when(orderService.checkoutPix(EMAIL)).thenReturn("CHAVE-PIX-123");

        mockMvc.perform(post("/ecommerce/orders/checkout/pix"))
                .andExpect(status().isOk())
                .andExpect(content().string("CHAVE-PIX-123"));
    }

    @Test
    @DisplayName("POST /ecommerce/orders/checkout/card retorna 200 com pedido criado")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void checkoutCard_returns200() throws Exception {
        CheckoutCardRequest request = new CheckoutCardRequest(5L, "123", 3);
        when(orderService.checkoutCard(EMAIL, 5L, "123", 3))
                .thenReturn(orderResponse(7L, OrderStatus.PAID, PaymentMethod.CREDIT_CARD));

        mockMvc.perform(post("/ecommerce/orders/checkout/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(7))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"));
    }

    @Test
    @DisplayName("GET /ecommerce/orders/checkout/card/installments retorna opcoes")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void listCardInstallments_returns200() throws Exception {
        when(orderService.listCardInstallments(EMAIL))
                .thenReturn(List.of(new InstallmentOptionResponse(
                        3,
                        new BigDecimal("33.33"),
                        new BigDecimal("100.00"),
                        "3x de R$ 33.33 sem juros"
                )));

        mockMvc.perform(get("/ecommerce/orders/checkout/card/installments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].installments").value(3))
                .andExpect(jsonPath("$[0].totalAmount").value(100.00));
    }

    @Test
    @DisplayName("POST /ecommerce/orders/checkout/card retorna 400 quando payload inválido")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void checkoutCard_returns400WhenInvalid() throws Exception {
        String invalid = "{ \"savedCardId\": null, \"cvv\": \"\" }";

        mockMvc.perform(post("/ecommerce/orders/checkout/card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /ecommerce/orders/{id}/cancel retorna 204")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void cancel_returns204() throws Exception {
        mockMvc.perform(post("/ecommerce/orders/42/cancel"))
                .andExpect(status().isNoContent());

        verify(orderService).cancel(42L, EMAIL);
    }

    @Test
    @DisplayName("POST /ecommerce/orders/{id}/cancel retorna 422 quando pedido já foi enviado")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void cancel_returns422WhenShipped() throws Exception {
        doThrow(new OrderShippedException(42L))
                .when(orderService).cancel(42L, EMAIL);

        mockMvc.perform(post("/ecommerce/orders/42/cancel"))
                .andExpect(status().isUnprocessableEntity());
    }
}
