package com.orderprocessing.order_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;    //   1. Start Docker Desktop  2. docker start redis
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrderSystemApplication {
	public static void main(String[] args) {
		SpringApplication.run(OrderSystemApplication.class, args);
	}
}







