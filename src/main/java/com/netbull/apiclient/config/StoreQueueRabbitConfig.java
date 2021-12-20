package com.netbull.apiclient.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StoreQueueRabbitConfig {
    @Bean
    public Exchange storeExchange() {
        return ExchangeBuilder
                .directExchange("store")
                .build();
    }


    @Bean
    public Queue storeCreatedQueue() {
        return QueueBuilder
                .durable("store-created")
                .deadLetterExchange("store")
                .deadLetterRoutingKey("store.deadLetter")
                .deliveryLimit(5)
                .build();
    }

    @Bean
    public Queue storeUpdatedQueue() {
        return QueueBuilder
                .durable("store-updated")
                .deadLetterExchange("store")
                .deadLetterRoutingKey("store.deadLetter")
                .deliveryLimit(5)
                .build();
    }

    @Bean
    public Queue storeDeletedQueue() {
        return QueueBuilder
                .durable("store-deleted")
                .deadLetterExchange("store")
                .deadLetterRoutingKey("store.deadLetter")
                .deliveryLimit(5)
                .build();
    }

    @Bean
    public Binding storeCreatedBiding() {
        return BindingBuilder
                .bind(this.storeCreatedQueue())
                .to(this.storeExchange())
                .with("store.created")
                .noargs();
    }

    @Bean
    public Binding storeUpdatedBiding() {
        return BindingBuilder
                .bind(this.storeUpdatedQueue())
                .to(this.storeExchange())
                .with("store.updated")
                .noargs();
    }

    @Bean
    public Binding storeDeletedBiding() {
        return BindingBuilder
                .bind(this.storeDeletedQueue())
                .to(this.storeExchange())
                .with("store.deleted")
                .noargs();
    }

    @Bean
    public Queue storeDeadLetterQueue() {
        return QueueBuilder
                .durable("store-dead-letter")
                .autoDelete()
                .build();
    }

    @Bean
    public Binding storeDeadLetterBiding() {
        return BindingBuilder
                .bind(this.storeDeadLetterQueue())
                .to(this.storeExchange())
                .with("store.deadLetter")
                .noargs();
    }
}
