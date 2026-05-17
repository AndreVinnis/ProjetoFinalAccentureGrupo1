package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.JwtAuthenticationFilter;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.SecurityConfig;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.AddToCartRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CartItemResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CartResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.UpdateCartItemRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.CartStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CartEmptyException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CartItemNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CartNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CartController - Web Layer Tests")
class CartControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CartService cartService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String EMAIL = "ana@email.com";

    private CartResponse cartResponse;

    @BeforeEach
    void setUp() {
        cartResponse = new CartResponse(
                50L,
                List.of(new CartItemResponse(100L, "Smartphone", 2,
                        new BigDecimal("1000.00"), new BigDecimal("2000.00"))),
                new BigDecimal("2000.00"), CartStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("GET /ecommerce/cart/me retorna 200 com o carrinho do usuário")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void getMyCart_returns200() throws Exception {
        when(cartService.getMyCart(EMAIL)).thenReturn(cartResponse);

        mockMvc.perform(get("/ecommerce/cart/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(50))
                .andExpect(jsonPath("$.items[0].productId").value(100))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.subtotal").value(2000.00));
    }

    @Test
    @DisplayName("GET /ecommerce/cart/me retorna 404 quando carrinho não existe")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void getMyCart_returns404WhenMissing() throws Exception {
        when(cartService.getMyCart(EMAIL)).thenThrow(new CartNotFoundException());

        mockMvc.perform(get("/ecommerce/cart/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /ecommerce/cart/close/me retorna 200")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void closeMyCart_returns200() throws Exception {
        when(cartService.closeCart(EMAIL)).thenReturn(cartResponse);

        mockMvc.perform(patch("/ecommerce/cart/close/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(50));
    }

    @Test
    @DisplayName("PATCH /ecommerce/cart/close/me retorna 422 quando carrinho está vazio")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void closeMyCart_returns422WhenEmpty() throws Exception {
        when(cartService.closeCart(EMAIL)).thenThrow(new CartEmptyException());

        mockMvc.perform(patch("/ecommerce/cart/close/me"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PATCH /ecommerce/cart/open/me retorna 200")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void openMyCart_returns200() throws Exception {
        when(cartService.openCart(EMAIL)).thenReturn(cartResponse);

        mockMvc.perform(patch("/ecommerce/cart/open/me"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /ecommerce/cart/me/items adiciona item e retorna 200")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void addItem_returns200() throws Exception {
        AddToCartRequest request = new AddToCartRequest(100L, 2);
        when(cartService.addItem(eq(EMAIL), any(AddToCartRequest.class))).thenReturn(cartResponse);

        mockMvc.perform(post("/ecommerce/cart/me/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(2000.00));

        verify(cartService).addItem(eq(EMAIL), any(AddToCartRequest.class));
    }

    @Test
    @DisplayName("POST /ecommerce/cart/me/items retorna 400 quando payload é inválido")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void addItem_returns400WhenInvalid() throws Exception {
        String invalid = "{ \"productId\": null, \"quantity\": 0 }";

        mockMvc.perform(post("/ecommerce/cart/me/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /ecommerce/cart/me/items/{productId} atualiza item e retorna 200")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void updateItem_returns200() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest(5);
        when(cartService.updateItemQuantity(eq(EMAIL), eq(100L), any(UpdateCartItemRequest.class)))
                .thenReturn(cartResponse);

        mockMvc.perform(put("/ecommerce/cart/me/items/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /ecommerce/cart/me/items/{productId} retorna 404 quando item não está no carrinho")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void updateItem_returns404WhenMissing() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest(3);
        when(cartService.updateItemQuantity(eq(EMAIL), eq(100L), any(UpdateCartItemRequest.class)))
                .thenThrow(new CartItemNotFoundException(100L));

        mockMvc.perform(put("/ecommerce/cart/me/items/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /ecommerce/cart/me/items/{productId} remove item e retorna 200")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void removeItem_returns200() throws Exception {
        when(cartService.removeItem(EMAIL, 100L)).thenReturn(cartResponse);

        mockMvc.perform(delete("/ecommerce/cart/me/items/100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /ecommerce/cart/me limpa o carrinho e retorna 200")
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void clearCart_returns200() throws Exception {
        when(cartService.clearMyCart(EMAIL)).thenReturn(cartResponse);

        mockMvc.perform(delete("/ecommerce/cart/me"))
                .andExpect(status().isOk());
    }
}
