package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Product;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.ProductRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ecommerce/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ECOMMERCE_ADMIN')") // Proteção da rota
public class AdminProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody ProductRequest request) {
        Product product = productService.createProduct(
                request.name(),
                request.description(),
                request.price(),
                request.initialStock(),
                request.categoryId()
        );
        return ResponseEntity.ok(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
        Product product = productService.updateProduct(
                id,
                request.name(),
                request.description(),
                request.price(),
                request.categoryId()
        );
        return ResponseEntity.ok(product);
    }

    @PostMapping("/{id}/restock")
    public ResponseEntity<Product> restock(@PathVariable Long id, @RequestParam int quantity) {
        return ResponseEntity.ok(productService.restock(id, quantity));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateProduct(@PathVariable Long id) {
        productService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}