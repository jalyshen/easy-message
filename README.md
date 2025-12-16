# Easy Message - Azure Event Hubs Starter

Easy Message is a lightweight Spring Boot Starter designed specifically for Azure Event Hubs.

Inspired by the design philosophy of Eventuate Tram, it aims to simplify message production and consumption in distributed systems. By wrapping the native Azure SDK, it provides out-of-the-box features such as multi-instance routing, lifecycle management, distributed tracing, and declarative consumers.

## ✨ Key Features
- 📦 Ready to Use: Simply add the Maven dependency and configure your YAML to connect to Azure Event Hubs immediately.
- 🔀 Smart Routing: Supports mapping logical Destinations to physical Event Hub names, allowing seamless routing to multiple Event Hub instances.
- 🎧 Declarative Consumption: Subscribe to messages easily using the @MessageListener annotation, eliminating the need for boilerplate Client construction code.
- 💾 Reliable Checkpointing: Integrated with Azure Blob Storage to automatically manage consumer offsets, ensuring At-least-once delivery guarantees.
- 🔍 Distributed Tracing: Built-in interceptor mechanism that automatically injects/extracts Trace Contexts, making it easy to integrate with observability systems.
- 🚀 Lifecycle Management: Leveraging Spring's SmartLifecycle, it ensures consumers start processing messages only after the Spring container is fully initialized.

## 🏗 Architecture
Easy Message adopts a layered architecture to decouple core abstractions from the underlying implementation.

### UML Class Diagram
![class_diagram](./class.png)

## 🛠 Quick Start

### 1. Prerequisites
- Java: 17+
- Spring Boot: 3.x
- Azure Resources:
- Event Hubs Namespace (with Event Hubs created)
- Storage Account (for Checkpointing)

### 2. Add Dependency
Add the following to your project's pom.xml:
```xml
<dependency>
    <groupId>com.easy.messaging</groupId>
    <artifactId>message-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 3. Configuration (application.yml)
This is the most critical step. You need to configure the Checkpoint storage and your specific business Event Hub instances.
```yaml
message:
  # [Required] Checkpoint Storage Configuration (Azure Blob Storage)
  checkpoint-connection-string: "DefaultEndpointsProtocol=https;AccountName=your_storage;AccountKey=...;EndpointSuffix=core.windows.net"
  checkpoint-container-name: "checkpoints" # The container will be created automatically

  # [Optional] Default Logic Mappings
  defaults:
    destinations:
      # Logical Name -> Physical Event Hub Name
      create-order: "orders-hub"
      send-email: "notifications-hub"

  # [Core] Event Hub Instance Configuration
  instances:
    # Business Module 1: Orders
    orders:
      enabled: true
      event-hub-name: "orders-hub"
      producer-connector:
        enabled: true
        connection-string: "Endpoint=sb://...;SharedAccessKeyName=...;SharedAccessKey=..."
      consumer-connector:
        enabled: true
        connection-string: "Endpoint=sb://...;SharedAccessKeyName=...;SharedAccessKey=..."
        consumer-group: "$Default" # Defaults to $Default

    # Business Module 2: Notifications
    notifications:
      enabled: true
      event-hub-name: "notifications-hub"
      # This module only consumes messages, does not produce
      consumer-connector:
        enabled: true
        connection-string: "Endpoint=sb://...;..."
```

## 💻 Usage Guide
### 1. Sending Messages (Producer)
Inject the MessageProducer interface and use MessageBuilder to construct your message.
```java
@RestController
public class OrderController {

    private final MessageProducer messageProducer;

    public OrderController(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @PostMapping("/orders")
    public String createOrder(@RequestBody String orderJson) {
        // Build the message
        Message message = MessageBuilder.withPayload(orderJson)
                .withHeader(Message.ID, UUID.randomUUID().toString())
                .withHeader("custom-header", "value")
                .build();

        // Send the message
        // "create-order" is the logical name, which resolves to "orders-hub" based on config
        messageProducer.send("create-order", message);

        return "Order Sent";
    }
}
```
### 2. Receiving Messages (Consumer)
Add the @MessageListener annotation to any method within a Spring Bean (@Component/@Service).

⚠️ <b>Note:</b> The method parameter must be of type com.easy.messaging.core.message.Message.
```java
@Component
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @MessageListener(destination = "create-order", subscriberId = "order-service-consumer")
    public void handleOrderCreation(Message message) {
        String payload = message.getPayload();
        String msgId = message.getId();
        
        log.info("Received Order: ID={}, Payload={}", msgId, payload);
        
        // Process business logic...
    }
}
```

## 🧩 Advanced Features
### Distributed Tracing
Enable via message.tracing-enabled: true (default is true).
When a Producer sends a message, traceId and spanId are automatically generated or propagated in the Headers. Consumers can retrieve the context via message.getHeader("traceId").

### Interceptors
You can implement the MessageInterceptor interface and register it as a Bean. The framework will automatically add it to the interception chain.
```java
@Component
public class LoggingInterceptor implements MessageInterceptor {
    @Override
    public void preSend(Message message) {
        System.out.println("Before Sending: " + message.getPayload());
    }
    
    @Override
    public void postHandle(String subscriberId, Message message, Throwable t) {
        if (t != null) {
            System.err.println("Handling failed: " + t.getMessage());
        }
    }
}
```

## ⚙️ Build & Development
This project is a multi-module Maven project:

- message-core: Core interfaces and abstractions.
- message-eventhub: Concrete implementation for Azure Event Hub SDK.
- message-autoconfigure: Spring Boot auto-configuration logic.
- message-starter: Aggregator module for end-users.

### Local Installation:
```shell
mvn clean install
```