package com.middy.assignment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.middy.assignment.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.middy.assignment.dto.OrderDto;
import com.middy.assignment.dto.StatisticsDto;

import static com.middy.assignment.Utils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConcurrencyTest {
    private static final Logger log = LoggerFactory.getLogger(ConcurrencyTest.class);

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private Clock clock;

    @BeforeEach
    void setup() {
        orderService.deleteAllOrders();
    }

    @Test
    @Timeout(60)
    void testConcurrentOrderSubmission() throws InterruptedException {
        int threadCount = 100;
        int orderPerThread = 50;
        int totalOrders = threadCount * orderPerThread;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalOrders);

        ArrayList<OrderDto> orders = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        long now = clock.millis();
        long minTimestampStr = now;
        long maxTimestampStr = 0L;
        var randomTimeDiff = new Random();
        var randomDecimals = new Random();
        for (int i = 0; i < totalOrders; i++) {
            double amount = 100.0 + i + randomDecimals.nextInt(0, 99) / 100.0;
            long timestamp = now - randomTimeDiff.nextLong(0, 30000);
            OrderDto order = getOrder(amount, timestamp);
            orders.add(order);
            sum = sum.add(BigDecimal.valueOf(amount));
            minTimestampStr = Math.min(timestamp, minTimestampStr);
            maxTimestampStr =  Math.max(timestamp, maxTimestampStr);
        }

        sum = sum.setScale(2, RoundingMode.HALF_UP);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);

        log.info("Prepared {} orders with total sum: {} and avg: {} timestamps from {}({}) to {}({})",
            totalOrders, sum, avg,
                java.time.Instant.ofEpochMilli(minTimestampStr).toString(), minTimestampStr,
                java.time.Instant.ofEpochMilli(maxTimestampStr).toString(), maxTimestampStr);

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                for (int j = 0; j < orderPerThread; j++) {
                    submitOrder(orders.get(idx * orderPerThread + j), webTestClient);
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        log.info("All {} orders submitted in {} ms", totalOrders, (endTime - startTime));
        executor.shutdown();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/actuator/metrics/http.server.requests")
                        .queryParam("tag", "method:POST")
                        .queryParam("tag", "uri:/v1/orders")
                        .build())
                .header("accept", "application/vnd.spring-boot.actuator.v3+json")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    log.info("POST metrics response: {}", body);
                    assert body.contains("statistic\":\"MAX\",\"value\":");
                    String valueStr = body.replaceAll(".*statistic\":\"MAX\",\"value\":([0-9\\.]+).*", "$1");
                    double value = Double.parseDouble(valueStr);
                    log.info("MAX response time for POST /v1/orders: {} seconds", value);
//                    assert value < 2 : "MAX value is not less than 2 seconds";
                });

        // Get percentile information from histogram endpoint
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/actuator/metrics/http.server.requests.percentile")
                        .queryParam("tag", "method:POST")
                        .queryParam("tag", "uri:/v1/orders")
                        .queryParam("phi", "0.999")
                        .build())
                .header("accept", "application/vnd.spring-boot.actuator.v3+json")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    log.info("Percentile metrics response: {}", body);
                });

        BigDecimal finalSum = sum;
        webTestClient.get()
                .uri("/v1/statistics")
                .exchange()
                .expectStatus().isOk()
                .expectBody(StatisticsDto.class)
                .value(stats -> {
                    assertEquals(totalOrders, stats.getCount(), "Count incorrect");
                    assertEquals(finalSum, stats.getSum(), "Sum incorrect");
                    assertEquals(avg, stats.getAvg(), "Sum incorrect");
                });


    }
}

