package com.middy.assignment.controller;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.middy.assignment.dto.transformers.OrderTransformer;
import com.middy.assignment.service.OrderService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static OrderService mockOrderService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public OrderTransformer orderTransformer() {
            return new OrderTransformer(Clock.systemDefaultZone());
        }
        @Bean
        public OrderService orderService() {
            mockOrderService = Mockito.mock(OrderService.class);
            return mockOrderService;
        }
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(mockOrderService);
    }

    @Test
    void addOrder_success_returns201() throws Exception {
        String now = java.time.Instant.now().toString();
        String body = "{\"amount\":\"100.00\",\"timestamp\":\"" + now + "\"}";
        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));
    }

    @Test
    void addOrder_oldOrder_returns204() throws Exception {
        String old = java.time.Instant.ofEpochMilli(System.currentTimeMillis() - 61000).toString();
        String body = "{\"amount\":\"100.00\",\"timestamp\":\"" + old + "\"}";
        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void addOrder_invalidJson_returns400() throws Exception {
        String body = "{\"amount\":null,\"timestamp\":null}";
        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
    }

    @Test
    void addOrder_unparsableAmount_returns422() throws Exception {
        String now = java.time.Instant.now().toString();
        String body = "{\"amount\":\"not-a-number\",\"timestamp\":\"" + now + "\"}";
        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(""));
    }

    @Test
    void addOrder_futureOrder_returns422() throws Exception {
        String future = java.time.Instant.ofEpochMilli(System.currentTimeMillis() + 60000).toString();
        String body = "{\"amount\":\"100.00\",\"timestamp\":\"" + future + "\"}";
        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(""));
    }

    @Test
    void deleteAllOrders_success_returns204() throws Exception {
        mockMvc.perform(delete("/v1/orders"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        
        verify(mockOrderService).deleteAllOrders();
    }

    @Test
    void deleteAllOrders_serviceException_returns500() throws Exception {
        doThrow(new RuntimeException("Service error")).when(mockOrderService).deleteAllOrders();
        
        mockMvc.perform(delete("/v1/orders"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));
    }

}