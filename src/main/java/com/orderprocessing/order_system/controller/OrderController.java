package com.orderprocessing.order_system.controller;

import com.orderprocessing.order_system.Order;
import com.orderprocessing.order_system.service.OrderService;
import com.orderprocessing.order_system.repository.OrderRepository;
import com.orderprocessing.order_system.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final RateLimiter rateLimiter;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam String idempotencyKey) {

        // Rate limit check
        if (!rateLimiter.isAllowed(userId.toString(), "CREATE_ORDER")) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Maximum 5 orders per minute allowed.");
        }

        Order order = orderService.createOrder(
                userId, productId, quantity, idempotencyKey);

        return ResponseEntity.ok(order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(
            @PathVariable Long orderId,
            @RequestParam Long userId) {

        // Rate limit check
        if (!rateLimiter.isAllowed(userId.toString(), "GET_ORDER")) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Maximum 60 requests per minute allowed.");
        }

        Optional<Order> order = orderRepository.findById(orderId);

        if (order.isPresent()) {
            return ResponseEntity.ok(order.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}