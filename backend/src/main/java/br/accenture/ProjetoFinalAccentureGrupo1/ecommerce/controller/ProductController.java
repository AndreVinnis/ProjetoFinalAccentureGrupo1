package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.controller;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Product;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.service.ProductService;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/ecommerce/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // Lista os produtos ativos (vitrine). Pode filtrar por categoria se passar o ID na URL
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> listProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        return ResponseEntity.ok(productService.listActiveProducts(categoryId, maxPrice, pageable));
    }

    // Traz os detalhes de um único produto
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductDetails(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }
}