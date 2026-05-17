package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Category;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Product;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.AdminProductResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.ProductResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService - Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductService productService;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(1L).name("Eletrônicos").build();
        product = Product.builder()
                .id(100L)
                .name("Smartphone")
                .description("Smartphone top")
                .price(new BigDecimal("1500.00"))
                .totalStock(10)
                .reservedStock(0)
                .active(true)
                .category(category)
                .build();
    }

    @Test
    @DisplayName("findById retorna o produto quando existe")
    void findById_success() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        Product result = productService.findById(100L);

        assertThat(result).isSameAs(product);
    }

    @Test
    @DisplayName("findById lança RuntimeException quando produto não existe")
    void findById_throwsWhenMissing() {
        when(productRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Produto não encontrado");
    }

    @Test
    @DisplayName("listActiveProducts usa filtro de categoria e preço quando ambos informados")
    void listActiveProducts_categoryAndPriceFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findByActiveTrueAndCategoryNameAndPriceLessThanEqual(
                eq("Eletrônicos"), eq(new BigDecimal("2000.00")), eq(pageable))).thenReturn(page);

        Page<ProductResponse> result = productService.listActiveProducts("Eletrônicos", new BigDecimal("2000.00"), pageable);

        assertThat(result.getContent()).hasSize(1);
        ProductResponse first = result.getContent().get(0);
        assertThat(first.id()).isEqualTo(100L);
        assertThat(first.categoryName()).isEqualTo("Eletrônicos");
        assertThat(first.availableStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("listActiveProducts usa filtro apenas de categoria quando preço é nulo")
    void listActiveProducts_categoryOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findByActiveTrueAndCategoryName(eq("Eletrônicos"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductResponse> result = productService.listActiveProducts("Eletrônicos", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository, never()).findByActiveTrue(any());
    }

    @Test
    @DisplayName("listActiveProducts usa filtro apenas de preço quando categoria é nula")
    void listActiveProducts_priceOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findByActiveTrueAndPriceLessThanEqual(eq(new BigDecimal("1000.00")), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductResponse> result = productService.listActiveProducts(null, new BigDecimal("1000.00"), pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("listActiveProducts retorna todos os ativos quando não há filtros")
    void listActiveProducts_noFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findByActiveTrue(eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductResponse> result = productService.listActiveProducts(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).price()).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("listProductsForAdmin retorna produtos com estoque total, reservado, disponÃ­vel e status")
    void listProductsForAdmin_success() {
        Pageable pageable = PageRequest.of(0, 10);
        product.setReservedStock(3);
        when(productRepository.findAll(eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(product)));

        Page<AdminProductResponse> result = productService.listProductsForAdmin(pageable);

        assertThat(result.getContent()).hasSize(1);
        AdminProductResponse first = result.getContent().get(0);
        assertThat(first.id()).isEqualTo(100L);
        assertThat(first.totalStock()).isEqualTo(10);
        assertThat(first.reservedStock()).isEqualTo(3);
        assertThat(first.availableStock()).isEqualTo(7);
        assertThat(first.active()).isTrue();
        assertThat(first.categoryName()).isEqualTo("EletrÃ´nicos");
    }

    @Test
    @DisplayName("createProduct salva o produto com os dados informados")
    void createProduct_success() {
        when(categoryService.findEntityByName("Eletrônicos")).thenReturn(category);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product created = productService.createProduct(
                "Notebook",
                "Notebook gamer",
                new BigDecimal("4500.00"),
                5,
                "Eletrônicos"
        );

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();

        assertThat(saved.getName()).isEqualTo("Notebook");
        assertThat(saved.getDescription()).isEqualTo("Notebook gamer");
        assertThat(saved.getPrice()).isEqualByComparingTo("4500.00");
        assertThat(saved.getTotalStock()).isEqualTo(5);
        assertThat(saved.getReservedStock()).isZero();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCategory()).isSameAs(category);
        assertThat(created).isSameAs(saved);
    }

    @Test
    @DisplayName("updateProduct altera os dados do produto e salva")
    void updateProduct_success() {
        Category novaCategoria = Category.builder().id(2L).name("Acessórios").build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(categoryService.findEntityByName("Acessórios")).thenReturn(novaCategoria);
        when(productRepository.save(product)).thenReturn(product);

        Product updated = productService.updateProduct(
                100L,
                "Smartphone Pro",
                "Versão atualizada",
                new BigDecimal("1800.00"),
                "Acessórios"
        );

        assertThat(updated.getName()).isEqualTo("Smartphone Pro");
        assertThat(updated.getDescription()).isEqualTo("Versão atualizada");
        assertThat(updated.getPrice()).isEqualByComparingTo("1800.00");
        assertThat(updated.getCategory()).isSameAs(novaCategoria);
    }

    @Test
    @DisplayName("deactivate marca o produto como inativo")
    void deactivate_success() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        productService.deactivate(100L);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("activate marca o produto como ativo")
    void activate_success() {
        product.setActive(false);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        productService.activate(100L);

        assertThat(product.isActive()).isTrue();
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("restock incrementa o estoque total do produto")
    void restock_success() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        Product result = productService.restock(100L, 5);

        assertThat(result.getTotalStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("restock lança IllegalArgumentException quando quantidade <= 0")
    void restock_throwsWhenInvalidQuantity() {
        assertThatThrownBy(() -> productService.restock(100L, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> productService.restock(100L, -3))
                .isInstanceOf(IllegalArgumentException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("reserveStock acumula o reservedStock e salva")
    void reserveStock_success() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        productService.reserveStock(100L, 3);

        assertThat(product.getReservedStock()).isEqualTo(3);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("reserveStock lança RuntimeException para produto inativo")
    void reserveStock_throwsWhenInactive() {
        product.setActive(false);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.reserveStock(100L, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    @DisplayName("reserveStock lança RuntimeException quando estoque é insuficiente")
    void reserveStock_throwsWhenStockInsufficient() {
        product.setTotalStock(2);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.reserveStock(100L, 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Estoque insuficiente");
    }

    @Test
    @DisplayName("releaseReservation diminui o reservedStock")
    void releaseReservation_success() {
        product.setReservedStock(5);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        productService.releaseReservation(100L, 3);

        assertThat(product.getReservedStock()).isEqualTo(2);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("releaseReservation lança RuntimeException quando tenta liberar mais que o reservado")
    void releaseReservation_throwsWhenReleasingMoreThanReserved() {
        product.setReservedStock(1);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.releaseReservation(100L, 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("liberar");
    }

    @Test
    @DisplayName("consumeReserved diminui reservedStock e totalStock")
    void consumeReserved_success() {
        product.setReservedStock(4);
        product.setTotalStock(10);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        productService.consumeReserved(100L, 3);

        assertThat(product.getReservedStock()).isEqualTo(1);
        assertThat(product.getTotalStock()).isEqualTo(7);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("consumeReserved lança RuntimeException quando tenta consumir mais que o reservado")
    void consumeReserved_throwsWhenConsumingMoreThanReserved() {
        product.setReservedStock(2);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.consumeReserved(100L, 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("consumir");
    }
}
