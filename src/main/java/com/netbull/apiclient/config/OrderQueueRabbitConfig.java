package com.netbull.apiclient.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderQueueRabbitConfig {
    @Bean
    public Exchange orderExchange() {
        return ExchangeBuilder
                .directExchange("order-client")
                .build();
    }

    @Bean
    public Queue orderUpdatedDispatchedQueue() {
        return QueueBuilder
                .durable("order-client-updated-dispatched")
                .deadLetterExchange("order-client")
                .deadLetterRoutingKey("order.client.deadLetter")
                .deliveryLimit(5)
                .build();
    }

    @Bean
    public Binding orderUpdatedDispatchedBiding() {
        return BindingBuilder
                .bind(this.orderUpdatedDispatchedQueue())
                .to(this.orderExchange())
                .with("order.client.updated.dispatched")
                .noargs();
    }

    @Bean
    public Queue orderDeadLetterQueue() {
        return QueueBuilder
                .durable("order-client-dead-letter")
                .autoDelete()
                .build();
    }

    @Bean
    public Binding orderDeadLetterBiding() {
        return BindingBuilder
                .bind(this.orderDeadLetterQueue())
                .to(this.orderExchange())
                .with("order.client.deadLetter")
                .noargs();
    }
}