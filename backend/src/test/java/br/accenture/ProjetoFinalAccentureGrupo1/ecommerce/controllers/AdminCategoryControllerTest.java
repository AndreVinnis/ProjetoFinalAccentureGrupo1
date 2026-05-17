package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.JwtAuthenticationFilter;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CategoryRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CategoryResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CategoryAlreadyExistsException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CategoryNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminCategoryController - Web Layer Tests")
class AdminCategoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CategoryService categoryService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /ecommerce/admin/categories retorna 201 ao criar categoria válida")
    void create_returns201() throws Exception {
        CategoryRequest request = new CategoryRequest("Casa", "Para o lar");
        CategoryResponse response = new CategoryResponse(5L, "Casa", "Para o lar");
        when(categoryService.create(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/ecommerce/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Casa"));

        verify(categoryService).create(any(CategoryRequest.class));
    }

    @Test
    @DisplayName("POST /ecommerce/admin/categories retorna 400 quando nome em branco")
    void create_returns400WhenInvalid() throws Exception {
        String invalid = "{ \"name\": \"\", \"description\": \"x\" }";

        mockMvc.perform(post("/ecommerce/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /ecommerce/admin/categories retorna 409 quando categoria já existe")
    void create_returns409WhenConflict() throws Exception {
        CategoryRequest request = new CategoryRequest("Casa", "Para o lar");
        when(categoryService.create(any(CategoryRequest.class)))
                .thenThrow(new CategoryAlreadyExistsException("Casa"));

        mockMvc.perform(post("/ecommerce/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /ecommerce/admin/categories/{id} retorna 200 ao atualizar")
    void update_returns200() throws Exception {
        CategoryRequest request = new CategoryRequest("Eletrodomésticos", "Geladeiras");
        CategoryResponse response = new CategoryResponse(1L, "Eletrodomésticos", "Geladeiras");
        when(categoryService.update(eq(1L), any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/ecommerce/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Eletrodomésticos"));
    }

    @Test
    @DisplayName("PUT /ecommerce/admin/categories/{id} retorna 404 quando categoria não existe")
    void update_returns404WhenMissing() throws Exception {
        CategoryRequest request = new CategoryRequest("X", "Y");
        when(categoryService.update(eq(99L), any(CategoryRequest.class)))
                .thenThrow(new CategoryNotFoundException(99L));

        mockMvc.perform(put("/ecommerce/admin/categories/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /ecommerce/admin/categories/{id} retorna 204 ao deletar")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/ecommerce/admin/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(1L);
    }

    @Test
    @DisplayName("DELETE /ecommerce/admin/categories/{id} retorna 404 quando categoria não existe")
    void delete_returns404WhenMissing() throws Exception {
        doThrow(new CategoryNotFoundException(99L)).when(categoryService).delete(99L);

        mockMvc.perform(delete("/ecommerce/admin/categories/99"))
                .andExpect(status().isNotFound());
    }
}
