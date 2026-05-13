package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Category;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Product;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.ProductResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class  ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;


    // MÉTODOS DE LEITURA (PÚBLICOS)

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listActiveProducts(String categoryName, BigDecimal maxPrice, Pageable pageable) {
        Page<Product> products;

        if (categoryName != null && maxPrice != null) {
            products = productRepository.findByActiveTrueAndCategoryNameAndPriceLessThanEqual(categoryName, maxPrice, pageable);
        } else if (categoryName != null) {
            products = productRepository.findByActiveTrueAndCategoryName(categoryName, pageable);
        } else if (maxPrice != null) {
            products = productRepository.findByActiveTrueAndPriceLessThanEqual(maxPrice, pageable);
        } else {
            products = productRepository.findByActiveTrue(pageable);
        }

        return products.map(this::toProductResponse);
    }

    // Método auxiliar privado no final do Service
    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getAvailableStock(), // Usando o método @Transient que criamos
                product.getCategory().getId(),
                product.getCategory().getName()
        );
    }

    // MÉTODOS ADMINISTRATIVOS

    @Transactional
    public Product createProduct(String name, String description, BigDecimal price, int initialStock, String categoryName) {
        Category category = categoryService.findEntityByName(categoryName);

        Product product = Product.builder()
                .name(name)
                .description(description)
                .price(price)
                .totalStock(initialStock)
                .reservedStock(0)
                .category(category)
                .active(true)
                .build();

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, String name, String description, BigDecimal price, String categoryName) {
        Product product = findById(id);
        Category category = categoryService.findEntityByName(categoryName);

        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);

        return productRepository.save(product);
    }

    @Transactional
    public void deactivate(Long id) {
        Product product = findById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public Product restock(Long id, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("A quantidade de reabastecimento deve ser maior que zero.");
        }
        Product product = findById(id);
        product.setTotalStock(product.getTotalStock() + quantity);
        return productRepository.save(product);
    }

    // GESTÃO DE ESTOQUE (CHAMADOS PELO CARRINHO/PEDIDO)

    @Transactional
    public void reserveStock(Long productId, int quantity) {
        Product product = findById(productId);

        if (!product.isActive()) {
            throw new RuntimeException("Produto inativo não pode ser reservado.");
        }
        if (product.getAvailableStock() < quantity) {
            throw new RuntimeException("Estoque insuficiente. Disponível: " + product.getAvailableStock());
        }

        product.setReservedStock(product.getReservedStock() + quantity);
        productRepository.save(product);
    }

    @Transactional
    public void releaseReservation(Long productId, int quantity) {
        Product product = findById(productId);

        if (product.getReservedStock() < quantity) {
            throw new RuntimeException("Tentativa de liberar mais estoque do que o reservado.");
        }

        product.setReservedStock(product.getReservedStock() - quantity);
        productRepository.save(product);
    }

    @Transactional
    public void consumeReserved(Long productId, int quantity) {
        Product product = findById(productId);

        if (product.getReservedStock() < quantity) {
            throw new RuntimeException("Tentativa de consumir mais estoque do que o reservado.");
        }

        // Subtrai tanto do reservado quanto do total, pois a venda foi concretizada
        product.setReservedStock(product.getReservedStock() - quantity);
        product.setTotalStock(product.getTotalStock() - quantity);
        
        productRepository.save(product);
    }

}