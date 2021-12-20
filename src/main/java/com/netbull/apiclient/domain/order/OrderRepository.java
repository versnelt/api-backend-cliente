package com.netbull.apiclient.domain.order;

import com.netbull.apiclient.domain.client.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface OrderRepository extends PagingAndSortingRepository<Order, BigInteger> {

    public Optional<Set<Order>> findOrdersByClient(Client client);

    public default Page<Order> findOrdersPageByClient(Pageable pageable, Client client) {
        List<Order> products = this.findOrdersByClient(client).orElse(new HashSet<Order>())
                .stream()
                .collect(Collectors.toList());
        return new PageImpl<Order>(products, pageable, products.size());
    }
}
