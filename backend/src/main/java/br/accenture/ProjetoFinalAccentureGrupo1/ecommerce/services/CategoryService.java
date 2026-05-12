package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Category;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {
    
    // Método temporário só para o ProductService não dar erro
    public Category findById(Long id) {
        return new Category(); 
    }
}