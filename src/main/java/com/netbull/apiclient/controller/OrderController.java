package com.netbull.apiclient.controller;

import com.netbull.apiclient.domain.order.Order;
import com.netbull.apiclient.domain.order.OrderState;
import com.netbull.apiclient.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.net.URI;

@RestController
@Controller
@Slf4j
@RequestMapping(path = "/v1/clients/orders")
public class OrderController {

    @Autowired
    OrderService orderService;

    @Operation(summary = "Criar um pedido.")
    @PostMapping(produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> createPedido(@RequestBody Order order) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        this.orderService.persistOrder(order, auth.getName());

        URI uri = MvcUriComponentsBuilder.fromController(getClass())
                .path("/{id}")
                .buildAndExpand(order.getId())
                .toUri();

        return ResponseEntity.created(uri).body("Pedido criado.");
    }

    @Operation(summary = "Buscar todos os pedidos do cliente.")
    @GetMapping( produces = {MediaType.APPLICATION_JSON})
    public ResponseEntity<Page<Order>> getAllOrders(
            @ParameterObject @PageableDefault(sort = {"id"}, direction = Sort.Direction.ASC,
            page = 0, size = 10) Pageable pageable) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Page<Order> order = orderService.getOrdersPageByClient(pageable, auth.getName());

        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Buscar um pedido pelo id.")
    @GetMapping(path = "/{id}", produces = {MediaType.APPLICATION_JSON})
    public ResponseEntity<Order> getOrderById(@PathVariable BigInteger id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Order order = orderService.getOrderById(id, auth.getName());
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Alterar estado do pedido para entregue.")
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> patchAddressType(@PathVariable BigInteger id, @RequestBody Order order) {
        OrderState orderState = order.getState();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        this.orderService.setOrderStateToDelivered(id, auth.getName(), orderState);

        return ResponseEntity.ok("Pedido alterado para entregue.");

    }
}
