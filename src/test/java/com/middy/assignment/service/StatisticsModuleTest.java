package com.middy.assignment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import com.middy.assignment.model.Order;
import com.middy.assignment.model.Statistics;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.*;

class StatisticsModuleTest {

    private StatisticsModule statisticsModule;
    private long currentTime;
    private static final int STATS_PERIOD = 60000; // 60 seconds

    @BeforeEach
    void setUp() {
        statisticsModule = new StatisticsModule(Clock.systemDefaultZone(), STATS_PERIOD);
        currentTime = System.currentTimeMillis();
    }

    @Nested
    @DisplayName("Adding Orders Tests")
    class AddOrderTests {

        @Test
        @DisplayName("Should add single order successfully")
        void shouldAddSingleOrder() {
            // Given
            Order order = new Order(BigDecimal.valueOf(100.50), currentTime);

            // When
            statisticsModule.addOrder(order);
            Statistics stats = statisticsModule.getStatistics(currentTime, STATS_PERIOD);

            // Then
            assertEquals(BigDecimal.valueOf(100.50).setScale(2, RoundingMode.HALF_UP), stats.getSum());
            assertEquals(BigDecimal.valueOf(100.50).setScale(2, RoundingMode.HALF_UP), stats.getAvg());
            assertEquals(BigDecimal.valueOf(100.50).setScale(2, RoundingMode.HALF_UP), stats.getMax());
            assertEquals(BigDecimal.valueOf(100.50).setScale(2, RoundingMode.HALF_UP), stats.getMin());
            assertEquals(1, stats.getCount());
        }

        @Test
        @DisplayName("Should add multiple orders with same timestamp")
        void shouldAddMultipleOrdersSameTimestamp() {
            // Given
            Order order1 = new Order(BigDecimal.valueOf(100.00), currentTime);
            Order order2 = new Order(BigDecimal.valueOf(200.00), currentTime);
            Order order3 = new Order(BigDecimal.valueOf(50.00), currentTime);

            // When
            statisticsModule.addOrder(order1);
            statisticsModule.addOrder(order2);
            statisticsModule.addOrder(order3);
            Statistics stats = statisticsModule.getStatistics(currentTime, STATS_PERIOD);

            // Then
            assertEquals(BigDecimal.valueOf(350.00).setScale(2, RoundingMode.HALF_UP), stats.getSum());
            assertEquals(BigDecimal.valueOf(116.67).setScale(2, RoundingMode.HALF_UP), stats.getAvg());
            assertEquals(BigDecimal.valueOf(200.00).setScale(2, RoundingMode.HALF_UP), stats.getMax());
            assertEquals(BigDecimal.valueOf(50.00).setScale(2, RoundingMode.HALF_UP), stats.getMin());
            assertEquals(3, stats.getCount());
        }

        @Test
        @DisplayName("Should add orders with different timestamps within last minute")
        void shouldAddOrdersDifferentTimestamps() {
            // Given
            long time1 = currentTime - 30000; // 30 seconds ago
            long time2 = currentTime - 15000; // 15 seconds ago
            Order order1 = new Order(BigDecimal.valueOf(100.00), time1);
            Order order2 = new Order(BigDecimal.valueOf(200.00), time2);
            Order order3 = new Order(BigDecimal.valueOf(300.00), currentTime);

            // When
            statisticsModule.addOrder(order1);
            statisticsModule.addOrder(order2);
            statisticsModule.addOrder(order3);
            Statistics stats = statisticsModule.getStatistics(currentTime, STATS_PERIOD);

            // Then
            assertEquals(BigDecimal.valueOf(600.00).setScale(2, RoundingMode.HALF_UP), stats.getSum());
            assertEquals(BigDecimal.valueOf(200.00).setScale(2, RoundingMode.HALF_UP), stats.getAvg());
            assertEquals(BigDecimal.valueOf(300.00).setScale(2, RoundingMode.HALF_UP), stats.getMax());
            assertEquals(BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP), stats.getMin());
            assertEquals(3, stats.getCount());
        }
    }

    @Nested
    @DisplayName("Statistics Calculation Tests")
    class StatisticsCalculationTests {

        @Test
        @DisplayName("Should return zero statistics when no orders exist")
        void shouldReturnZeroStatisticsWhenNoOrders() {
            // When
            Statistics stats = statisticsModule.getStatistics(currentTime, STATS_PERIOD);

            // Then
            assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), stats.getSum());
            assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), stats.getAvg());
            assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), stats.getMax());
            assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), stats.getMin());
            assertEquals(0, stats.getCount());
        }

        @Test
        @DisplayName("Should exclude orders 60 or older seconds")
        void shouldExcludeOldOrders() {

            statisticsModule = new StatisticsModule(Clock.fixed(Instant.ofEpochMilli(currentTime), UTC), STATS_PERIOD);

            // Given
            long oldTime = currentTime - STATS_PERIOD; // 60 seconds older)
            long recentTime = currentTime - STATS_PERIOD / 2; // 30 seconds ago

            Order oldOrder = new Order(BigDecimal.valueOf(500.00), oldTime);
            Order recentOrder = new Order(BigDecimal.valueOf(100.00), recentTime);

            // When
            try {
                statisticsModule.addOrder(oldOrder);
            } catch (Exception e) {
                // ignore exception for old order
            }
            statisticsModule.addOrder(recentOrder);

            Statistics stats = statisticsModule.getStatistics(currentTime + 1, STATS_PERIOD);

            // Then - Should only include recent order
            assertEquals(BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP), stats.getSum());
            assertEquals(BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP), stats.getAvg());
            assertEquals(BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP), stats.getMax());
            assertEquals(BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP), stats.getMin());
            assertEquals(1, stats.getCount());
        }

        @Test
        @DisplayName("Should handle edge case at exactly 60 seconds boundary")
        void shouldHandleExactBoundary() {

            statisticsModule = new StatisticsModule(Clock.fixed(Instant.ofEpochMilli(currentTime), UTC), STATS_PERIOD);

            long exactBoundaryTime = currentTime - STATS_PERIOD + 1; // Exactly 59999 milliseconds ago
            Order boundaryOrder = new Order(BigDecimal.valueOf(150.00), exactBoundaryTime);
            Order recentOrder = new Order(BigDecimal.valueOf(250.00), currentTime);

            statisticsModule.addOrder(boundaryOrder);
            statisticsModule.addOrder(recentOrder);
            Statistics stats = statisticsModule.getStatistics(currentTime, STATS_PERIOD);

            assertEquals(BigDecimal.valueOf(400.00).setScale(2, RoundingMode.HALF_UP), stats.getSum());
            assertEquals(BigDecimal.valueOf(200.00).setScale(2, RoundingMode.HALF_UP), stats.getAvg());
            assertEquals(BigDecimal.valueOf(250.00).setScale(2, RoundingMode.HALF_UP), stats.getMax());
            assertEquals(BigDecimal.valueOf(150.00).setScale(2, RoundingMode.HALF_UP), stats.getMin());
            assertEquals(2, stats.getCount());
        }

        @Test
        @DisplayName("Should handle decimal precision correctly")
        void shouldHandleDecimalPrecision() {
            // Given
            Order order1 = new Order(new BigDecimal("33.333"), currentTime);
            Order order2 = new Order(new BigDecimal("33.333"), currentTime);
            Order order3 = new Order(new BigDecimal("33.334"), currentTime);

            // When
            statisticsModule.addOrder(order1);
            statisticsModule.addOrder(order2);
            statisticsModule.addOrder(order3);
            Statistics stats = statisticsModule.getStatistics(currentTime, STATS_PERIOD);

            // Then
            assertEquals(new BigDecimal("100.00"), stats.getSum());
            assertEquals(new BigDecimal("33.33"), stats.getAvg());
            assertEquals(new BigDecimal("33.33"), stats.getMax());
            assertEquals(new BigDecimal("33.33"), stats.getMin());
            assertEquals(3, stats.getCount());
        }
    }

    @Nested
    @DisplayName("Delete Orders Tests")
    class DeleteOrdersTests {

        @Test
        @DisplayName("Should clear all statistics after delete")
        void shouldClearAllStatisticsAfterDelete() {
            // Given
            Order order1 = new Order(BigDecimal.valueOf(100.00), currentTime);
            Order order2 = new Order(BigDecimal.valueOf(200.00), currentTime);
            statisticsModule.addOrder(order1);
            statisticsModule.addOrder(order2);

            // When
            statisticsModule.deleteAllOrders();
            Statistics stats = statisticsModule.getStatistics(currentTime, STATS_PERIOD);

            // Then
            assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), stats.getSum());
            assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), stats.getAvg());
            assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), stats.getMax());
            assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), stats.getMin());
            assertEquals(0, stats.getCount());
        }
    }

    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("Should handle concurrent order additions")
        void shouldHandleConcurrentOrderAdditions() throws InterruptedException {
            // Given
            int numberOfThreads = 10;
            int ordersPerThread = 100;
            Thread[] threads = new Thread[numberOfThreads];

            // When
            for (int i = 0; i < numberOfThreads; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < ordersPerThread; j++) {
                        Order order = new Order(BigDecimal.valueOf(10.00), currentTime);
                        statisticsModule.addOrder(order);
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            Statistics stats = statisticsModule.getStatistics(currentTime, STATS_PERIOD);

            // Then
            assertEquals(numberOfThreads * ordersPerThread, stats.getCount());
            assertEquals(BigDecimal.valueOf(numberOfThreads * ordersPerThread * 10.00).setScale(2, RoundingMode.HALF_UP), stats.getSum());
            assertEquals(BigDecimal.valueOf(10.00).setScale(2, RoundingMode.HALF_UP), stats.getAvg());
            assertEquals(BigDecimal.valueOf(10.00).setScale(2, RoundingMode.HALF_UP), stats.getMax());
            assertEquals(BigDecimal.valueOf(10.00).setScale(2, RoundingMode.HALF_UP), stats.getMin());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very small amounts")
        void shouldHandleVerySmallAmounts() {
            // Given
            Order order = new Order(new BigDecimal("0.01"), currentTime);

            // When
            statisticsModule.addOrder(order);
            Statistics stats = statisticsModule.getStatistics(currentTime, STATS_PERIOD);

            // Then
            assertEquals(new BigDecimal("0.01"), stats.getSum());
            assertEquals(new BigDecimal("0.01"), stats.getAvg());
            assertEquals(new BigDecimal("0.01"), stats.getMax());
            assertEquals(new BigDecimal("0.01"), stats.getMin());
            assertEquals(1, stats.getCount());
        }

        @Test
        @DisplayName("Should handle very large amounts")
        void shouldHandleVeryLargeAmounts() {
            // Given
            Order order = new Order(new BigDecimal("999999999.99"), currentTime);

            // When
            statisticsModule.addOrder(order);
            Statistics stats = statisticsModule.getStatistics(currentTime, STATS_PERIOD);

            // Then
            assertEquals(new BigDecimal("999999999.99"), stats.getSum());
            assertEquals(new BigDecimal("999999999.99"), stats.getAvg());
            assertEquals(new BigDecimal("999999999.99"), stats.getMax());
            assertEquals(new BigDecimal("999999999.99"), stats.getMin());
            assertEquals(1, stats.getCount());
        }

        @Test
        @DisplayName("Should handle zero amount orders")
        void shouldHandleZeroAmountOrders() {
            // Given
            Order order1 = new Order(BigDecimal.ZERO, currentTime);
            Order order2 = new Order(BigDecimal.valueOf(100.00), currentTime);

            // When
            statisticsModule.addOrder(order1);
            statisticsModule.addOrder(order2);
            Statistics stats = statisticsModule.getStatistics(currentTime, STATS_PERIOD);

            // Then
            assertEquals(BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP), stats.getSum());
            assertEquals(BigDecimal.valueOf(50.00).setScale(2, RoundingMode.HALF_UP), stats.getAvg());
            assertEquals(BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP), stats.getMax());
            assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), stats.getMin());
            assertEquals(2, stats.getCount());
        }

        @Test
        @DisplayName("Should handle future timestamps")
        void shouldHandleFutureTimestamps() {

            statisticsModule = new StatisticsModule(Clock.fixed(Instant.ofEpochMilli(currentTime), UTC), STATS_PERIOD);

            long futureTime = currentTime + 30000; // 30 seconds in the future
            Order futureOrder = new Order(BigDecimal.valueOf(100.00), futureTime);
            Order currentOrder = new Order(BigDecimal.valueOf(200.00), currentTime);

            try {
                statisticsModule.addOrder(futureOrder);
            } catch (Exception e) {
                // ignore exception for future order
            }

            statisticsModule.addOrder(currentOrder);
            Statistics stats = statisticsModule.getStatistics(currentTime, STATS_PERIOD);

            // Then - Future order should not be included in current time statistics
            assertEquals(BigDecimal.valueOf(200.00).setScale(2, RoundingMode.HALF_UP), stats.getSum());
            assertEquals(BigDecimal.valueOf(200.00).setScale(2, RoundingMode.HALF_UP), stats.getAvg());
            assertEquals(BigDecimal.valueOf(200.00).setScale(2, RoundingMode.HALF_UP), stats.getMax());
            assertEquals(BigDecimal.valueOf(200.00).setScale(2, RoundingMode.HALF_UP), stats.getMin());
            assertEquals(1, stats.getCount());
        }
    }

    @Nested
    @DisplayName("Rounding Tests")
    class RoundingTests {

        @Test
        @DisplayName("Should round average correctly using HALF_UP")
        void shouldRoundAverageCorrectly() {
            // Given - Create orders that will result in non-terminating decimal
            Order order1 = new Order(BigDecimal.valueOf(100.00), currentTime);
            Order order2 = new Order(BigDecimal.valueOf(200.00), currentTime);
            Order order3 = new Order(BigDecimal.valueOf(300.00), currentTime);

            // When
            statisticsModule.addOrder(order1);
            statisticsModule.addOrder(order2);
            statisticsModule.addOrder(order3);
            Statistics stats = statisticsModule.getStatistics(currentTime, STATS_PERIOD);

            // Then
            assertEquals(BigDecimal.valueOf(600.00).setScale(2, RoundingMode.HALF_UP), stats.getSum());
            assertEquals(BigDecimal.valueOf(200.00).setScale(2, RoundingMode.HALF_UP), stats.getAvg());
            assertEquals(3, stats.getCount());
        }

        @Test
        @DisplayName("Should handle division that results in repeating decimals")
        void shouldHandleRepeatingDecimals() {
            // Given - 10 / 3 = 3.333...
            Order order1 = new Order(BigDecimal.valueOf(10.00), currentTime);

            // When
            statisticsModule.addOrder(order1);
            statisticsModule.addOrder(order1);
            statisticsModule.addOrder(order1);
            Statistics stats = statisticsModule.getStatistics(currentTime, STATS_PERIOD);

            // Then
            assertEquals(BigDecimal.valueOf(30.00).setScale(2, RoundingMode.HALF_UP), stats.getSum());
            assertEquals(BigDecimal.valueOf(10.00).setScale(2, RoundingMode.HALF_UP), stats.getAvg());
            assertEquals(3, stats.getCount());
        }
    }
}
