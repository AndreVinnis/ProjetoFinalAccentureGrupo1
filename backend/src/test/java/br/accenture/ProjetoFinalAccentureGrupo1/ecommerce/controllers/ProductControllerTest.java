package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.JwtAuthenticationFilter;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Category;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Product;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.ProductResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProductController - Web Layer Tests")
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ProductService productService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /ecommerce/products retorna 200 com Page de produtos")
    void listProducts_returns200() throws Exception {
        ProductResponse pr = new ProductResponse(
                100L, "Smartphone", "Top",
                new BigDecimal("1500.00"), 10, 1L, "Eletrônicos"
        );
        when(productService.listActiveProducts(isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(pr), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/ecommerce/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].name").value("Smartphone"))
                .andExpect(jsonPath("$.content[0].availableStock").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /ecommerce/products aplica filtros categoryName e maxPrice")
    void listProducts_appliesFilters() throws Exception {
        when(productService.listActiveProducts(eq("Eletrônicos"), eq(new BigDecimal("2000.00")), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/ecommerce/products")
                        .param("categoryName", "Eletrônicos")
                        .param("maxPrice", "2000.00"))
                .andExpect(status().isOk());

        verify(productService).listActiveProducts(eq("Eletrônicos"), eq(new BigDecimal("2000.00")), any());
    }

    @Test
    @DisplayName("GET /ecommerce/products/{id} retorna 200 com o produto")
    void getProductDetails_returns200() throws Exception {
        Category category = Category.builder().id(1L).name("Eletrônicos").build();
        Product product = Product.builder()
                .id(100L).name("Smartphone").description("Top")
                .price(new BigDecimal("1500.00"))
                .totalStock(10).reservedStock(0)
                .active(true).category(category).build();
        when(productService.findById(100L)).thenReturn(product);

        mockMvc.perform(get("/ecommerce/products/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.name").value("Smartphone"));
    }

    @Test
    @DisplayName("GET /ecommerce/products/{id} propaga RuntimeException quando produto não existe")
    void getProductDetails_propagatesWhenMissing() throws Exception {
        when(productService.findById(99L)).thenThrow(new RuntimeException("Produto não encontrado: 99"));

        assertThatThrownBy(() -> mockMvc.perform(get("/ecommerce/products/99")))
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Produto não encontrado");
    }
}
