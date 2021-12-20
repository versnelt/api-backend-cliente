package com.netbull.apiclient.domain.order;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Optional;

@Repository
public interface ProductOrderRepository extends CrudRepository<ProductOrder, BigInteger> {
    public Optional<ProductOrder> findProductOrderByCode(String code);
}
