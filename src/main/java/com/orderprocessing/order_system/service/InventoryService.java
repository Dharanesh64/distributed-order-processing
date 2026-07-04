package com.orderprocessing.order_system.service;

import com.orderprocessing.order_system.Order;
import com.orderprocessing.order_system.Product;
import com.orderprocessing.order_system.repository.OrderRepository;
import com.orderprocessing.order_system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final RedisTemplate<String, String> redisTemplate;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockCacheService stockCacheService;
    private String lastReadOffset = "0-0";

    @Scheduled(fixedDelay = 1000)
    @SuppressWarnings("unchecked")
    public void processOrderEvents() {

        try {
            List<MapRecord<String, String, String>> records =
                    (List<MapRecord<String, String, String>>) (List<?>)
                            redisTemplate.opsForStream()
                                    .read(StreamOffset.create(
                                            "order-placed-stream",
                                            ReadOffset.from(lastReadOffset)));

            if (records == null || records.isEmpty()) return;

            for (MapRecord<String, String, String> record : records) {
                lastReadOffset = record.getId().getValue();

                Map<String, String> data = record.getValue();
                String orderId = data.get("orderId");
                String productId = data.get("productId");
                String quantity = data.get("quantity");
                String userId = data.get("userId");
                int requestedQty = Integer.parseInt(quantity);

                System.out.println("Inventory received order: " + orderId
                        + " product: " + productId);

                // Redis DECR — atomic stock decrement
                // No lock needed — DECR is atomic by itself
                String stockKey = "stock:" + productId;
                Long newStock = redisTemplate.opsForValue()
                        .decrement(stockKey, requestedQty);

                if (newStock != null && newStock >= 0) {
                    // Stock successfully reserved in Redis
                    System.out.println("Stock reserved via DECR. "
                            + "Product: " + productId
                            + " remaining stock: " + newStock);

                    // Update MySQL to match Redis
                    Optional<Product> productOpt = productRepository
                            .findById(Long.parseLong(productId));

                    if (productOpt.isPresent()) {
                        Product product = productOpt.get();
                        product.setStock(newStock.intValue());
                        productRepository.save(product);
                    }

                    // Update order status to CONFIRMED
                    updateOrderStatus(orderId, "CONFIRMED");
                    System.out.println("Order CONFIRMED: " + orderId);

                    // Fire to stock-reserved-stream
                    Map<String, String> eventData = Map.of(
                            "orderId", orderId,
                            "userId", userId,
                            "productId", productId,
                            "quantity", quantity
                    );

                    MapRecord<String, String, String> newRecord = StreamRecords
                            .newRecord()
                            .ofMap(eventData)
                            .withStreamKey("stock-reserved-stream");

                    redisTemplate.opsForStream().add(newRecord);
                    System.out.println("Event fired to stock-reserved-stream");

                } else {
                    // Stock went negative — restore Redis back
                    redisTemplate.opsForValue()
                            .increment(stockKey, requestedQty);

                    System.out.println("Out of stock. DECR restored. "
                            + "Product: " + productId);

                    // Cancel order
                    updateOrderStatus(orderId, "CANCELLED");
                    System.out.println("Order CANCELLED: " + orderId);
                }
            }
        } catch (Exception e) {
            System.out.println("Inventory error: " + e.getMessage());
        }
    }

    private void updateOrderStatus(String orderId, String status) {
        Optional<Order> orderOpt = orderRepository
                .findById(Long.parseLong(orderId));
        orderOpt.ifPresent(order -> {
            order.setStatus(status);
            orderRepository.save(order);
        });
    }
}