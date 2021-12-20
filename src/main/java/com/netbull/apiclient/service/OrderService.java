package com.netbull.apiclient.service;

import com.netbull.apiclient.domain.address.Address;
import com.netbull.apiclient.domain.client.Client;
import com.netbull.apiclient.domain.order.*;
import com.netbull.apiclient.domain.store.Product;
import com.netbull.apiclient.domain.store.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.ws.rs.NotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class OrderService {

    OrderRepository orderRepository;

    ClientService clientService;

    ProductOrderRepository productOrderRepository;

    ProductRepository productRepository;

    AddressService addressService;

    Validator validator;

    private RabbitTemplate rabbitTemplate;

    public OrderService(OrderRepository orderRepository, ClientService clientService,
                        AddressService addressService, ProductOrderRepository productOrderRepository,
                        ProductRepository productRepository, Validator validator,
                        RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.clientService = clientService;
        this.addressService = addressService;
        this.productOrderRepository = productOrderRepository;
        this.productRepository = productRepository;
        this.validator = validator;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void persistOrder(@NotNull(message = "O pedido não pode ser nulo.") Order order,
                             String userEmail) {
        Optional.ofNullable(order).orElseThrow(
                () -> new IllegalArgumentException("O pedido não pode ser nulo."));

        if (order.getProducts() != null) {
            validateProducts(order);
            order.setTotalValue(BigDecimal.ZERO);

            order.getProducts().forEach(
                    product -> order.setTotalValue(
                            order.getTotalValue()
                                    .add(product.getPrice().multiply(
                                            BigDecimal.valueOf(product.getQuantity().intValue())))));
        }

        if (order.getAddress() == null && addressService.getAddressByClientEmail(userEmail).size() == 1) {
            order.setAddress(addressService.getAddressByClientEmail(userEmail)
                    .stream()
                    .findFirst()
                    .get());
        } else if (order.getAddress() == null && addressService.getAddressByClientEmail(userEmail).size() > 1) {
            throw new IllegalArgumentException("O cliente possui mais de um endereço cadastrado, por favor especifique o " +
                    "endereço de envio no pedido.");
        } else if (order.getAddress() != null && order.getAddress().getId() != null) {
            Address address = addressService.getAddressById(order.getAddress().getId());
            if (address.getClient().getEmail().equals(userEmail)) {
                order.setAddress(address);
            }
        } else {
            order.setAddress(null);
        }

        order.setClient(clientService.getClientByEmail(userEmail));
        order.setOrderCreated(LocalDate.now());
        order.setState(OrderState.CRIADO);

        Set<ConstraintViolation<Order>> validateOrder = this.validator.validate(order);

        if (!validateOrder.isEmpty()) {
            throw new ConstraintViolationException("Pedido inválido.", validateOrder);
        }

        for (ProductOrder productOrder : order.getProducts()) {
            Product product = productRepository.findProductByCodeAndStore(productOrder.getCode(), order.getStore()).get();
            product.setQuantity(product.getQuantity().subtract(productOrder.getQuantity()));
            productRepository.save(product);
        }

        if (this.orderRepository.save(order) != null) {
            log.info("Pedido criado: {}", order.getId());
        }
        productOrderRepository.saveAll(order.getProducts());
        this.rabbitTemplate.convertAndSend("order-store", "order.store.created", order);
    }

    @Transactional
    public void setOrderStateToDelivered(BigInteger id, String userEmail, OrderState orderState) {
        Order order = orderRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Nenhum pedido foi encontrado com o id: " + id + "."));

        if(order.getState().equals(OrderState.ENTREGUE)) {
            throw new IllegalArgumentException("O pedido já foi entregue na data: " +
                    order.getOrderDelivered().format(DateTimeFormatter.ofPattern("dd/MM/YYYY")));
        }

        if(!orderState.equals(OrderState.ENTREGUE)) {
            throw new IllegalArgumentException("Somente é possível alterar o estado do pedido para: ENTREGUE.");
        }

        if(order.getState().equals(OrderState.CRIADO)) {
            throw new IllegalArgumentException("Não é possível alterar o estado para ENTREGUE antes do pedido ser enviado.");
        }

        if (!order.getClient().getEmail().equals(userEmail)) {
            throw new NotFoundException("Nenhum pedido foi encontrado com o id: " + id + ".");
        }

        order.setOrderDelivered(LocalDate.now());
        order.setState(OrderState.ENTREGUE);

        if (this.orderRepository.save(order) != null) {
            log.info("Pedido alterado: {}", order.getState());
        }
        this.rabbitTemplate.convertAndSend("order-store", "order.store.updated.delivered", order);
    }

    public Order getOrderById(BigInteger id, String userEmail) {
        Order order = orderRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Nenhum pedido foi encontrado com o id: " + id + "."));

        if (!userEmail.equals(order.getClient().getEmail())) {
            throw new NotFoundException("Nenhum pedido foi encontrado com o id: " + id + ".");
        }

        return order;
    }

    public Page<Order> getOrdersPageByClient(Pageable pageable, String userEmail) {
        Client client = clientService.getClientByEmail(userEmail);

        Page<Order> ordersPage = orderRepository.findOrdersPageByClient(pageable, client);

        if (ordersPage.isEmpty()) {
            throw new NotFoundException("Nenhum pedido foi encontrado.");
        }

        return ordersPage;
    }

    private void validateProducts(Order order) {
        int x = 0;
        Product lastProduct = new Product();
        List<ProductOrder> listProductsOrder = new ArrayList<>();
        Set<ConstraintViolation<ProductOrder>> validateProducts = null;

        for (ProductOrder productOrder : order.getProducts()) {

            validateProducts = this.validator.validate(productOrder);

            if (!validateProducts.isEmpty()) {
                throw new ConstraintViolationException("Produto inválido.", validateProducts);
            }

            Product product = productRepository.findProductByCodeAndStore(productOrder.getCode(), order.getStore())
                    .orElseThrow(
                            () -> new NotFoundException("Produto não encontrado.")
                    );

            if (product.getQuantity().compareTo(productOrder.getQuantity()) < 0) {
                throw new IllegalArgumentException("Não há quantidade disponível suficiente para o produto código: " +
                        product.getCode() + ", somente há disponível: " + product.getQuantity() + " ítens.");
            }

            if (x > 0 && product.equals(lastProduct)) {
                throw new IllegalArgumentException("Há produtos repetidos.");
            }

            if (x % 2 == 0) {
                lastProduct = product;
            }

            productOrder.setPrice(product.getPrice());
            productOrder.setOrder(order);
            listProductsOrder.add(productOrder);

            x++;
        }
        order.setProducts(listProductsOrder);
    }
}
