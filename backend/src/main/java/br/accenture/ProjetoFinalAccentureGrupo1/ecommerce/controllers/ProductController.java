package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Product;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.ProductService;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/ecommerce/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // Lista os produtos ativos (vitrine). Pode filtrar por categoria se passar o ID na URL
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ECOMMERCE_ADMIN')")
    public ResponseEntity<Page<ProductResponse>> listProducts(
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        return ResponseEntity.ok(productService.listActiveProducts(categoryName, maxPrice, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ECOMMERCE_ADMIN')")
    public ResponseEntity<Product> getProductDetails(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }
}
