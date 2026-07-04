package com.orderprocessing.order_system.service;

import com.orderprocessing.order_system.Order;
import com.orderprocessing.order_system.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final StockCacheService stockCacheService;

    public Order createOrder(Long userId, Long productId,
                             Integer quantity, String idempotencyKey) {

        // Step 1 — Check idempotency
        Optional<Order> existing = orderRepository
                .findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            return existing.get();
        }

        // Step 2 — PRE-CHECK stock from Redis cache
        int cachedStock = stockCacheService.getStockFromCache(productId);

        if (cachedStock <= 0) {
            System.out.println("Pre-check failed — product "
                    + productId + " is out of stock");

            // Create cancelled order immediately
            Order order = new Order();
            order.setUserId(userId);
            order.setProductId(productId);
            order.setQuantity(quantity);
            order.setStatus("CANCELLED");
            order.setIdempotencyKey(idempotencyKey);
            return orderRepository.save(order);
        }

        System.out.println("Pre-check passed — stock available: "
                + cachedStock + " for product: " + productId);

        // Step 3 — Create order as PENDING
        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setStatus("PENDING");
        order.setIdempotencyKey(idempotencyKey);

        Order savedOrder = orderRepository.save(order);

        // Step 4 — Fire to order-placed-stream
        Map<String, String> eventData = Map.of(
                "orderId", savedOrder.getId().toString(),
                "userId", userId.toString(),
                "productId", productId.toString(),
                "quantity", quantity.toString()
        );

        MapRecord<String, String, String> record = StreamRecords
                .newRecord()
                .ofMap(eventData)
                .withStreamKey("order-placed-stream");

        redisTemplate.opsForStream().add(record);

        System.out.println("Event fired to order-placed-stream for order: "
                + savedOrder.getId());

        return savedOrder;
    }
}