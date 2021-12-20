package com.netbull.apiclient.listener;

import com.netbull.apiclient.domain.store.Product;
import com.netbull.apiclient.domain.store.ProductRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProductListener {

    @Autowired
    ProductRepository productRepository;

    @RabbitListener(queues = "product-created")
    public void executeCreate(Product product) {
        productRepository.save(product);
    }

    @RabbitListener(queues = "product-updated")
    public void executeUpdate(Product product) {
        Product otherProduct = productRepository.findById(product.getId()).get();
        otherProduct.setQuantity(product.getQuantity());
        otherProduct.setCode(product.getCode());
        otherProduct.setPrice(product.getPrice());
        productRepository.save(otherProduct);
    }

    @RabbitListener(queues = "product-deleted")
    public void executeDelete(Product product) {
        productRepository.delete(product);
    }
}
