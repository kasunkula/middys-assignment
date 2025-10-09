package com.middy.assignment.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.middy.assignment.dto.transformers.OrderTransformer;
import com.middy.assignment.dto.OrderDto;
import com.middy.assignment.exception.OrderValidationException;
import com.middy.assignment.model.Order;
import com.middy.assignment.service.OrderService;


@Slf4j
@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderTransformer orderTransformer;

    public OrderController(OrderService orderService, OrderTransformer orderTransformer) {
        this.orderService = orderService;
        this.orderTransformer = orderTransformer;
    }

    @PostMapping
    public ResponseEntity<Void> addOrder(@RequestBody OrderDto orderDto) {
        try {
            Order order = orderTransformer.validateAndTransformToOrder(orderDto);
            orderService.addOrder(order);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (OrderValidationException e) {
            log.error("Unexpected error on addOrder", e);
            return ResponseEntity.status(e.getHttpStatus()).build();
        } catch (Exception e) {
            log.error("Unexpected error on addOrder", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllOrders() {
        try {
            orderService.deleteAllOrders();
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting all orders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
