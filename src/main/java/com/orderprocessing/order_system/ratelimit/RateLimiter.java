package com.orderprocessing.order_system.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final RedisTemplate<String, String> redisTemplate;

    public boolean isAllowed(String userId, String apiType) {

        int maxRequests;
        int windowSeconds = 60;

        // Different limits for different APIs
        switch (apiType) {
            case "CREATE_ORDER" -> maxRequests = 5;
            case "GET_ORDER" -> maxRequests = 60;
            default -> maxRequests = 10;
        }

        String key = "rate_limit:" + apiType + ":user:" + userId;

        String currentCount = redisTemplate.opsForValue().get(key);

        if (currentCount == null) {
            redisTemplate.opsForValue()
                    .set(key, "1", windowSeconds, TimeUnit.SECONDS);
            return true;
        }

        int count = Integer.parseInt(currentCount);

        if (count < maxRequests) {
            redisTemplate.opsForValue().increment(key);
            return true;
        }

        return false;
    }
}