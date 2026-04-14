package com.orderprocessing.order_system.scheduler;

import com.orderprocessing.order_system.Order;
import com.orderprocessing.order_system.Product;
import com.orderprocessing.order_system.repository.OrderRepository;
import com.orderprocessing.order_system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Scheduled(fixedDelay = 60000)
    public void cancelExpiredOrders() {

        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(10);

        List<Order> expiredOrders = orderRepository
                .findByStatusAndCreatedAtBefore("PENDING", expiryTime);

        if (expiredOrders.isEmpty()) return;

        for (Order order : expiredOrders) {
            System.out.println("Expiring order: " + order.getId()
                    + " created at: " + order.getCreatedAt());

            Optional<Product> productOpt = productRepository
                    .findById(order.getProductId());

            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                product.setStock(product.getStock() + order.getQuantity());
                productRepository.save(product);
                System.out.println("Stock released for expired order: "
                        + order.getId());
            }

            order.setStatus("CANCELLED");
            orderRepository.save(order);
            System.out.println("Order expired and cancelled: " + order.getId());
        }
    }
}