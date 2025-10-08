package com.middy.assignment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.middy.assignment.model.Order;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final StatisticsModule statisticsModule;

    public OrderServiceImpl(StatisticsModule statisticsModule) {
        this.statisticsModule = statisticsModule;
    }

    @Override
    public void addOrder(Order order) {
        statisticsModule.addOrder(order);
    }

    @Override
    public void deleteAllOrders() {
        statisticsModule.deleteAllOrders();
    }
}
