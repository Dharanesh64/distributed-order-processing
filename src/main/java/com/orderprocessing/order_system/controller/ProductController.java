package com.orderprocessing.order_system.controller;

import com.orderprocessing.order_system.Product;
import com.orderprocessing.order_system.repository.ProductRepository;
import com.orderprocessing.order_system.service.StockCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final StockCacheService stockCacheService;

    // Add new product
    @PostMapping("/add")
    public ResponseEntity<Product> addProduct(
            @RequestParam String name,
            @RequestParam Integer stock,
            @RequestParam Double price) {

        // Save to MySQL
        Product product = new Product();
        product.setName(name);
        product.setStock(stock);
        product.setPrice(price);
        Product saved = productRepository.save(product);

        // Update Redis cache
        stockCacheService.updateStockCache(saved.getId(), stock);

        System.out.println("Product added: " + name
                + " stock: " + stock
                + " cached in Redis");

        return ResponseEntity.ok(saved);
    }

    // Get all products
    @GetMapping("/all")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    // Update stock
    @PutMapping("/stock")
    public ResponseEntity<String> updateStock(
            @RequestParam Long productId,
            @RequestParam Integer stock) {

        productRepository.findById(productId).ifPresent(product -> {
            product.setStock(stock);
            productRepository.save(product);
            stockCacheService.updateStockCache(productId, stock);
            System.out.println("Stock updated for product: "
                    + productId + " new stock: " + stock);
        });

        return ResponseEntity.ok("Stock updated successfully");
    }
}