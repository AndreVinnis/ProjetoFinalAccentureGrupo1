package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.config;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Category;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Product;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CategoryRepository; // Ajuste o import
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.ProductRepository;   // Ajuste o import
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EcommerceDataInitializer {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedECommerceData() {
        if (categoryRepository.count() == 0 && productRepository.count() == 0) {

            Category eletronicos = categoryRepository.save(Category.builder()
                    .name("Eletrônicos")
                    .description("Smartphones, Notebooks e Gadgets em geral.")
                    .build());

            Category vestuario = categoryRepository.save(Category.builder()
                    .name("Vestuário")
                    .description("Roupas masculinas, femininas e acessórios.")
                    .build());

            Category casa = categoryRepository.save(Category.builder()
                    .name("Casa e Decoração")
                    .description("Móveis, eletrodomésticos e itens de decoração.")
                    .build());

            Category livros = categoryRepository.save(Category.builder()
                    .name("Livros")
                    .description("Ficção, não-ficção, técnicos e acadêmicos.")
                    .build());

            Category esportes = categoryRepository.save(Category.builder()
                    .name("Esportes")
                    .description("Artigos esportivos e suplementação.")
                    .build());

            List<Product> products = List.of(
                    // --- ELETRÔNICOS ---
                    createProduct(eletronicos, "Smartphone X", "Smartphone de última geração", "3500.00", 50),
                    createProduct(eletronicos, "Notebook Pro", "Notebook para desenvolvedores", "7500.00", 30),
                    createProduct(eletronicos, "Fone Bluetooth", "Fone com cancelamento de ruído", "600.00", 100),
                    createProduct(eletronicos, "Smartwatch", "Relógio inteligente para treinos", "1200.00", 40),

                    // --- VESTUÁRIO ---
                    createProduct(vestuario, "Camiseta Básica", "Camiseta 100% algodão preta", "50.00", 200),
                    createProduct(vestuario, "Calça Jeans", "Calça jeans corte reto", "150.00", 80),
                    createProduct(vestuario, "Jaqueta de Couro", "Jaqueta de couro sintético", "350.00", 25),
                    createProduct(vestuario, "Tênis Casual", "Tênis confortável para o dia a dia", "250.00", 60),

                    // --- CASA ---
                    createProduct(casa, "Cafeteira Expresso", "Cafeteira automática 15 bar", "800.00", 20),
                    createProduct(casa, "Liquidificador", "Liquidificador potente 1000W", "180.00", 45),
                    createProduct(casa, "Sofá 3 Lugares", "Sofá retrátil e reclinável", "2200.00", 10),
                    createProduct(casa, "Luminária de Mesa", "Luminária LED articulável", "90.00", 70),

                    // --- LIVROS ---
                    createProduct(livros, "Clean Code", "Livro sobre boas práticas de programação", "120.00", 50),
                    createProduct(livros, "Java Efetivo", "Guia para programadores Java", "140.00", 35),
                    createProduct(livros, "Spring Boot em Ação", "Aprenda a criar APIs robustas", "110.00", 40),
                    createProduct(livros, "O Programador Pragmático", "De aprendiz a mestre", "130.00", 60),

                    // --- ESPORTES ---
                    createProduct(esportes, "Bola de Futebol", "Bola oficial tamanho 5", "100.00", 150),
                    createProduct(esportes, "Halteres 5kg", "Par de halteres emborrachados", "120.00", 30),
                    createProduct(esportes, "Tapete de Yoga", "Tapete em EVA antiderrapante", "60.00", 80),
                    createProduct(esportes, "Whey Protein", "Suplemento proteico 900g", "160.00", 100)
            );

            productRepository.saveAll(products);
        }
    }

    // Método auxiliar para não repetir o código do Builder 20 vezes
    private Product createProduct(Category category, String name, String description, String price, int stock) {
        return Product.builder()
                .category(category)
                .name(name)
                .description(description)
                .price(new BigDecimal(price))
                .totalStock(stock)
                .reservedStock(0)
                .active(true)
                .build();
    }
}