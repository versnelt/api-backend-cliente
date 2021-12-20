package com.netbull.apiclient.listener;

import com.netbull.apiclient.domain.store.ProductRepository;
import com.netbull.apiclient.domain.store.Store;
import com.netbull.apiclient.domain.store.StoreRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StoreListener {

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    ProductRepository productRepository;

    @RabbitListener(queues = "store-created")
    public void executeCreate(Store store) {

        storeRepository.save(store);
    }

    @RabbitListener(queues = "store-updated")
    public void executeUpdate(Store store) {
        Store otherStore = storeRepository.findById(store.getId()).get();
        otherStore.setCnpj(store.getCnpj());
        storeRepository.save(otherStore);
    }

    @RabbitListener(queues = "store-deleted")
    public void executeDelete(Store store) {
        productRepository.findProductsByStore(store).ifPresent(
                setProducts -> setProducts.forEach(productRepository::delete));
        storeRepository.delete(store);
    }
}
