package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Category;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CategoryRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CategoryResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CartNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CategoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
// André Vinícius Barros Macambira
public class CategoryService {

    private final CategoryRepository categoryRepository;


    @Transactional(readOnly = true)
    public CategoryResponse findByName(String name) {
        Category category = categoryRepository.findByName(name).orElseThrow(
                () -> new CartNotFoundException()
        );
        return toCategoryResponse(category);
    }

    @Transactional(readOnly = true)
    public Category findEntityByName(String name) {
        return categoryRepository.findByName(name).orElseThrow(
                () -> new CartNotFoundException()
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllCategorys() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request){
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();
        categoryRepository.save(category);
        return toCategoryResponse(category);
    }

    private CategoryResponse toCategoryResponse(Category category){
        return new CategoryResponse(category.getName(), category.getDescription());
    }
}