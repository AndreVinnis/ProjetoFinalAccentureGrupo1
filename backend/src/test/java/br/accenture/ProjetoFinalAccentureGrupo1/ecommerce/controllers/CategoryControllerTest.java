package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.JwtAuthenticationFilter;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CategoryResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CategoryNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CategoryController - Web Layer Tests")
class CategoryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CategoryService categoryService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /ecommerce/categories retorna 200 com lista de categorias")
    void list_returns200() throws Exception {
        when(categoryService.list()).thenReturn(List.of(
                new CategoryResponse(1L, "Eletrônicos", "Produtos eletrônicos"),
                new CategoryResponse(2L, "Roupas", "Vestuário")
        ));

        mockMvc.perform(get("/ecommerce/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Eletrônicos"))
                .andExpect(jsonPath("$[1].name").value("Roupas"));
    }

    @Test
    @DisplayName("GET /ecommerce/categories/{id} retorna 200 com a categoria")
    void findByName_returns200() throws Exception {
        when(categoryService.findByName("Eletronicos"))
                .thenReturn(new CategoryResponse(1L, "Eletronicos", "Produtos eletronicos"));

        mockMvc.perform(get("/ecommerce/categories/Eletronicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Eletronicos"));
    }

    @Test
    @DisplayName("GET /ecommerce/categories/{name} retorna 404 quando categoria não existe")
    void findByName_returns404WhenMissing() throws Exception {
        when(categoryService.findByName("Inexistente")).thenThrow(new CategoryNotFoundException(0L));

        mockMvc.perform(get("/ecommerce/categories/Inexistente"))
                .andExpect(status().isNotFound());
    }
}
