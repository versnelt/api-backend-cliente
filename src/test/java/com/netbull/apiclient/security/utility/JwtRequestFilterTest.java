package com.netbull.apiclient.security.utility;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource("classpath:application-test.properties")
public class JwtRequestFilterTest {

    private static final String URI_ADDRESS = "/v1/client/address";

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper mapper;

    private JwtRequestFilter jwtRequestFilter;

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
    public void setup() throws Exception {
        this.jwtRequestFilter = new JwtRequestFilter();
        this.mvc = MockMvcBuilders.webAppContextSetup(this.wac)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .apply(springSecurity())
                .build();

        mapper.setAnnotationIntrospector(INTROSPECTOR);
    }

    @Test
    @DisplayName("Testa filtro quando usuário não autorizado.")
    public void test_filtroQuandoNaoAuth_isUnauthorized() throws Exception {

        this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS)
                ).andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Testa filtro quando token inválido.")
    public void test_filtroQuandoTokenInválido_isUnauthorized() throws Exception {

        ResultActions resultActions = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS)
                                .header("Authorization", "Bearer asasdf")
                ).andDo(print())
                .andExpect(status().isUnauthorized());

        assertEquals("Token inválido.",
                resultActions.andReturn().getResponse().getContentAsString());

        ResultActions resultActions2 = this.mvc.perform(
                        MockMvcRequestBuilders.get(URI_ADDRESS)
                                .header("Authorization", "asasdf")
                ).andDo(print())
                .andExpect(status().isUnauthorized());

        assertEquals("Token inválido.",
                resultActions2.andReturn().getResponse().getContentAsString());
    }

}