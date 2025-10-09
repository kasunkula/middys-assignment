package com.middy.assignment.service;

import com.middy.assignment.model.Order;
import com.middy.assignment.model.Statistics;

public interface StatisticsModule {
    public void addOrder(Order newOrder);

    public void deleteAllOrders();

    public Statistics getStatistics(long currentTimeMillis, int periodInMillis);
}
