package com.netbull.apiclient.domain.store;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;

@Repository
public interface StoreRepository extends PagingAndSortingRepository<Store, BigInteger> {

    public Page<Store> findAll(Pageable pageable);
}
