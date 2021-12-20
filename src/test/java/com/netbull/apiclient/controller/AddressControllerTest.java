package com.netbull.apiclient.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.netbull.apiclient.domain.address.Address;
import com.netbull.apiclient.domain.address.Type;
import com.netbull.apiclient.domain.address.TypeRepository;
import com.netbull.apiclient.domain.client.Client;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource("classpath:application-test.properties")
public class AddressControllerTest {

    private static final String URI_ADDRESS = "/v1/clients/addresses";
    private static final String URI_CLIENT = "/v1/clients";
    private static final String URI_AUTH = "/authenticate";
    private AtomicInteger key = new AtomicInteger(1);
    private Client logger = new Client();

    private StringBuilder bearerToken = new StringBuilder();

    @Autowired
    private TypeRepository typeRepository;

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
    }



    @BeforeEach()
    public void beforeEach() throws Exception {
        mapper.setAnnotationIntrospector(INTROSPECTOR);

        Client client = new Client();

        client.setName("João Silva");
        client.setCpf("1234567891".concat(key.toString()));
        client.setEmail("cris@dewes".concat(key.toString()));
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
    }

    @Test
    @DisplayName("Envio de endereco sem campos obrigatórios.")
    public void test_envioCamposSemDados_retona400() throws Exception {
        Address address = new Address();

        this.mvc.perform(
                MockMvcRequestBuilders.post(URI_ADDRESS)
                        .header("Authorization", bearerToken.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(address))
                        ).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cria endereço.")
    public void test_criarEndereco_retona201() throws Exception {

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
    @DisplayName("Busca endereço por id, quando existe e quando não existe.")
    public void test_buscaEnderecoPorId_retona201() throws Exception {

        ResultActions resultGetNull = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS.concat("/{id}"), "1")
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Endereço não encontrado.",
                resultGetNull.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

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

        ResultActions resultCreatedAddress = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_ADDRESS)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", bearerToken.toString())
                                .content(mapper.writeValueAsString(address))
                ).andDo(print())
                .andExpect(status().isCreated());

        ResultActions resultGetAddress = this.mvc.perform(
                        MockMvcRequestBuilders.get(resultCreatedAddress
                                        .andReturn()
                                        .getResponse().getHeader("Location"))
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        Address addressResponse = mapper.readValue(resultGetAddress.andReturn()
                .getResponse()
                .getContentAsString(),
                Address.class);

        assertEquals(address.getStreet(), addressResponse.getStreet());
    }

    @Test
    @DisplayName("Busca todos os tipos de endereços disponíveis.")
    public void test_buscaTiposDeEndereco_retona201() throws Exception {

        Type type = new Type();
        type.setId(Integer.valueOf(1));
        type.setDescription("Escritório");

        typeRepository.save(type);

        Type type2 = new Type();
        type2.setId(Integer.valueOf(2));
        type2.setDescription("Casa");

        typeRepository.save(type2);

        Type type3 = new Type();
        type3.setId(Integer.valueOf(3));
        type3.setDescription("Trabalho");

        typeRepository.save(type3);

        ResultActions resultGetTypes = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS.concat("/types"))
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        List<HashMap<String, String>> typeResponse = mapper.readValue(resultGetTypes.andReturn()
                        .getResponse()
                        .getContentAsString(),
               List.class);

        assertEquals(3, typeResponse.size());
        assertEquals(type.getDescription(), typeResponse.get(0).get("description"));
        assertEquals(type2.getDescription(), typeResponse.get(1).get("description"));
        assertEquals(type3.getDescription(), typeResponse.get(2).get("description"));
    }

    @Test
    @DisplayName("Busca endereço por cliente, quando existe e quando não existe.")
    public void test_buscaEnderecoPorCliente_retona201() throws Exception {

        ResultActions resultGetNull = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS)
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", bearerToken.toString())
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Endereço não encontrado.",
                resultGetNull.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

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

        ResultActions resultCreatedAddress = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_ADDRESS)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", bearerToken.toString())
                                .content(mapper.writeValueAsString(address))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Endereço criado.", resultCreatedAddress.andReturn().getResponse().getContentAsString());

        ResultActions resultGetAddress = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS)
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", bearerToken.toString())
                ).andDo(print())
                .andExpect(status().isOk());

        assertNotNull(resultGetAddress);
    }

    @Test
    @DisplayName("Altera o tipo de endereço.")
    public void test_alteraOTipoDeEndereço() throws Exception {

        ResultActions resultGetNull = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS.concat("/{id}"), "1000")
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Endereço não encontrado.",
                resultGetNull.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

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

        ResultActions resultCreatedAddress = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_ADDRESS)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", bearerToken.toString())
                                .content(mapper.writeValueAsString(address))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Endereço criado.", resultCreatedAddress.andReturn().getResponse().getContentAsString());

        Type newType = new Type();
        newType.setId(Integer.valueOf(2));
        newType.setDescription("Casa");

        typeRepository.save(type);

        ResultActions resultPatchAddress = this.mvc.perform(
                        MockMvcRequestBuilders.patch(resultCreatedAddress
                                        .andReturn()
                                        .getResponse()
                                        .getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", bearerToken.toString())
                                .content(mapper.writeValueAsString(newType))
                ).andDo(print())
                .andExpect(status().isOk());

        assertEquals("Endereço alterado.", resultPatchAddress.andReturn().getResponse().getContentAsString());

        ResultActions resultGetAddress = this.mvc.perform(
                        MockMvcRequestBuilders.get(resultCreatedAddress
                                        .andReturn()
                                        .getResponse()
                                        .getHeader("Location"))
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        Address addressReturn = mapper.readValue(resultGetAddress
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                Address.class);

        assertEquals(newType.getDescription(), addressReturn.getType().getDescription());
    }

    @Test
    @DisplayName("Altera o endereço.")
    public void test_alteraOEndereço() throws Exception {

        ResultActions resultGetNull = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS.concat("/{id}"), "100")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", bearerToken.toString())
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Endereço não encontrado.",
                resultGetNull.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

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

        ResultActions resultCreatedAddress = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_ADDRESS)
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", bearerToken.toString())
                                .content(mapper.writeValueAsString(address))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Endereço criado.", resultCreatedAddress.andReturn().getResponse().getContentAsString());

        address.setNumber("789");
        address.setDistrict("Bairro");
        address.setCity("Feliz");

        ResultActions resultPutAddress = this.mvc.perform(
                        MockMvcRequestBuilders.put(resultCreatedAddress
                                        .andReturn()
                                        .getResponse()
                                        .getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", bearerToken.toString())
                                .content(mapper.writeValueAsString(address))
                ).andDo(print())
                .andExpect(status().isOk());

        assertEquals("Endereço alterado.", resultPutAddress.andReturn().getResponse().getContentAsString());

        ResultActions resultGetAddress = this.mvc.perform(
                        MockMvcRequestBuilders.get(resultCreatedAddress
                                        .andReturn()
                                        .getResponse()
                                        .getHeader("Location"))
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", bearerToken.toString())
                ).andDo(print())
                .andExpect(status().isOk());

        Address addressReturn = mapper.readValue(resultGetAddress
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                Address.class);

        assertEquals(address.getNumber(), addressReturn.getNumber());
        assertEquals(address.getDistrict(), addressReturn.getDistrict());
        assertEquals(address.getCity(), addressReturn.getCity());
    }

    @Test
    @DisplayName("Exclui um endereço.")
    public void test_deletaOEndereço() throws Exception {

        ResultActions resultGetNull = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS.concat("/{id}"), "100")
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Endereço não encontrado.",
                resultGetNull.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

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

        ResultActions resultCreatedAddress = this.mvc.perform(
                        MockMvcRequestBuilders.post(URI_ADDRESS)
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(address))
                ).andDo(print())
                .andExpect(status().isCreated());

        assertEquals("Endereço criado.", resultCreatedAddress.andReturn().getResponse().getContentAsString());

        ResultActions resultDeleteAddress = this.mvc.perform(
                        MockMvcRequestBuilders.delete(resultCreatedAddress
                                        .andReturn()
                                        .getResponse()
                                        .getHeader("Location"))
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isOk());

        assertEquals("Endereço deletado.", resultDeleteAddress.andReturn().getResponse().getContentAsString());

        ResultActions resultGetExcluido = this.mvc.perform(
                        MockMvcRequestBuilders.get(resultCreatedAddress
                                        .andReturn()
                                        .getResponse()
                                        .getHeader("Location"))
                                .header("Authorization", bearerToken.toString())
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().isNotFound());

        assertEquals("Endereço não encontrado.",
                resultGetExcluido.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
