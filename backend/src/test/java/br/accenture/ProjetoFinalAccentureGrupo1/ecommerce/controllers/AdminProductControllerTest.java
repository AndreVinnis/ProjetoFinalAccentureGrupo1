package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.JwtAuthenticationFilter;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Category;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Product;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.AdminProductResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.ProductRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminProductController - Web Layer Tests")
class AdminProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ProductService productService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(1L).name("Eletrônicos").build();
        product = Product.builder()
                .id(100L).name("Smartphone").description("Top")
                .price(new BigDecimal("1500.00"))
                .totalStock(10).reservedStock(0)
                .active(true).category(category).build();
    }

    @Test
    @DisplayName("GET /ecommerce/admin/products lista produtos e retorna 200")
    void listProducts_returns200() throws Exception {
        AdminProductResponse response = new AdminProductResponse(
                100L,
                "Smartphone",
                "Top",
                new BigDecimal("1500.00"),
                10,
                0,
                10,
                true,
                Instant.parse("2026-01-01T00:00:00Z"),
                1L,
                "Eletrônicos"
        );

        when(productService.listProductsForAdmin(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/ecommerce/admin/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].totalStock").value(10))
                .andExpect(jsonPath("$.content[0].availableStock").value(10))
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    @DisplayName("POST /ecommerce/admin/products cria produto e retorna 200")
    void createProduct_returns200() throws Exception {
        ProductRequest request = new ProductRequest("Smartphone", "Top",
                new BigDecimal("1500.00"), 10, "Eletrônicos");
        when(productService.createProduct("Smartphone", "Top",
                new BigDecimal("1500.00"), 10, "Eletrônicos")).thenReturn(product);

        mockMvc.perform(post("/ecommerce/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.name").value("Smartphone"));
    }

    @Test
    @DisplayName("PUT /ecommerce/admin/products/{id} atualiza e retorna 200")
    void updateProduct_returns200() throws Exception {
        ProductRequest request = new ProductRequest("Smartphone Pro", "Versão nova",
                new BigDecimal("1800.00"), 0, "Eletrônicos");
        product.setName("Smartphone Pro");
        when(productService.updateProduct(100L, "Smartphone Pro", "Versão nova",
                new BigDecimal("1800.00"), "Eletrônicos")).thenReturn(product);

        mockMvc.perform(put("/ecommerce/admin/products/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Smartphone Pro"));
    }

    @Test
    @DisplayName("POST /ecommerce/admin/products/{id}/restock retorna 200")
    void restock_returns200() throws Exception {
        product.setTotalStock(15);
        when(productService.restock(100L, 5)).thenReturn(product);

        mockMvc.perform(post("/ecommerce/admin/products/100/restock")
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStock").value(15));
    }

    @Test
    @DisplayName("POST /ecommerce/admin/products/{id}/restock propaga IllegalArgumentException quando quantity inválida")
    void restock_propagatesWhenInvalid() throws Exception {
        when(productService.restock(100L, 0))
                .thenThrow(new IllegalArgumentException("A quantidade de reabastecimento deve ser maior que zero."));

        assertThatThrownBy(() -> mockMvc.perform(post("/ecommerce/admin/products/100/restock")
                        .param("quantity", "0")))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A quantidade de reabastecimento deve ser maior que zero.");
    }

    @Test
    @DisplayName("POST /ecommerce/admin/products/{id}/deactivate retorna 204")
    void deactivateProduct_returns204() throws Exception {
        mockMvc.perform(post("/ecommerce/admin/products/100/deactivate"))
                .andExpect(status().isNoContent());

        verify(productService).deactivate(100L);
    }

    @Test
    @DisplayName("POST /ecommerce/admin/products/{id}/activate retorna 204")
    void activateProduct_returns204() throws Exception {
        mockMvc.perform(post("/ecommerce/admin/products/100/activate"))
                .andExpect(status().isNoContent());

        verify(productService).activate(100L);
    }
}
