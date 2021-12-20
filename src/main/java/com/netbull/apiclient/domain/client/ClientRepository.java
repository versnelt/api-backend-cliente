package com.netbull.apiclient.domain.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Optional;

@Repository
public interface ClientRepository extends PagingAndSortingRepository<Client, BigInteger> {

    public Optional<Client> findByEmail(String email);
    public Optional<Client> findByCpf(String cpf);
    public Page<Client> findAll(Pageable pageable);
}
