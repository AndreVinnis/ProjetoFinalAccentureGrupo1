package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Category;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CategoryRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CategoryResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CategoryAlreadyExistsException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CategoryNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService - Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Eletrônicos")
                .description("Produtos eletrônicos em geral")
                .build();
    }

    @Test
    @DisplayName("findByName retorna CategoryResponse quando a categoria existe")
    void findByName_success() {
        when(categoryRepository.findByNameIgnoreCase("Eletrônicos")).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.findByName("Eletrônicos");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Eletrônicos");
        assertThat(response.description()).isEqualTo("Produtos eletrônicos em geral");
    }

    @Test
    @DisplayName("findByName lança CategoryNotFoundException quando a categoria não existe")
    void findByName_throwsWhenMissing() {
        when(categoryRepository.findByNameIgnoreCase("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findByName("X"))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("findEntityByName retorna a entidade Category quando existe")
    void findEntityByName_success() {
        when(categoryRepository.findByNameIgnoreCase("Eletrônicos")).thenReturn(Optional.of(category));

        Category result = categoryService.findEntityByName("Eletrônicos");

        assertThat(result).isSameAs(category);
    }

    @Test
    @DisplayName("findEntityByName lança CategoryNotFoundException quando não existe")
    void findEntityByName_throwsWhenMissing() {
        when(categoryRepository.findByNameIgnoreCase("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findEntityByName("X"))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("createCategory salva a categoria a partir do request")
    void createCategory_success() {
        CategoryRequest request = new CategoryRequest("Livros", "Livros e revistas");

        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        CategoryResponse response = categoryService.createCategory(request);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        Category saved = captor.getValue();

        assertThat(saved.getName()).isEqualTo("Livros");
        assertThat(saved.getDescription()).isEqualTo("Livros e revistas");
        assertThat(response.name()).isEqualTo("Livros");
    }

    @Test
    @DisplayName("list retorna todas as categorias mapeadas para CategoryResponse")
    void list_success() {
        Category other = Category.builder().id(2L).name("Roupas").description("Vestuário").build();
        when(categoryRepository.findAll()).thenReturn(List.of(category, other));

        List<CategoryResponse> result = categoryService.list();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Eletrônicos");
        assertThat(result.get(1).name()).isEqualTo("Roupas");
    }

    @Test
    @DisplayName("findById retorna a entidade quando existe")
    void findById_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Category result = categoryService.findById(1L);

        assertThat(result).isSameAs(category);
    }

    @Test
    @DisplayName("findById lança CategoryNotFoundException quando não existe")
    void findById_throwsWhenMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(99L))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("findResponseById retorna o CategoryResponse correspondente")
    void findResponseById_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.findResponseById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Eletrônicos");
    }

    @Test
    @DisplayName("create salva a categoria normalizando o nome e a descrição")
    void create_success() {
        CategoryRequest request = new CategoryRequest("  Casa  ", "  Para o lar  ");

        when(categoryRepository.existsByNameIgnoreCase("Casa")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });

        CategoryResponse response = categoryService.create(request);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        Category saved = captor.getValue();

        assertThat(saved.getName()).isEqualTo("Casa");
        assertThat(saved.getDescription()).isEqualTo("Para o lar");
        assertThat(response.id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("create normaliza descrição em branco para null")
    void create_blankDescriptionBecomesNull() {
        CategoryRequest request = new CategoryRequest("Casa", "   ");

        when(categoryRepository.existsByNameIgnoreCase("Casa")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.create(request);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isNull();
    }

    @Test
    @DisplayName("create lança CategoryAlreadyExistsException quando o nome já existe")
    void create_throwsWhenNameAlreadyExists() {
        CategoryRequest request = new CategoryRequest("Eletrônicos", "qualquer");
        when(categoryRepository.existsByNameIgnoreCase("Eletrônicos")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(CategoryAlreadyExistsException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update altera a categoria existente")
    void update_success() {
        CategoryRequest request = new CategoryRequest("Eletrodomésticos", "Geladeiras e fogões");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findByNameIgnoreCase("Eletrodomésticos")).thenReturn(Optional.empty());
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponse response = categoryService.update(1L, request);

        assertThat(category.getName()).isEqualTo("Eletrodomésticos");
        assertThat(category.getDescription()).isEqualTo("Geladeiras e fogões");
        assertThat(response.name()).isEqualTo("Eletrodomésticos");
    }

    @Test
    @DisplayName("update permite manter o mesmo nome (mesmo id)")
    void update_sameNameSameId() {
        CategoryRequest request = new CategoryRequest("Eletrônicos", "Atualizada");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findByNameIgnoreCase("Eletrônicos")).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        categoryService.update(1L, request);

        assertThat(category.getDescription()).isEqualTo("Atualizada");
    }

    @Test
    @DisplayName("update lança CategoryAlreadyExistsException quando o nome pertence a outra categoria")
    void update_throwsWhenNameBelongsToAnother() {
        CategoryRequest request = new CategoryRequest("Roupas", "qualquer");
        Category other = Category.builder().id(2L).name("Roupas").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findByNameIgnoreCase("Roupas")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> categoryService.update(1L, request))
                .isInstanceOf(CategoryAlreadyExistsException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update lança CategoryNotFoundException quando id não existe")
    void update_throwsWhenMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(99L, new CategoryRequest("X", "Y")))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("delete remove a categoria existente")
    void delete_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.delete(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("delete lança CategoryNotFoundException quando o id não existe")
    void delete_throwsWhenMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(categoryRepository, never()).delete(any());
    }
}
