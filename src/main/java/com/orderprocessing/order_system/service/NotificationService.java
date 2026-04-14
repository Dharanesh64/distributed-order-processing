package com.orderprocessing.order_system.service;

import com.orderprocessing.order_system.Order;
import com.orderprocessing.order_system.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final OrderRepository orderRepository;
    private String lastSuccessOffset = "0-0";
    private String lastCancelOffset = "0-0";

    @Scheduled(fixedDelay = 1000)
    @SuppressWarnings("unchecked")
    public void sendNotifications() {
        handlePaymentDoneStream();
        handleCancelledStream();
    }

    private void handlePaymentDoneStream() {
        try {
            List<MapRecord<String, String, String>> records =
                    (List<MapRecord<String, String, String>>) (List<?>)
                            redisTemplate.opsForStream()
                                    .read(StreamOffset.create(
                                            "payment-done-stream",
                                            ReadOffset.from(lastSuccessOffset)));

            if (records == null || records.isEmpty()) return;

            for (MapRecord<String, String, String> record : records) {
                lastSuccessOffset = record.getId().getValue();

                Map<String, String> data = record.getValue();
                String orderId = data.get("orderId");
                String userId = data.get("userId");

                System.out.println("SMS sent to user " + userId
                        + " → Order " + orderId
                        + " confirmed! Your item is being processed.");
            }
        } catch (Exception e) {
            // stream not ready yet
        }
    }

    private void handleCancelledStream() {
        try {
            List<MapRecord<String, String, String>> records =
                    (List<MapRecord<String, String, String>>) (List<?>)
                            redisTemplate.opsForStream()
                                    .read(StreamOffset.create(
                                            "order-cancelled-stream",
                                            ReadOffset.from(lastCancelOffset)));

            if (records == null || records.isEmpty()) return;

            for (MapRecord<String, String, String> record : records) {
                lastCancelOffset = record.getId().getValue();

                Map<String, String> data = record.getValue();
                String orderId = data.get("orderId");
                String userId = data.get("userId");
                String reason = data.get("reason");

                System.out.println("SMS sent to user " + userId
                        + " → Sorry, order " + orderId
                        + " was cancelled. Reason: " + reason
                        + ". Stock has been released.");
            }
        } catch (Exception e) {
            // stream not ready yet
        }
    }
}