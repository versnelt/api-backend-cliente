package com.netbull.apiclient.domain.store;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProductRepository extends CrudRepository<Product, BigInteger> {

    public Optional<Set<Product>> findProductsByStore(Store store);
    public default Optional<Product> findProductByCodeAndStore(String code, Store store) {
        Optional<Set<Product>> productsOfStore = this.findProductsByStore(store);
        for(Product product : productsOfStore.get()) {
            if(product.getCode().equals(code)){
                return Optional.of(product);
            }
        }
        return Optional.empty();
    }
}
