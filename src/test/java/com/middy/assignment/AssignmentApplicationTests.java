package com.middy.assignment;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.middy.assignment.dto.StatisticsDto;
import com.middy.assignment.service.OrderService;

import static com.middy.assignment.Utils.submitOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssignmentApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private OrderService orderService;

    private WebTestClient webTestClient;

    @Test
    void contextLoads() {

    }

    @BeforeEach
    void setup() {
        orderService.deleteAllOrders();
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void addOrder() {

        submitOrder(100.0, 1000, webTestClient);
        submitOrder(60.0, 1000, webTestClient);
        submitOrder(130.0, 5000, webTestClient);
        submitOrder(70.0, 10000, webTestClient);
        submitOrder(140.0, 10000, webTestClient);

        webTestClient.get()
                .uri("/v1/statistics")
                .exchange()
                .expectStatus().isOk()
                .expectBody(StatisticsDto.class)
                .value(stats -> {
                    assertEquals(stats.getCount(), 5, "Count incorrect");
                    assertEquals(BigDecimal.valueOf(500.00).setScale(2), stats.getSum(), "Sum incorrect");
                    assertEquals(BigDecimal.valueOf(100.00).setScale(2), stats.getAvg(), "Avg incorrect");
                    assertEquals(BigDecimal.valueOf(140.00).setScale(2), stats.getMax(), "Max incorrect");
                    assertEquals(BigDecimal.valueOf(60.00).setScale(2), stats.getMin(), "Min incorrect");
                });
    }


}
