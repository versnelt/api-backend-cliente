package com.netbull.apiclient.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.netbull.apiclient.domain.address.Address;
import com.netbull.apiclient.domain.address.Type;
import com.netbull.apiclient.domain.address.TypeRepository;
import com.netbull.apiclient.domain.client.Client;
import com.netbull.apiclient.domain.order.Order;
import com.netbull.apiclient.domain.order.OrderRepository;
import com.netbull.apiclient.domain.order.OrderState;
import com.netbull.apiclient.domain.order.ProductOrder;
import com.netbull.apiclient.domain.store.Product;
import com.netbull.apiclient.domain.store.ProductRepository;
import com.netbull.apiclient.domain.store.Store;
import com.netbull.apiclient.domain.store.StoreRepository;
import com.netbull.apiclient.security.model.JwtRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource("classpath:application-test.properties")
class OrderControllerTest {

    @Autowired
    OrderRepository orderRepository;

    private static final String URI_ORDER = "/v1/clients/orders";
    private static final String URI_CLIENT = "/v1/clients";
    private static final String URI_ADDRESS = "/v1/clients/addresses";
    private static final String URI_AUTH = "/authenticate";
    private AtomicInteger key = new AtomicInteger(1);
    private Client logger = new Client();

    private Store store = new Store();
    private Product product1 = new Product();
    private Product product2 = new Product();


    private StringBuilder bearerToken = new StringBuilder();

    @Autowired
    private TypeRepository typeRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper mapper;

    private MockMvc mvc;

    private static final JacksonAnnotationIntrospector INTROSPECTOR = new JacksonAnnotationIntrospector() {
        @Override
        protected <A extends Annotation> A _findAnnotation(final Annotated annotated, final Class<A> annoClass) {
            if (!annotated.hasAnnotation(JsonProperty.class)) {
                return super._findAnnotation(annotated, annoClass);
            }
            return null;
        }
    };

    @BeforeAll
    public void setup() {

        this.mvc = MockMvcBuilders.webAppContextSetup(this.wac)
                .apply(springSecurity())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();

        this.store = new Store();
        this.store.setCnpj("11111111111111");
        this.store.setId(BigInteger.ONE);
        this.storeRepository.save(this.store);

        this.product1 = new Product();
        this.product1.setPrice(BigDecimal.TEN);
        this.product1.setCode("1");
        this.product1.setStore(this.store);
        this.product1.setId(BigInteger.ONE);
        this.product1.setQuantity(BigInteger.valueOf(100));
        this.productRepository.save(this.product1);

        this.product2 = new Product();
        this.product2.setPrice(BigDecimal.TEN);
        this.product2.setCode("2");
        this.product2.setStore(store);
        this.product2.setId(BigInteger.TWO);
        this.product2.setQuantity(BigInteger.valueOf(100));
        this.productRepository.save(this.product2);
    }

    @BeforeEach()
    public void beforeEach() throws Exception {
        mapper.setAnnotationIntrospector(INTROSPECTOR);

        Client client = new Client();
        String cpfKey = "";
        if(key.toString().toCharArray().length < 2) {
            cpfKey = "5".concat(key.toString());
        } else {
            cpfKey = key.toString();
        }

        client.setName("João Silva");
        client.setCpf("523456789".concat(cpfKey));
        client.setEmail("crisdewes@dewes".concat(key.toString()));
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");
        key.incrementAndGet();

        logger = client;

        ResultActions resultCreated = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_CLIENT)
                                .accept(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                                .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(client))
                ).andDo(print())
                .andExpect(status().isCreated());

        String messageResultCreated = resultCreated.andReturn().getResponse().getContentAsString();

        assertEquals("Cliente salvo.", messageResultCreated);

        JwtRequest jwtRequest = new JwtRequest(client.getEmail(), client.getPassword());

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_AUTH)
                                .accept(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                                .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(jwtRequest))
                ).andDo(print())
                .andExpect(status().isOk());

        HashMap<String, String> jwtResponse = mapper.readValue(resultActions
                .andReturn()
                .getResponse()
                .getContentAsString(), HashMap.class);

        bearerToken = new StringBuilder();
        jwtResponse.values().stream().forEach(token -> bearerToken.append("Bearer " + token));
        assertTrue(jwtResponse.containsKey("jwtToken"));

        Type type = new Type();
        type.setId(Integer.valueOf(1));
        type.setDescription("Escritório");

        typeRepository.save(type);

        Address address = new Address();
        address.setStreet("rua");
        address.setNumber("123456");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("Rio Grande do Sul");
        address.setType(type);

        this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_ADDRESS)
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(address))
                ).andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Testa criar pedido sem campos obrigatórios.")
    public void test_criarPedidoSemCamposObrigatorios_retorna400() throws Exception {
        Order order = new Order();

        mvc.perform(
                MockMvcRequestBuilders.post(URI_ORDER)
                        .header("Authorization", bearerToken.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(order))
        ).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Testa criar pedido.")
    public void test_criarPedido_retorna201() throws Exception {

        ProductOrder productOrder3 = new ProductOrder();
        productOrder3.setCode("1");
        productOrder3.setQuantity(BigInteger.ONE);
        ProductOrder productOrder4 = new ProductOrder();
        productOrder4.setCode("2");
        productOrder4.setQuantity(BigInteger.ONE);

        Order order = new Order();
        order.setProducts(List.of(productOrder3, productOrder4));
        order.setStore(store);

        ResultActions resultActions = mvc.perform(
                MockMvcRequestBuilders.post(URI_ORDER)
                        .header("Authorization", bearerToken.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(order))
        ).andExpect(status().isCreated());

        assertEquals("Pedido criado.", resultActions.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Testa busca de pedido por id.")
    public void test_buscaPedidoPorIdQuandoNaoExiste_retorna404() throws Exception {

        ResultActions resultget = mvc.perform(
                        MockMvcRequestBuilders.get(URI_ORDER.concat("/{id}"), BigInteger.ONE)
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Nenhum pedido foi encontrado com o id: " + BigInteger.ONE + ".",
                resultget.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Testa busca de pedido por id quando o pedido existe mas pertence à outro usuário.")
    public void test_buscaPedidoPorIdQuandoNaoPertenceAoClienteLogado_retorna404() throws Exception {

        Client client = new Client();

        client.setName("João Silva");
        client.setCpf("22345678911");
        client.setEmail("a@cris");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");
        key.incrementAndGet();

        this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_CLIENT)
                                .accept(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                                .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(client))
                ).andDo(print())
                .andExpect(status().isCreated());

        JwtRequest jwtRequest2 = new JwtRequest(client.getEmail(), client.getPassword());

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_AUTH)
                                .accept(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                                .contentType(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(jwtRequest2))
                ).andDo(print())
                .andExpect(status().isOk());

        HashMap<String, String> jwtResponse = mapper.readValue(resultActions
                .andReturn()
                .getResponse()
                .getContentAsString(), HashMap.class);

        String bearerToken2 = "Bearer " + jwtResponse.get("jwtToken");

        Type type = new Type();
        type.setId(Integer.valueOf(1));

        Address address = new Address();
        address.setStreet("rua");
        address.setNumber("123456");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("Rio Grande do Sul");
        address.setType(type);

        this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_ADDRESS)
                                .header("Authorization", bearerToken2)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(address))
                ).andDo(print())
                .andExpect(status().isCreated());

        Order orderOther = new Order();

        ProductOrder productOrder = new ProductOrder();
        productOrder.setCode("1");
        productOrder.setQuantity(BigInteger.ONE);

        ProductOrder productOrder2 = new ProductOrder();
        productOrder2.setCode("2");
        productOrder2.setQuantity(BigInteger.ONE);

        orderOther.setProducts(List.of(productOrder, productOrder2));
        orderOther.setStore(store);

        ResultActions resultpost = mvc.perform(
                MockMvcRequestBuilders.post(URI_ORDER)
                        .header("Authorization", bearerToken2)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(orderOther))
        ).andExpect(status().isCreated());

        assertEquals("Pedido criado.", resultpost.andReturn().getResponse().getContentAsString());

        ResultActions resultget = mvc.perform(
                        MockMvcRequestBuilders.get(URI_ORDER.concat("/{id}"), BigInteger.ONE)
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Nenhum pedido foi encontrado com o id: " + BigInteger.ONE + ".",
                resultget.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Testa busca de pedido por id.")
    public void test_buscaPedidoPorIdQuandoExiste_retorna200() throws Exception {

        ProductOrder productOrder = new ProductOrder();
        productOrder.setCode("1");
        productOrder.setQuantity(BigInteger.ONE);

        ProductOrder productOrder2 = new ProductOrder();
        productOrder2.setCode("2");
        productOrder2.setQuantity(BigInteger.ONE);

        Order order = new Order();
        order.setProducts(List.of(productOrder2, productOrder));
        order.setStore(store);

        ResultActions resultActions = mvc.perform(
                MockMvcRequestBuilders.post(URI_ORDER)
                        .header("Authorization", bearerToken.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(order))
        ).andExpect(status().isCreated());

        assertEquals("Pedido criado.", resultActions.andReturn().getResponse().getContentAsString());

        mvc.perform(
                        MockMvcRequestBuilders.get(resultActions.andReturn().getResponse().getHeader("Location"))
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Testa busca dos pedidos do cliente logado.")
    public void test_buscaPedidoDoClienteLogado_retorna200() throws Exception {

        ProductOrder productOrder = new ProductOrder();
        productOrder.setCode("1");
        productOrder.setQuantity(BigInteger.ONE);

        ProductOrder productOrder2 = new ProductOrder();
        productOrder2.setCode("2");
        productOrder2.setQuantity(BigInteger.ONE);

        Order order = new Order();
        order.setProducts(List.of(productOrder2, productOrder));
        order.setStore(store);

        ResultActions resultActions = mvc.perform(
                MockMvcRequestBuilders.post(URI_ORDER)
                        .header("Authorization", bearerToken.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(order))
        ).andExpect(status().isCreated());

        assertEquals("Pedido criado.", resultActions.andReturn().getResponse().getContentAsString());

        ProductOrder productOrder3 = new ProductOrder();
        productOrder3.setCode("1");
        productOrder3.setQuantity(BigInteger.ONE);

        ProductOrder productOrder4 = new ProductOrder();
        productOrder4.setCode("2");
        productOrder4.setQuantity(BigInteger.ONE);

        Order order2 = new Order();
        order2.setProducts(List.of(productOrder3, productOrder4));
        order2.setStore(store);

        ResultActions resultActions2 = mvc.perform(
                MockMvcRequestBuilders.post(URI_ORDER)
                        .header("Authorization", bearerToken.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(order2))
        ).andExpect(status().isCreated());

        assertEquals("Pedido criado.", resultActions2.andReturn().getResponse().getContentAsString());

        mvc.perform(
                MockMvcRequestBuilders.get(URI_ORDER)
                        .header("Authorization", bearerToken.toString())
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Testa busca dos pedidos do cliente logado quando não encontra.")
    public void test_buscaPedidoDoClienteLogadoQuandoNaoEncontra_retorna404() throws Exception {

        ResultActions resultget = mvc.perform(
                        MockMvcRequestBuilders.get(URI_ORDER)
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Nenhum pedido foi encontrado.",
                resultget.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Testa alteração do estado do pedido quando não existe.")
    public void test_alteracaoDoPedidoQuandoNaoExiste_retorna404() throws Exception {

        Order order = new Order();
        order.setState(OrderState.ENTREGUE);

        ResultActions resultget = mvc.perform(
                        MockMvcRequestBuilders.patch(URI_ORDER.concat("/{id}"), BigInteger.valueOf(1050))
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(order))
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Nenhum pedido foi encontrado com o id: " + BigInteger.valueOf(1050) + ".",
                resultget.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Testa alteração do estado do pedido quando envia valor diferente de ENTREGUE.")
    public void test_alteracaoDoEstadoDoPedidoQuandoEnviaValorDiferenteDeEntregue_retorna400() throws Exception {

        ProductOrder productOrder = new ProductOrder();
        productOrder.setCode("1");
        productOrder.setQuantity(BigInteger.ONE);

        ProductOrder productOrder2 = new ProductOrder();
        productOrder2.setCode("2");
        productOrder2.setQuantity(BigInteger.ONE);

        Order order = new Order();
        order.setProducts(List.of(productOrder2, productOrder));
        order.setStore(store);

        ResultActions resultActions = mvc.perform(
                MockMvcRequestBuilders.post(URI_ORDER)
                        .header("Authorization", bearerToken.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(order))
        ).andExpect(status().isCreated());

        assertEquals("Pedido criado.", resultActions.andReturn().getResponse().getContentAsString());
        BigInteger id = BigInteger.valueOf(
                Integer.valueOf(resultActions.andReturn().getResponse().getHeader("Location")
                        .split("/")[6]));

        Order saveOrder = this.orderRepository.findById(id).get();
        saveOrder.setState(OrderState.ENVIADO);
        this.orderRepository.save(saveOrder);

        Order order1 = new Order();
        order1.setState(OrderState.CRIADO);

        ResultActions resultpatch = mvc.perform(
                        MockMvcRequestBuilders.patch(resultActions.andReturn().getResponse().getHeader("Location"))
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(order1))
                ).andDo(print())
                .andExpect(status().isBadRequest());

        assertEquals("Somente é possível alterar o estado do pedido para: ENTREGUE.",
                resultpatch.andReturn().getResponse().getContentAsString());

        order1.setState(OrderState.ENVIADO);

        ResultActions resultpatch2 = mvc.perform(
                        MockMvcRequestBuilders.patch(resultActions.andReturn().getResponse().getHeader("Location"))
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(order1))
                ).andDo(print())
                .andExpect(status().isBadRequest());

        assertEquals("Somente é possível alterar o estado do pedido para: ENTREGUE.",
                resultpatch2.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Testa alteração do estado do pedido antes de estar como ENVIADO.")
    public void test_alteracaoDoEstadoDoPedidoAntesDeEstarComoEnviado_retorna400() throws Exception {

        ProductOrder productOrder = new ProductOrder();
        productOrder.setCode("1");
        productOrder.setQuantity(BigInteger.ONE);

        ProductOrder productOrder2 = new ProductOrder();
        productOrder2.setCode("2");
        productOrder2.setQuantity(BigInteger.ONE);

        Order order = new Order();
        order.setProducts(List.of(productOrder2, productOrder));
        order.setStore(store);

        ResultActions resultActions = mvc.perform(
                MockMvcRequestBuilders.post(URI_ORDER)
                        .header("Authorization", bearerToken.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(order))
        ).andExpect(status().isCreated());

        Order orderEntregue = new Order();
        orderEntregue.setState(OrderState.ENTREGUE);

        assertEquals("Pedido criado.", resultActions.andReturn().getResponse().getContentAsString());
        ResultActions resultpatch = mvc.perform(
                        MockMvcRequestBuilders.patch(resultActions.andReturn().getResponse().getHeader("Location"))
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(orderEntregue))
                ).andDo(print())
                .andExpect(status().isBadRequest());

        assertEquals("Não é possível alterar o estado para ENTREGUE antes do pedido ser enviado.",
                resultpatch.andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Testa alteração do estado do pedido para ENTREGUE.")
    public void test_alteracaoDoEstadoDoPedidoParaEntregue_retorna200() throws Exception {

        ProductOrder productOrder = new ProductOrder();
        productOrder.setCode("1");
        productOrder.setQuantity(BigInteger.ONE);

        ProductOrder productOrder2 = new ProductOrder();
        productOrder2.setCode("2");
        productOrder2.setQuantity(BigInteger.ONE);

        Order order = new Order();
        order.setProducts(List.of(productOrder2, productOrder));
        order.setStore(store);

        ResultActions resultActions = mvc.perform(
                MockMvcRequestBuilders.post(URI_ORDER)
                        .header("Authorization", bearerToken.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(order))
        ).andExpect(status().isCreated());

        assertEquals("Pedido criado.", resultActions.andReturn().getResponse().getContentAsString());
        BigInteger id = BigInteger.valueOf(
                Integer.valueOf(resultActions.andReturn().getResponse().getHeader("Location")
                        .split("/")[6]));

        Order saveOrder = this.orderRepository.findById(id).get();
        saveOrder.setState(OrderState.ENVIADO);
        this.orderRepository.save(saveOrder);

        Order orderEntregue = new Order();
        orderEntregue.setState(OrderState.ENTREGUE);

        ResultActions resultpatch = mvc.perform(
                        MockMvcRequestBuilders.patch(resultActions.andReturn().getResponse().getHeader("Location"))
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(orderEntregue))
                ).andDo(print())
                .andExpect(status().isOk());

        assertEquals("Pedido alterado para entregue.",
                resultpatch.andReturn().getResponse().getContentAsString());
    }
}