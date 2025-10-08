package com.middy.assignment.service;

import com.middy.assignment.model.Order;

public interface OrderService {
    
    void addOrder(Order order);

    /**
     * Deletes all orders from the system.
     * Implementations should remove every order record.
     */
    void deleteAllOrders();
}
