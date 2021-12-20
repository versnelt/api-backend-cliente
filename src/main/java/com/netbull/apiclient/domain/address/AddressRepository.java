package com.netbull.apiclient.domain.address;

import com.netbull.apiclient.domain.client.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AddressRepository extends JpaRepository<Address, BigInteger> {

    public Optional<Set<Address>> findByClient(Client client);
}
