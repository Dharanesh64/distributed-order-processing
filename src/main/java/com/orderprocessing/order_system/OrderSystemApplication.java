package com.orderprocessing.order_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrderSystemApplication {
	public static void main(String[] args) {
		SpringApplication.run(OrderSystemApplication.class, args);
	}
}
//    "C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql" -u root -p
//     USE order_system;
//     UPDATE product SET stock = 10 WHERE id = 1;

//      docker exec -it redis redis-cli
//      SET stock:1 10
//      exit