package com.netbull.apiclient.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductQueueRabbitConfig {
    @Bean
    public Exchange productExchange() {
        return ExchangeBuilder
                .directExchange("product")
                .build();
    }


    @Bean
    public Queue productCreatedQueue() {
        return QueueBuilder
                .durable("product-created")
                .deadLetterExchange("product")
                .deadLetterRoutingKey("product.deadLetter")
                .deliveryLimit(5)
                .build();
    }

    @Bean
    public Queue productUpdatedQueue() {
        return QueueBuilder
                .durable("product-updated")
                .deadLetterExchange("product")
                .deadLetterRoutingKey("product.deadLetter")
                .deliveryLimit(5)
                .build();
    }

    @Bean
    public Queue productDeletedQueue() {
        return QueueBuilder
                .durable("product-deleted")
                .deadLetterExchange("product")
                .deadLetterRoutingKey("product.deadLetter")
                .deliveryLimit(5)
                .build();
    }

    @Bean
    public Binding productCreatedBiding() {
        return BindingBuilder
                .bind(this.productCreatedQueue())
                .to(this.productExchange())
                .with("product.created")
                .noargs();
    }

    @Bean
    public Binding productUpdatedBiding() {
        return BindingBuilder
                .bind(this.productUpdatedQueue())
                .to(this.productExchange())
                .with("product.updated")
                .noargs();
    }

    @Bean
    public Binding productDeletedBiding() {
        return BindingBuilder
                .bind(this.productDeletedQueue())
                .to(this.productExchange())
                .with("product.deleted")
                .noargs();
    }

    @Bean
    public Queue productDeadLetterQueue() {
        return QueueBuilder
                .durable("product-dead-letter")
                .autoDelete()
                .build();
    }

    @Bean
    public Binding productDeadLetterBiding() {
        return BindingBuilder
                .bind(this.productDeadLetterQueue())
                .to(this.productExchange())
                .with("product.deadLetter")
                .noargs();
    }
}
