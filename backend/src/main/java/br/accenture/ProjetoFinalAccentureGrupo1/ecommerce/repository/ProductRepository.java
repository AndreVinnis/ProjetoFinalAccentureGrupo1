package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Page<Product> findByActiveTrue(Pageable pageable);
    
    Page<Product> findByActiveTrueAndCategoryId(Long categoryId, Pageable pageable);

    // Novos métodos com filtro de preço
    Page<Product> findByActiveTrueAndPriceLessThanEqual(BigDecimal maxPrice, Pageable pageable);
    
    Page<Product> findByActiveTrueAndCategoryIdAndPriceLessThanEqual(Long categoryId, BigDecimal maxPrice, Pageable pageable);
}