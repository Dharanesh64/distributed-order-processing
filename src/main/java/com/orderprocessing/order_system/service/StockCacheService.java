package com.orderprocessing.order_system.service;

import com.orderprocessing.order_system.Product;
import com.orderprocessing.order_system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockCacheService implements ApplicationRunner {

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductRepository productRepository;

    // Runs automatically when application starts
    @Override
    public void run(ApplicationArguments args) {
        System.out.println("Loading stock into Redis cache...");

        List<Product> products = productRepository.findAll();

        for (Product product : products) {
            String key = "stock:" + product.getId();
            redisTemplate.opsForValue().set(key,
                    String.valueOf(product.getStock()));
            System.out.println("Cached stock for product "
                    + product.getId() + " = " + product.getStock());
        }

        System.out.println("Stock cache loaded successfully.");
    }

    // Call this after stock changes in DB
    public void updateStockCache(Long productId, int newStock) {
        String key = "stock:" + productId;
        redisTemplate.opsForValue().set(key, String.valueOf(newStock));
    }

    // Get stock from cache
    public int getStockFromCache(Long productId) {
        String key = "stock:" + productId;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return 0;
        return Integer.parseInt(value);
    }
}