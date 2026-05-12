package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Category;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CategoryRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CategoryResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CategoryAlreadyExistsException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CategoryNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public CategoryResponse findResponseById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        assertNameAvailable(request.name());
        Category category = Category.builder()
                .name(request.name().trim())
                .description(normalizeDescription(request.description()))
                .build();
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findById(id);
        categoryRepository.findByNameIgnoreCase(request.name().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new CategoryAlreadyExistsException(request.name());
                });

        category.setName(request.name().trim());
        category.setDescription(normalizeDescription(request.description()));
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findById(id);
        categoryRepository.delete(category);
    }

    private void assertNameAvailable(String name) {
        if (categoryRepository.existsByNameIgnoreCase(name.trim())) {
            throw new CategoryAlreadyExistsException(name);
        }
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
