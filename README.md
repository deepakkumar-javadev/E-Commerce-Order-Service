# E-Commerce Order Service

Order Service is a Spring Boot microservice responsible for creating and managing customer orders in the e-commerce application.

It handles the order lifecycle, communicates with Cart, Inventory, and Payment services, and uses Kafka for asynchronous event-driven communication.

## Features

* Create customer orders
* Fetch order details
* Track order status
* Manage order lifecycle
* Cart and Inventory service integration
* Payment service integration
* JWT-based authentication
* Kafka-based asynchronous communication
* MySQL database
* REST APIs
* Service discovery with Eureka

## Tech Stack

* Java 17
* Spring Boot
* Spring Data JPA
* Spring Cloud OpenFeign
* Spring Security
* JWT
* Apache Kafka
* MySQL
* Maven
* Eureka Service Discovery

## Microservice Communication

```text id="9zkn3a"
                     ┌─────────────────┐
                     │   API Gateway   │
                     └────────┬────────┘
                              │
                              ↓
                     ┌─────────────────┐
                     │  Order Service  │
                     │      :8086      │
                     └───┬────┬────┬───┘
                         │    │    │
                ┌────────┘    │    └────────┐
                ↓             ↓             ↓
        ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
        │Cart Service │ │  Inventory  │ │   Payment   │
        │    :8085    │ │   Service   │ │   Service   │
        └─────────────┘ │    :8084    │ │    :8087    │
                        └─────────────┘ └─────────────┘
                             
                         Apache Kafka
                              │
              ┌───────────────┼────────────────┐
              ↓               ↓                ↓
        Inventory Events  Payment Events   Order Events
```

## Order Creation Flow

```text id="k2r8ne"
Customer
   ↓
Order Service
   ↓
Get Cart Items
   ↓
Validate Inventory
   ↓
Create Order
   ↓
Publish Order Created Event
   ↓
Inventory Reservation
   ↓
Payment Processing
   ↓
Order Confirmation
```

## Service Communication

### Synchronous Communication

Order Service uses **Feign Client** to communicate with:

* Cart Service
* Inventory Service
* Payment Service

### Asynchronous Communication

Apache Kafka is used for event-driven communication.

Important events include:

* `order.created`
* `inventory.reserve`
* `inventory.reserved`
* `inventory.commit`
* `payment.success`
* `order.delivered`

This helps decouple services and supports asynchronous processing of order-related operations.

## Order Lifecycle

```text id="8m3q1v"
PENDING
   ↓
CONFIRMED
   ↓
SHIPPED
   ↓
DELIVERED
```

If an order cannot be processed successfully, it can move to an appropriate failure or cancellation state.

## Payment Support

The Order Service supports:

* Cash on Delivery (COD)
* Online Payment

For online payments, the order is processed with the Payment Service and updated after successful payment confirmation.

## Database

Order Service uses MySQL for persistent order data.

```text id="e8k4z9"
Database: ecomorderdb
```

Main entities:

* `Order`
* `OrderItem`

## Security

The service uses Spring Security and JWT authentication to secure customer-facing APIs.

Service-to-service communication is also secured using internal authentication mechanisms.

## Service Port

```text id="k7x1md"
Order Service: 8086
```

## Project Structure

```text id="f2q6yp"
src/main/java/com/deepak/orderService
│
├── Dto
├── Repository
├── config
├── controller
├── entity
├── feignClients
├── kafka
│   └── config
├── security
└── service
```

## Running the Service

Make sure the required infrastructure and dependent services are running.

Then start the application using:

```bash id="2b4r8x"
mvn spring-boot:run
```

Or run the main Spring Boot application from your IDE.

## Role in E-Commerce System

Order Service acts as the central component for managing the order lifecycle.

```text id="d8q2ws"
Customer
   ↓
Cart
   ↓
Order Service
   ├── Inventory
   ├── Payment
   └── Notification
          ↓
      Order Tracking
```

The service coordinates order creation, inventory reservation, payment processing, and order status updates using synchronous APIs and Kafka events.
