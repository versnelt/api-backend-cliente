package com.netbull.apiclient.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.netbull.apiclient.domain.client.Client;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.core.MediaType;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource("classpath:application-test.properties")
public class ClientControllerTest {

    private static final String URI_CLIENT = "/v1/clients";

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
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();

        mapper.setAnnotationIntrospector(INTROSPECTOR);
    }

    @Test
    @DisplayName("Envio de cliente sem campos obrigatórios.")
    public void test_envioCamposSemDados_retona400() throws Exception {
        Client client = new Client();

        this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_CLIENT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(client))
                ).andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cria cliente.")
    public void test_criarCliente_retona201() throws Exception {

        Client client = new Client();

        client.setName("João Silva");
        client.setCpf("12345678910");
        client.setEmail("cria@com");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        ResultActions resultCreated = this.mvc.perform(
                                                MockMvcRequestBuilders.post(URI_CLIENT)
                                                    .accept(MediaType.APPLICATION_JSON)
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content(mapper.writeValueAsString(client))
                                                ).andDo(print())
                                                .andExpect(status().isCreated());

        String messageResultCreated = resultCreated.andReturn().getResponse().getContentAsString();

        assertEquals("Cliente salvo.", messageResultCreated);
    }

    @Test
    @DisplayName("Busca todos clientes.")
    public void test_buscaTodosClientes() throws Exception {

        Client client = new Client();

        client.setName("João Silva");
        client.setCpf("11111111155");
        client.setEmail("a@co22");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        ResultActions resultCreated = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_CLIENT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(client))
                ).andDo(print())
                .andExpect(status().isCreated());

        String messageResultCreated = resultCreated.andReturn().getResponse().getContentAsString();

        assertEquals("Cliente salvo.", messageResultCreated);

        this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_CLIENT)
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Busca cliente por email.")
    public void test_buscaClientePorEmail_retona200() throws Exception {
        Client client = new Client();

        client.setName("Sabrina Silva");
        client.setCpf("22222222223");
        client.setEmail("a@atos3");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        ResultActions resultCreated = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_CLIENT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(client))
                        ).andDo(print())
                        .andExpect(status().isCreated());

        String messageResultCreated = resultCreated.andReturn().getResponse().getContentAsString();

        assertEquals("Cliente salvo.", messageResultCreated);

        ResultActions resultConsulted = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_CLIENT.concat("/email/{email}"),
                                client.getEmail())
                                .accept(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk());

        Client clientConsulted = mapper.readValue(resultConsulted
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                Client.class);

        assertEquals(client.getCpf(), clientConsulted.getCpf());
    }

    @Test
    @DisplayName("Busca cliente por cpf.")
    public void test_buscaClientePorCpf_retona200() throws Exception {
        Client client = new Client();

        client.setName("Sabrina Silva");
        client.setCpf("33353333334");
        client.setEmail("a@dewes454");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        ResultActions resultCreated = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_CLIENT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(client))
                ).andDo(print())
                .andExpect(status().isCreated());

        String messageResultCreated = resultCreated.andReturn().getResponse().getContentAsString();

        assertEquals("Cliente salvo.", messageResultCreated);

        ResultActions resultConsulted = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_CLIENT.concat("/cpf/{cpf}"),
                                        client.getCpf())
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        Client clientConsulted = mapper.readValue(resultConsulted
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                Client.class);

        assertEquals(client.getCpf(), clientConsulted.getCpf());
    }

    @Test
    @DisplayName("Busca cliente por id.")
    public void test_buscaClientePorId_retona200() throws Exception {
        Client client = new Client();

        client.setName("Sabrina Silva");
        client.setCpf("33333333334");
        client.setEmail("a@dewes4");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        ResultActions resultCreated = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_CLIENT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(client))
                ).andDo(print())
                .andExpect(status().isCreated());

        String messageResultCreated = resultCreated.andReturn().getResponse().getContentAsString();

        assertEquals("Cliente salvo.", messageResultCreated);

        ResultActions resultConsulted = this.mvc.perform(
                        MockMvcRequestBuilders.get(resultCreated.andReturn().getResponse().getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        Client clientConsulted = mapper.readValue(resultConsulted
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                Client.class);

        assertEquals(client.getCpf(), clientConsulted.getCpf());
    }

    @Test
    @DisplayName("Altera atributos do cliente.")
    @WithMockUser(username = "aDewes@versnelt5")
    public void test_alteraUmAtributoDoCliente_retona200() throws Exception {

        Client client = new Client();

        client.setName("CristianoDewes");
        client.setCpf("44444744445");
        client.setEmail("aDewes@versnelt5");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        ResultActions resultCreatedPost = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_CLIENT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(client))
                ).andDo(print())
                .andExpect(status().isCreated());

        String messageResultCreated = resultCreatedPost.andReturn().getResponse().getContentAsString();
        assertEquals("Cliente salvo.", messageResultCreated);

        Client newClient = new Client();

        newClient.setName("AndreiLuizSegundo");
        newClient.setCpf("44444744445");
        newClient.setEmail("aDewes@versnelt5");
        newClient.setBirthday(LocalDate.now().minusDays(10));
        newClient.setPassword("1234");

        ResultActions resultCreatedPut = this.mvc.perform(
                        MockMvcRequestBuilders.put(URI_CLIENT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(newClient))
                ).andDo(print())
                .andExpect(status().isCreated());

        String messageResultCreatedPut = resultCreatedPut.andReturn().getResponse().getContentAsString();

        assertEquals("Cliente alterado.", messageResultCreatedPut);

        ResultActions resultConsulted = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_CLIENT.concat("/cpf/{cpf}"),
                                        client.getCpf())
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        Client clientConsulted = mapper.readValue(resultConsulted
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                Client.class);

        assertEquals(newClient.getName(), clientConsulted.getName());
    }

    @Test
    @DisplayName("Testa método delete.")
    @WithMockUser("a@versnelt6")
    public void test_deletaClient() throws Exception {
        Client client = new Client();

        client.setName("Cristiano");
        client.setCpf("44444444446");
        client.setEmail("a@versnelt6");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        ResultActions resultCreatedPost = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_CLIENT)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(client))
                ).andDo(print())
                .andExpect(status().isCreated());

        String messageResultCreated = resultCreatedPost.andReturn().getResponse().getContentAsString();
        assertEquals("Cliente salvo.", messageResultCreated);

        ResultActions resultDelete = this.mvc.perform(
                        MockMvcRequestBuilders.delete(URI_CLIENT)
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        String messageDelete = resultDelete.andReturn().getResponse().getContentAsString();
        assertEquals("Cliente deletado.", messageDelete);
    }
}
