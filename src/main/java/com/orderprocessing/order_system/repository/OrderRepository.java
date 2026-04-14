package com.orderprocessing.order_system.repository;

import com.orderprocessing.order_system.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    List<Order> findByStatusAndCreatedAtBefore(String status, LocalDateTime dateTime);
}