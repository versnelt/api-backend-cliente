package com.netbull.apiclient.listener;

import com.netbull.apiclient.domain.order.Order;
import com.netbull.apiclient.domain.order.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderListener {

    @Autowired
    OrderRepository orderRepository;

    @RabbitListener(queues = "order-client-updated-dispatched")
    public void executeUpdate(Order order) {
        Order otherOrder = orderRepository.findById(order.getId()).get();
        otherOrder.setState(order.getState());
        otherOrder.setOrderDispatched(order.getOrderDispatched());
        if (this.orderRepository.save(otherOrder) != null) {
            log.info("Pedido alterado: {}", order.getState());
        }
    }
}
