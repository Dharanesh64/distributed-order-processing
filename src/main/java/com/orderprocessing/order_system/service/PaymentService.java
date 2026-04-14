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
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RedisTemplate<String, String> redisTemplate;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private String lastReadOffset = "0-0";

    @Scheduled(fixedDelay = 1000)
    @SuppressWarnings("unchecked")
    public void processPayments() {

        try {
            List<MapRecord<String, String, String>> records =
                    (List<MapRecord<String, String, String>>) (List<?>)
                            redisTemplate.opsForStream()
                                    .read(StreamOffset.create(
                                            "stock-reserved-stream",
                                            ReadOffset.from(lastReadOffset)));

            if (records == null || records.isEmpty()) return;

            for (MapRecord<String, String, String> record : records) {
                lastReadOffset = record.getId().getValue();

                Map<String, String> data = record.getValue();
                String orderId = data.get("orderId");
                String userId = data.get("userId");
                String productId = data.get("productId");
                String quantity = data.get("quantity");

                System.out.println("Payment Service processing order: " + orderId);

                // Simulate payment — 80% success rate
                boolean paymentSuccess = new Random().nextInt(10) < 8;

                Optional<Order> orderOpt = orderRepository
                        .findById(Long.parseLong(orderId));

                if (orderOpt.isEmpty()) continue;

                Order order = orderOpt.get();

                if (paymentSuccess) {
                    order.setStatus("PROCESSING");
                    orderRepository.save(order);
                    System.out.println("Payment SUCCESS. Order PROCESSING: " + orderId);

                    // Fire to payment-done-stream
                    Map<String, String> eventData = Map.of(
                            "orderId", orderId,
                            "userId", userId,
                            "productId", productId,
                            "quantity", quantity
                    );

                    MapRecord<String, String, String> newRecord = StreamRecords
                            .newRecord()
                            .ofMap(eventData)
                            .withStreamKey("payment-done-stream");

                    redisTemplate.opsForStream().add(newRecord);
                    System.out.println("Event fired to payment-done-stream for order: " + orderId);

                } else {
                    // Payment failed — Saga compensation
                    System.out.println("Payment FAILED for order: " + orderId
                            + " — Running Saga compensation");

                    // Step 1 — Release stock back
                    Optional<Product> productOpt = productRepository
                            .findById(Long.parseLong(productId));

                    if (productOpt.isPresent()) {
                        Product product = productOpt.get();
                        product.setStock(product.getStock()
                                + Integer.parseInt(quantity));
                        productRepository.save(product);
                        System.out.println("Saga: Stock released back. Product "
                                + productId + " stock restored by " + quantity);
                    }

                    // Step 2 — Cancel order
                    order.setStatus("CANCELLED");
                    orderRepository.save(order);
                    System.out.println("Saga: Order CANCELLED: " + orderId);

                    // Step 3 — Fire compensation event
                    Map<String, String> compensationData = Map.of(
                            "orderId", orderId,
                            "userId", userId,
                            "productId", productId,
                            "reason", "payment_failed"
                    );

                    MapRecord<String, String, String> compensationRecord = StreamRecords
                            .newRecord()
                            .ofMap(compensationData)
                            .withStreamKey("order-cancelled-stream");

                    redisTemplate.opsForStream().add(compensationRecord);
                    System.out.println("Saga: Compensation event fired to order-cancelled-stream");
                }
            }
        } catch (Exception e) {
            System.out.println("Payment error: " + e.getMessage());
        }
    }
}