package com.orderprocessing.order_system;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "product")
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer stock;
    private Double price;
}