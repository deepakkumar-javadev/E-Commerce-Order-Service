package com.deepak.orderService.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.deepak.orderService.kafka.OrderReservedEvent;
import com.deepak.orderService.kafka.PaymentSuccessEvent;

@Configuration
public class kafkaConfig {

    // =====================================================
    // 1. PAYMENT SUCCESS CONSUMER
    // =====================================================

    @Bean
    public ConsumerFactory<String, PaymentSuccessEvent> paymentConsumerFactory() {

        JsonDeserializer<PaymentSuccessEvent> deserializer =
                new JsonDeserializer<>(PaymentSuccessEvent.class);

        deserializer.addTrustedPackages(
                "com.deepak.orderService.kafka"
        );

        Map<String, Object> props = new HashMap<>();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );

        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "order-service"
        );

        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentSuccessEvent>
    paymentKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, PaymentSuccessEvent>
                factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(paymentConsumerFactory());

        return factory;
    }


    // =====================================================
    // 2. INVENTORY RESERVED CONSUMER
    // =====================================================

    @Bean
    public ConsumerFactory<String, OrderReservedEvent>
    inventoryConsumerFactory() {

        JsonDeserializer<OrderReservedEvent> deserializer =
                new JsonDeserializer<>(OrderReservedEvent.class);

        // Order Service ke DTO ko trust karo
        deserializer.addTrustedPackages(
                "com.deepak.orderService.kafka"
        );

        // Inventory Service ke type header ko ignore karo
        // JSON ko OrderReservedEvent.class mein deserialize karo
        deserializer.setUseTypeHeaders(false);

        Map<String, Object> props = new HashMap<>();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );

        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "order-service"
        );

        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderReservedEvent>
    inventoryKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, OrderReservedEvent>
                factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(inventoryConsumerFactory());

        return factory;
    }
}