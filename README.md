# Order Statistics API

A high-performance Spring Boot application that provides real-time statistics for order transactions. The application
calculates statistics (sum, average, minimum, maximum, and count) for orders received within the last 60 seconds.

## 📝 Assumptions and Design Decisions

### 60-Second Window Exclusivity

The application considers orders that are **exactly 60,000 milliseconds (60 seconds) old as exclusive** in the
statistics calculation. This means:

- An order with timestamp exactly 60 seconds ago **WILL BE EXCLUDED** from statistics
- An order with timestamp 59,999ms old or newer **WILL BE INCLUDED** in statistics
- An order with timestamp 60,000+ milliseconds ago **WILL BE REJECTED**

**Example:**

```
Current time: 1696507800000 (2023-10-05T10:30:00.000Z)
Valid order timestamp: 1696507740001 (2023-10-05T10:29:00.001Z) - 59,999ms old ✅
Invalid order timestamp: 1696507740000 (2023-10-05T10:29:00.000Z) - exactly 60 seconds old ❌
Invalid order timestamp: 1696507739999 (2023-10-05T10:28:59.999Z) - 60.001 seconds old ❌
```

### Array-Based Time Indexing Strategy

The application uses a **modulo-based circular buffer** for array indexing:

```java
int index = (int) (newOrder.getTimestamp() % statisticsPeriodInMillis);
```
**Design Rationale:**

- **Circular Buffer**: Uses timestamp modulo 60,000 to create a rotating time window
- **Space Efficient**: Fixed memory usage regardless of actual timestamp values
- **Time-based Grouping**: Orders from the same millisecond across different minutes overrides the previous data

### Array Size: 60,000 Elements

The array is sized as `statisticsPeriodInMillis` (60,000 elements) to accommodate:

- Valid age range: 0 to 59,999 milliseconds (exclusive of 60,000ms)
- Orders exactly 60,000ms old are rejected before indexing

### Thread Safety Design

**Atomic Operations**: Uses `AtomicReferenceArray<InterimStatistics>` for lock-free concurrent access
**Per-Millisecond Synchronization**: Each `InterimStatistics` object has its own synchronized block
**Race Condition Acknowledgment**: Known concurrency limitation documented in `InterimStatistics.add()` method

### O(1) Statistics Calculation

**Constant Time Guarantee**: `getStatistics()` loops through exactly 60,000 elements regardless of order volume
**Performance Characteristic**: O(60000) = O(1) since 60,000 is a fixed constant, not dependent on input size
**Trade-off**: Memory usage (60,000 objects) for guaranteed response time

## 🚀 Features

- **Real-time Statistics**: Calculate statistics for orders within the last 60 seconds
- **High Performance**: Optimized for concurrent operations with thread-safe implementations
- **Comprehensive Monitoring**: Spring Boot Actuator integration with metrics and health checks
- **API Documentation**: Swagger/OpenAPI integration for interactive API documentation
- **Concurrent Processing**: Thread-safe statistics calculations supporting high-throughput scenarios

## 📋 Table of Contents

- [Technologies](#technologies)
- [API Endpoints](#api-endpoints)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [Testing](#testing)
- [Monitoring](#monitoring)
- [Performance Considerations](#performance-considerations)

## 🛠 Technologies

- **Java 17**
- **Spring Boot 3.5.6**
- **Spring Web MVC**
- **Spring Boot Actuator** (for monitoring)
- **Lombok** (for reducing boilerplate code)
- **Jackson** (for JSON processing)
- **SpringDoc OpenAPI** (for API documentation)
- **Maven** (for dependency management)
- **JUnit 5** (for testing)
- **Mockito** (for mocking in tests)

## 🔗 API Endpoints

### Order Management

#### Add Order

```http
POST /v1/orders
Content-Type: application/json

{
  "amount": "100.50",
  "timestamp": "2025-10-05T10:30:00.000Z"
}
```

**Response Codes:**

- `201 Created` - Order successfully added
- `400 Bad Request` - Invalid JSON format
- `422 Unprocessable Entity` - Invalid amount or future timestamp
- `204 No Content` - Order timestamp is older than 60 seconds

#### Delete All Orders

```http
DELETE /v1/orders
```

**Response:** `200 OK`

### Statistics

#### Get Current Statistics

```http
GET /v1/statistics
```

**Response:**

```json
{
  "sum": "1500.75",
  "avg": "150.08",
  "max": "300.00",
  "min": "50.25",
  "count": 10
}
```

### Monitoring Endpoints

- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **API Documentation**: `GET /swagger-ui.html`

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Installation & Running

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd assignment
   ```

2. **Build the application**
   ```bash
   mvn clean compile
   ```

3. **Run tests**
   ```bash
   mvn test
   ```

4. **Start the application**
   ```bash
   mvn spring-boot:run
   ```

   Or run the JAR file:
   ```bash
   mvn clean package
   java -jar target/assignment-0.0.1-SNAPSHOT.jar
   ```

5. **Access the application**
    - API Base URL: `http://localhost:8080`
    - Swagger UI: `http://localhost:8080/swagger-ui.html`
    - Actuator Health: `http://localhost:8080/actuator/health`

## 🏗 Architecture

### Core Components

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controllers   │    │    Services     │    │     Models      │
│                 │    │                 │    │                 │
│ OrderController │───▶│ StatisticsService│───▶│ StatisticsModule│
│StatisticsController│ │                 │    │ InterimStatistics│
└─────────────────┘    └─────────────────┘    │ Order           │
                                              └─────────────────┘
```

### Key Classes

- **`OrderController`**: Handles HTTP requests for order operations
- **`StatisticsController`**: Provides real-time statistics endpoints
- **`StatisticsModule`**: Thread-safe statistics aggregation using atomic operations
- **`InterimStatistics`**: Millisecond-level aggregated statistics

### Thread Safety

The application uses several concurrency mechanisms:

- **`AtomicReferenceArray`**: For thread-safe statistics storage
- **`AtomicReference`**: For atomic BigDecimal operations
- **`synchronized` blocks**: For critical sections in statistics updates

## 🧪 Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ConcurrencyTest

# Run tests with coverage
mvn test jacoco:report
```

### Test Categories

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test API endpoints with Spring context
- **Concurrency & Performance Tests**: Validate thread-safety under high load

### Example Test

```java

@Test
void testConcurrentOrderSubmission() throws InterruptedException {
    int threadCount = 100;
    // Submit 100 orders concurrently and verify statistics
}
```

## 📊 Monitoring

### Actuator Endpoints

- `/actuator/health` - Application health status
- `/actuator/metrics` - JVM and application metrics
- `/actuator/metrics/http.server.requests` - HTTP request metrics
- `/actuator/info` - Application information

### Key Metrics

- **Request throughput**: `http.server.requests`
- **Response times**: Percentiles (50th, 95th, 99th)
- **JVM metrics**: Memory usage, garbage collection
- **Custom metrics**: Statistics calculation performance

### Example Monitoring Query

```bash
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=method:POST"
```

### Benchmarks

| Operation                     | Throughput   | Response Time (P95) |
|-------------------------------|--------------|---------------------|
| Add Order                     | 10,000 req/s | ??                  |
| Get Statistics                | 50,000 req/s | ??                  |
| Concurrent Load (100 threads) | 8,000 req/s  | ??                  |
