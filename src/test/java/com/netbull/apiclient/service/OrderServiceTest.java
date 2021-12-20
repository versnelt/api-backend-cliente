package com.netbull.apiclient.service;

import com.netbull.apiclient.domain.address.Address;
import com.netbull.apiclient.domain.client.Client;
import com.netbull.apiclient.domain.order.Order;
import com.netbull.apiclient.domain.order.*;
import com.netbull.apiclient.domain.store.Product;
import com.netbull.apiclient.domain.store.ProductRepository;
import com.netbull.apiclient.domain.store.Store;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import javax.validation.*;
import javax.ws.rs.NotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderServiceTest {
    private OrderService orderService;

    private OrderRepository orderRepository;

    private ProductOrderRepository productOrderRepository;

    ProductRepository productRepository;

    private ClientService clientService;

    private AddressService addressService;

    private RabbitTemplate rabbitTemplate;

    private Validator validation;

    private Pageable pageable;

    private

    Product product = new Product();

    Product product2 = new Product();


    @BeforeAll
    public void setupBeforAll() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        this.validation = validatorFactory.getValidator();
    }

    @BeforeEach
    public void setupBeforEach() {
        this.orderRepository = Mockito.mock(OrderRepository.class);
        this.productOrderRepository = Mockito.mock(ProductOrderRepository.class);
        this.pageable = Mockito.mock(Pageable.class);
        this.clientService = Mockito.mock(ClientService.class);
        this.addressService = Mockito.mock(AddressService.class);
        this.productRepository = Mockito.mock(ProductRepository.class);
        this.rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        this.orderService = new OrderService(orderRepository, clientService,
                addressService, productOrderRepository, productRepository,
                validation, rabbitTemplate);

        this.product = new Product();
        this.product.setPrice(BigDecimal.TEN);
        this.product.setCode("1");
        this.product.setQuantity(BigInteger.TEN);

        this.product2 = new Product();
        this.product2.setPrice(BigDecimal.TEN);
        this.product2.setCode("2");
        this.product2.setQuantity(BigInteger.TEN);
    }

    @Test
    @DisplayName("Testa persistir quando o pedido for nulo.")
    public void test_persistirOrderQuandoNulo_lancaException() {
        assertNotNull(orderService);

        Order order = null;

        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> orderService.persistOrder(order, ""));

        assertNotNull(assertThrows);
        assertEquals("O pedido não pode ser nulo.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa persistir pedido quando todos os atributos obrigatórios são nulos.")
    public void test_persistOrderQuandoAtributosObrigatoriosNulos_lancaException() {
        assertNotNull(orderService);

        Order order = new Order();

        when(clientService.getClientByEmail(any())).thenReturn(null);
        var assertThrows = assertThrows(ConstraintViolationException.class,
                () -> orderService.persistOrder(order, ""));

        List<String> messages = assertThrows.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertEquals(5, assertThrows.getConstraintViolations().size());

        assertThat(messages, hasItems(
                "A loja não pode ser nula.",
                "O valor total não pode ser nulo.",
                "O endereço do cliente não pode ser nulo.",
                "O cliente não pode ser nulo.",
                "Os produtos não podem ser nulos."
        ));
    }

    @Test
    @DisplayName("Testa persistir quando não encontra produto.")
    public void test_persistirQuandoProdutoNaoEncontrado_lancaException() {
        assertNotNull(orderService);

        ProductOrder productOrder = new ProductOrder();
        productOrder.setCode("1");
        productOrder.setQuantity(BigInteger.ONE);

        Order order = new Order();
        order.setProducts(List.of(productOrder));
        order.setAddress(new Address());

        Client client = new Client();
        client.setEmail("a@A");
        client.setName("cris");

        when(productRepository.findProductByCodeAndStore(any(), any())).thenReturn(Optional.empty());

        var assertThrows = assertThrows(NotFoundException.class,
                () -> orderService.persistOrder(order, client.getEmail()));

        assertEquals("Produto não encontrado.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa persistir quando há produtos repetidos.")
    public void test_persistirQuandoHaProdutosRepetidos_lancaException() {
        assertNotNull(orderService);

        Store store = new Store();
        store.setCnpj("11111111111111");
        product.setStore(store);

        ProductOrder productOrder = new ProductOrder();
        productOrder.setCode("1");
        productOrder.setQuantity(BigInteger.ONE);

        Order order = new Order();
        order.setProducts(List.of(productOrder, productOrder));
        order.setAddress(new Address());
        order.setStore(store);

        Client client = new Client();
        client.setEmail("a@A");
        client.setName("cris");

        when(productRepository.findProductByCodeAndStore("1", store)).thenReturn(Optional.of(product));

        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> orderService.persistOrder(order, client.getEmail()));

        assertEquals("Há produtos repetidos.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa persistir quando não envia campos obrigatórios do produto.")
    public void test_persistirQuandoNaoEnviaCamposObrigatoriosDosProdutos_lancaException() {
        assertNotNull(orderService);

        ProductOrder productOrder = new ProductOrder();

        Order order = new Order();
        order.setProducts(List.of(productOrder));
        order.setAddress(new Address());

        Client client = new Client();
        client.setEmail("a@A");
        client.setName("cris");

        var assertThrows = assertThrows(ConstraintViolationException.class,
                () -> orderService.persistOrder(order, client.getEmail()));

        List<String> messages = assertThrows.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages, hasItems(
                "Quantidade não pode ser nula.",
                "Código não pode ser nulo."
        ));

        productOrder.setCode("1");
        productOrder.setQuantity(BigInteger.valueOf(-1));

        var assertThrows2 = assertThrows(ConstraintViolationException.class,
                () -> orderService.persistOrder(order, client.getEmail()));

        List<String> messages2 = assertThrows2.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages2, hasItems(
                "Quantidade não pode ser menor que um."
        ));
    }

    @Test
    @DisplayName("Testa persistir quando o quantidade do produto é maior do que a disponível.")
    public void test_persistirQuandoQuantidadeMaiorQueDisponivel_lancaException() {
        assertNotNull(orderService);

        Store store = new Store();
        store.setCnpj("11111111111111");
        product.setStore(store);

        ProductOrder productOrder = new ProductOrder();
        productOrder.setCode("1");
        productOrder.setQuantity(BigInteger.valueOf(100));

        Order order = new Order();
        order.setProducts(List.of(productOrder, productOrder));
        order.setAddress(new Address());
        order.setStore(store);

        Client client = new Client();
        client.setEmail("a@A");
        client.setName("cris");

        when(productRepository.findProductByCodeAndStore("1", store)).thenReturn(Optional.of(product));

        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> orderService.persistOrder(order, client.getEmail()));

        assertEquals("Não há quantidade disponível suficiente para o produto código: " +
        product.getCode() + ", somente há disponível: " + product.getQuantity() + " ítens.",
                assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa persistir pedido quando cliente possui mais de um endereço cadastrado.")
    public void test_persistOrderQuandoEndereçoENuloEClientePossuiSomenteUmEndereco_lancaException() {
        assertNotNull(orderService);

        Store store = new Store();
        store.setCnpj("11111111111111");
        product.setStore(store);

        ProductOrder productOrder = new ProductOrder();
        productOrder.setCode("1");
        productOrder.setQuantity(BigInteger.ONE);

        Order order = new Order();
        order.setProducts(List.of(productOrder));

        when(addressService.getAddressByClientEmail(any())).thenReturn(Set.of(new Address(), new Address()));
        when(productRepository.findProductByCodeAndStore(any(), any())).thenReturn(Optional.of(product));

        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> orderService.persistOrder(order, ""));

        assertEquals("O cliente possui mais de um endereço cadastrado, por favor especifique o " +
                "endereço de envio no pedido.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa persistir e atribuição automática dos campos: Cliente, data de criação," +
            " valor total e estado do pedido.")
    public void test_atribuicaoAutmaticaDeData_Cliente_Valor_Total_Loja() {
        assertNotNull(orderService);

        BigInteger quantityProduct1 = product.getQuantity();
        BigInteger quantityProduct2 = product2.getQuantity();

        Store store = new Store();
        store.setCnpj("11111111111111");

        ProductOrder productOrder = new ProductOrder();
        productOrder.setCode("1");;
        productOrder.setQuantity(BigInteger.ONE);

        ProductOrder productOrder2 = new ProductOrder();
        productOrder2.setCode("2");
        productOrder2.setQuantity(BigInteger.TWO);

        product.setStore(store);
        product2.setStore(store);

        Client client = new Client();
        client.setEmail("a@A");
        client.setName("cris");

        Address address = new Address();
        address.setId(BigInteger.ONE);
        address.setClient(client);
        Set<Address> addresses = Set.of(address);

        Order order = new Order();
        order.setProducts(List.of(productOrder, productOrder2));
        order.setStore(store);

        when(addressService.getAddressByClientEmail(any())).thenReturn(addresses);
        when(clientService.getClientByEmail(any())).thenReturn(client);
        when(productRepository.findProductByCodeAndStore("1", store)).thenReturn(Optional.of(product));
        when(productRepository.findProductByCodeAndStore("2", store)).thenReturn(Optional.of(product2));

        orderService.persistOrder(order, client.getEmail());

        assertEquals(order.getClient().getName(), client.getName());
        assertEquals(order.getOrderCreated(), LocalDate.now());
        assertEquals(order.getState(), OrderState.CRIADO);
        assertEquals(productOrder.getPrice().multiply(BigDecimal.valueOf(productOrder.getQuantity().intValue()))
                        .add(productOrder2.getPrice().multiply(BigDecimal.valueOf(productOrder2.getQuantity().intValue()))),
                order.getTotalValue());
        assertEquals(quantityProduct1.subtract(productOrder.getQuantity()), product.getQuantity());
        assertEquals(quantityProduct2.subtract(productOrder2.getQuantity()), product2.getQuantity());
        assertEquals(order.getOrderDispatched(), null);
        assertEquals(order.getOrderDelivered(), null);

        then(rabbitTemplate).should(times(1)).convertAndSend(anyString(),
                anyString(), eq(order));
        then(productOrderRepository).should(times(1)).saveAll(any());
        then(productRepository).should(times(2)).save(any());
        then(orderRepository).should(times(1)).save(any());
    }

    @Test
    @DisplayName("Testa persistir quando envia endereço pelo pedido.")
    public void test_persistirQuandoEnviaEnderecoPeloPedido() {
        assertNotNull(orderService);

        BigInteger quantityProduct1 = product.getQuantity();
        BigInteger quantityProduct2 = product2.getQuantity();

        Store store = new Store();
        store.setCnpj("11111111111111");

        ProductOrder productOrder = new ProductOrder();
        productOrder.setCode("1");;
        productOrder.setQuantity(BigInteger.ONE);

        ProductOrder productOrder2 = new ProductOrder();
        productOrder2.setCode("2");
        productOrder2.setQuantity(BigInteger.TWO);

        product.setStore(store);
        product2.setStore(store);

        Client client = new Client();
        client.setEmail("a@A");
        client.setName("cris");

        Address address = new Address();
        address.setId(BigInteger.ONE);
        address.setClient(client);
        Set<Address> addresses = Set.of(address);

        Order order = new Order();
        order.setProducts(List.of(productOrder, productOrder2));
        order.setStore(store);
        order.setAddress(address);

        when(addressService.getAddressById(any())).thenReturn(address);
        when(clientService.getClientByEmail(any())).thenReturn(client);
        when(productRepository.findProductByCodeAndStore("1", store)).thenReturn(Optional.of(product));
        when(productRepository.findProductByCodeAndStore("2", store)).thenReturn(Optional.of(product2));

        orderService.persistOrder(order, client.getEmail());

        assertEquals(order.getClient().getName(), client.getName());
        assertEquals(order.getOrderCreated(), LocalDate.now());
        assertEquals(order.getState(), OrderState.CRIADO);
        assertEquals(productOrder.getPrice().multiply(BigDecimal.valueOf(productOrder.getQuantity().intValue()))
                        .add(productOrder2.getPrice().multiply(BigDecimal.valueOf(productOrder2.getQuantity().intValue()))),
                order.getTotalValue());
        assertEquals(quantityProduct1.subtract(productOrder.getQuantity()), product.getQuantity());
        assertEquals(quantityProduct2.subtract(productOrder2.getQuantity()), product2.getQuantity());
        assertEquals(order.getOrderDispatched(), null);
        assertEquals(order.getOrderDelivered(), null);

        then(productOrderRepository).should(times(1)).saveAll(any());
        then(productRepository).should(times(2)).save(any());
        then(orderRepository).should(times(1)).save(any());
        then(rabbitTemplate).should(times(1)).convertAndSend(anyString(),
                anyString(), eq(order));
    }

    @Test
    @DisplayName("Testa alteração do pedido para entregue quando não encontra.")
    public void test_alteracaoDoPedidoParaEntregueQuandoNaoEncontra_lancaException() {
        assertNotNull(orderService);

        when(orderRepository.findById(any())).thenReturn(Optional.empty());
        var assertThrows = assertThrows(NotFoundException.class,
                () -> orderService.setOrderStateToDelivered(BigInteger.ONE, "", OrderState.ENTREGUE));

        assertEquals("Nenhum pedido foi encontrado com o id: " + BigInteger.ONE + ".",
                assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa alteração do pedido para entregue quando o pedido já está como entregue.")
    public void test_alteracaoDoPedidoParaEntregueQuandoPedidJaEntregue_lancaException() {
        assertNotNull(orderService);

        Client client = new Client();
        client.setEmail("a@A");
        client.setName("cris");

        Order order = new Order();
        order.setClient(client);
        order.setState(OrderState.ENTREGUE);
        order.setOrderDelivered(LocalDate.now());

        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> orderService.setOrderStateToDelivered(BigInteger.ONE, "t", OrderState.ENTREGUE));

        assertEquals("O pedido já foi entregue na data: " +
                        order.getOrderDelivered().format(DateTimeFormatter.ofPattern("dd/MM/YYYY")),
                assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa alteração do pedido para entregue antes de estar como enviado.")
    public void test_alteracaoDoPedidoParaEntregueAntesDeEstarComoEnviado_lancaException() {
        assertNotNull(orderService);

        Client client = new Client();
        client.setEmail("a@A");
        client.setName("cris");

        Order order = new Order();
        order.setClient(client);
        order.setState(OrderState.CRIADO);

        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> orderService.setOrderStateToDelivered(BigInteger.ONE, "t", OrderState.ENTREGUE));

        assertEquals("Não é possível alterar o estado para ENTREGUE antes do pedido ser enviado.",
                assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa alteração do pedido para outro estado que não seja para ENTREGUE.")
    public void test_alteracaoDoPedidoParaOutroEstadoQueNaoSejaParaEntregue_lancaException() {
        assertNotNull(orderService);

        Client client = new Client();
        client.setEmail("a@A");
        client.setName("cris");

        Order order = new Order();
        order.setClient(client);
        order.setState(OrderState.ENVIADO);

        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> orderService.setOrderStateToDelivered(BigInteger.ONE, "a@A", OrderState.CRIADO));

        assertEquals("Somente é possível alterar o estado do pedido para: ENTREGUE.",
                assertThrows.getMessage());

        var assertThrows2 = assertThrows(IllegalArgumentException.class,
                () -> orderService.setOrderStateToDelivered(BigInteger.ONE, "a@A", OrderState.ENVIADO));

        assertEquals("Somente é possível alterar o estado do pedido para: ENTREGUE.",
                assertThrows2.getMessage());
    }

    @Test
    @DisplayName("Testa alteração do pedido para entregue quando não pertence ao usuário logado.")
    public void test_alteracaoDoPedidoParaEntregueQuandoNaoPertenceAoUsuarioLogado_lancaException() {
        assertNotNull(orderService);

        Client client = new Client();
        client.setEmail("a@A");
        client.setName("cris");

        Order order = new Order();
        order.setClient(client);
        order.setState(OrderState.ENVIADO);

        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        var assertThrows = assertThrows(NotFoundException.class,
                () -> orderService.setOrderStateToDelivered(BigInteger.ONE, "t", OrderState.ENTREGUE));

        assertEquals("Nenhum pedido foi encontrado com o id: " + BigInteger.ONE + ".",
                assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa alteração do pedido para entregue quando dados corretos.")
    public void test_alteracaoDoPedidoParaEntregueQuandoDadosCorretos() {
        assertNotNull(orderService);

        Client client = new Client();
        client.setEmail("a@A");
        client.setName("cris");

        Order order = new Order();
        order.setClient(client);
        order.setState(OrderState.ENVIADO);

        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        orderService.setOrderStateToDelivered(BigInteger.ONE, client.getEmail(), OrderState.ENTREGUE);

        assertEquals(order.getState(), OrderState.ENTREGUE);
        assertEquals(order.getOrderDelivered(), LocalDate.now());
        then(orderRepository).should(times(1)).save(any());
        then(rabbitTemplate).should(times(1)).convertAndSend(anyString(),
                anyString(), eq(order));
    }

    @Test
    @DisplayName("Testa busca pedido por ID quando não econtra.")
    public void test_buscaPedidoPorIdQuandoNaoEncontra_lancaException() {
        assertNotNull(orderService);

        var assertThrows = assertThrows(NotFoundException.class,
                () -> orderService.getOrderById(BigInteger.ONE, ""));

        assertEquals("Nenhum pedido foi encontrado com o id: " + BigInteger.ONE + ".",
                assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa busca pedido por ID quando não pertence ao usuário logado.")
    public void test_buscaPedidoPorIdQuandoNaoPertenceAoUsuarioLogado_lancaException() {
        assertNotNull(orderService);

        Client client = new Client();
        client.setEmail("a@A");
        client.setName("cris");

        Order order = new Order();
        order.setClient(client);

        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        var assertThrows = assertThrows(NotFoundException.class,
                () -> orderService.getOrderById(BigInteger.ONE, ""));

        assertEquals("Nenhum pedido foi encontrado com o id: " + BigInteger.ONE + ".",
                assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa busca de pedido por ID.")
    public void test_buscaPedidoPorId() {
        assertNotNull(orderService);

        Client client = new Client();
        client.setEmail("a@A");
        client.setName("cris");

        Order order = new Order();
        order.setClient(client);
        order.setState(OrderState.ENTREGUE);

        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        Order orderGet = orderService.getOrderById(BigInteger.ONE, "a@A");

        assertEquals(order.getState(), orderGet.getState());
    }

    @Test
    @DisplayName("Testa busca de todos pedidos do cliente logado quando não encontra nenhum.")
    public void test_buscaTodosPedidoDoClientQuandoNaoEncontra_lancaEception(){
        assertNotNull(orderService);

        Page<Order> ordersPage = new PageImpl<>(new ArrayList<>(), this.pageable, 0);
        when(orderRepository.findOrdersPageByClient(any(), any())).thenReturn(ordersPage);

        var assertThrows = assertThrows(NotFoundException.class,
                () -> orderService.getOrdersPageByClient(this.pageable, ""));

        assertEquals("Nenhum pedido foi encontrado.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa busca de todos pedidos do cliente logado.")
    public void test_buscaTodosPedidoDoClient(){
        assertNotNull(orderService);

        List<Order> ordersArray = new ArrayList<>();
        for(int x = 0; x < 100; x++) {
            ordersArray.add(new Order());
        }

        Page<Order> ordersPage = new PageImpl<>(ordersArray, this.pageable, 0);
        when(orderRepository.findOrdersPageByClient(any(), any())).thenReturn(ordersPage);


        List<Order> ordersPageGet = orderService.getOrdersPageByClient(this.pageable, "").toList();

        assertNotNull(ordersPageGet);
        assertEquals(ordersArray.size(), ordersPageGet.size());
    }
}