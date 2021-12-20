package com.netbull.apiclient.service;

import com.netbull.apiclient.domain.address.Address;
import com.netbull.apiclient.domain.address.AddressRepository;
import com.netbull.apiclient.domain.client.Client;
import com.netbull.apiclient.domain.client.ClientRepository;
import com.netbull.apiclient.utility.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import javax.validation.*;
import javax.ws.rs.NotFoundException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClientServiceTest {

    private ClientService clientService;

    private Validator validation;

    private ClientRepository clientRepository;

    private AddressRepository addressRepository;

    private StringUtils stringUtils;

    private Pageable pageable;

    @BeforeAll
    public void setupBeforAll() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        this.validation = validatorFactory.getValidator();
    }

    @BeforeEach
    public void setupBeforEach() {
        this.clientRepository = Mockito.mock(ClientRepository.class);
        this.addressRepository = Mockito.mock(AddressRepository.class);
        this.stringUtils = Mockito.mock(StringUtils.class);
        this.pageable = Mockito.mock(Pageable.class);
        this.clientService = new ClientService(validation, clientRepository, addressRepository, stringUtils);
    }

    @Test
    @DisplayName("Testa persistir quando o cliente for nulo.")
    public void testaPersistir_quando_ClienteNull_lancaExcecao() {
        assertNotNull(clientService);

        Client client = null;

        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> clientService.persistClient(client));

        assertNotNull(assertThrows);
    }

    @Test
    @DisplayName("Testa persistir quando todos os atributos do cliente quando forem nulos.")
    public void testaPersistir_quando_atributosDoClienteEhNull_lancaException() {
        assertNotNull(clientService);

        Client client = new Client();

        var assertThrows = assertThrows(ConstraintViolationException.class,
                () -> clientService.persistClient(client));

        assertEquals(5, assertThrows.getConstraintViolations().size());

        List<String> messages = assertThrows.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages, hasItems(
                "O nome não pode ser vazio.",
                "O CPF não pode ser vazio.",
                "O e-mail não pode ser vazio.",
                "A data de nascimento não pode ser vazia.",
                "A senha não pode ser vazia."
        ));
    }

    @Test
    @DisplayName("Testa persistir quando todos os atributos do cliente quando não estão no padrão.")
    public void testaPersistir_quando_atributosEstaoForaDoPadrao_lancaException() {
        assertNotNull(clientService);

        Client client = new Client();

        client.setName("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        client.setCpf("ABC");
        client.setEmail("a.com");
        client.setBirthday(LocalDate.now());
        client.setPassword("av");

        var assertThrows = assertThrows(ConstraintViolationException.class,
                () -> clientService.persistClient(client));

        assertEquals(5, assertThrows.getConstraintViolations().size());

        List<String> messages = assertThrows.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages, hasItems(
                "O nome é muito grande.",
                "Data de nascimento inválida.",
                "E-mail inválido.",
                "CPF inválido.",
                "Senha muito curta."
        ));
    }

    @Test
    @DisplayName("Testa persistir quando todos os atributos do cliente quando estão no padrão.")
    public void testaPersistir_quando_atributosEstaoDentroDoPadrao() {
        assertNotNull(clientService);

        Client client = new Client();

        client.setName("João Silva");
        client.setCpf("11111111111");
        client.setEmail("a@com");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        clientService.persistClient(client);

        then(clientRepository).should(times(1)).save(any());
    }

    @Test
    @DisplayName("Testa persistir quando o email ja está sendo utilizado.")
    public void testaPersistir_quando_emailEstaSendoUtilizado_lancaException() {
        assertNotNull(clientService);

        Client client = new Client();
        client.setName("João Silva");
        client.setCpf("11111111111");
        client.setEmail("a@com");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        Client client2 = new Client();

        client2.setName("João Silva");
        client2.setCpf("11111111112");
        client2.setEmail("a@com");
        client2.setBirthday(LocalDate.now().minusDays(1));
        client2.setPassword("abc");

        when(clientRepository.findByEmail(client.getEmail()))
                .thenReturn(Optional.of(client));

        var assertThrows = assertThrows(DuplicateKeyException.class,
                () -> clientService.persistClient(client2));
        List<String> messages = List.of(assertThrows.getMessage());

        assertThat(messages, hasItems("Email já utilizado."));

        then(clientRepository).should(times(1)).findByEmail(anyString());

    }

    @Test
    @DisplayName("Testa persistir quando o CPF ja está sendo utilizado.")
    public void testaPersistir_quando_cpfEstaSendoUtilizado_lancaException() {
        assertNotNull(clientService);

        Client client = new Client();
        client.setName("João Silva");
        client.setCpf("11111111111");
        client.setEmail("a@com");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        Client client2 = new Client();

        client2.setName("João Silva");
        client2.setCpf("11111111111");
        client2.setEmail("a@atos");
        client2.setBirthday(LocalDate.now().minusDays(1));
        client2.setPassword("abc");

        when(clientRepository.findByCpf(client.getCpf()))
                .thenReturn(Optional.of(client));

        var assertThrows = assertThrows(DuplicateKeyException.class,
                () -> clientService.persistClient(client2));
        List<String> messages = List.of(assertThrows.getMessage());

        assertThat(messages, hasItems("CPF já cadastrado."));

        then(clientRepository).should(times(1)).findByEmail(anyString());

    }

    @Test
    @DisplayName("Testa busca de todos clientes quando vazio e quando encontra.")
    public void testa_BuscaTodosClientes() {
        assertNotNull(clientService);
        Page<Client> clientsPage = new PageImpl<>(new ArrayList<>(), this.pageable, 0);

        when(clientRepository.findAll(this.pageable)).thenReturn(clientsPage);

        var assertThrows = assertThrows(NotFoundException.class,
                () -> this.clientService.getAllClients(this.pageable));

        then(clientRepository).should(times(1)).findAll(this.pageable);
        assertEquals("Nenhum cliente foi encontrado.", assertThrows.getMessage());

        List<Client> clients = new ArrayList<>();
        clients.add(new Client());
        clients.add(new Client());
        clients.add(new Client());
        clients.add(new Client());

        clientsPage = new PageImpl<>(clients, this.pageable, 0);

        when(clientRepository.findAll(this.pageable)).thenReturn(clientsPage);

        List<Client> clientsEncontrados = clientService.getAllClients(this.pageable).toList();

        then(clientRepository).should(times(2)).findAll(this.pageable);
        assertNotNull(clientsEncontrados);
        assertEquals(4, clientsEncontrados.size());
    }

    @Test
    @DisplayName("Testa a busca por id quando encontra e não encontra.")
    public void testa_buscaClientePorId() {
        assertNotNull(clientService);

        var asserThrows = assertThrows(NotFoundException.class,
                () -> clientService.getClientById(BigInteger.ONE));

        assertEquals("Cliente não encontrado.", asserThrows.getMessage());
        then(clientRepository).should(times(1)).findById(any());

        Client client = new Client();
        client.setName("João Silva");
        client.setCpf("11111111111");
        client.setEmail("a@com");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        when(clientRepository.findById(any())).thenReturn(Optional.of(client));

        Client clientResult = clientService.getClientById(BigInteger.ONE);

        assertNotNull(clientResult);
        assertEquals(client.getCpf(), clientResult.getCpf());
        then(clientRepository).should(times(2)).findById(any());
    }

    @Test
    @DisplayName("Testa a busca por email quando encontra e não encontra.")
    public void testa_buscaClientePorEmail() {
        assertNotNull(clientService);

        var asserThrows = assertThrows(NotFoundException.class,
                () -> clientService.getClientByEmail(null));

        assertEquals("Cliente não encontrado.", asserThrows.getMessage());
        then(clientRepository).should(times(1)).findByEmail(any());

        Client client = new Client();
        client.setName("João Silva");
        client.setCpf("11111111111");
        client.setEmail("a@com");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        when(clientRepository.findByEmail(any())).thenReturn(Optional.of(client));

        Client clientResult = clientService.getClientByEmail(client.getEmail());

        assertNotNull(clientResult);
        assertEquals(client.getCpf(), clientResult.getCpf());
        then(clientRepository).should(times(2)).findByEmail(any());
    }

    @Test
    @DisplayName("Testa a busca por CPF quando encontra e não encontra.")
    public void testa_buscaClientePorCPF() {
        assertNotNull(clientService);

        var asserThrows = assertThrows(NotFoundException.class,
                () -> clientService.getClientByCpf(""));

        assertEquals("Cliente não encontrado.", asserThrows.getMessage());
        then(clientRepository).should(times(1)).findByCpf(any());

        Client client = new Client();
        client.setName("João Silva");
        client.setCpf("11111111111");
        client.setEmail("a@com");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        when(clientRepository.findByCpf(any())).thenReturn(Optional.of(client));

        Client clientResult = clientService.getClientByCpf(client.getCpf());

        assertNotNull(clientResult);
        assertEquals(client.getCpf(), clientResult.getCpf());
        then(clientRepository).should(times(2)).findByCpf(any());
    }

    @Test
    @DisplayName("Testa alteração do cliente quando ele existir e não existir.")
    public void testa_alteracaoCliente() {
        assertNotNull(clientService);

        var assertThrows1 = assertThrows(NotFoundException.class,
                () -> clientService.putClient("user", new Client()));

        assertEquals("Cliente não encontrado.", assertThrows1.getMessage());

        Client client = new Client();
        client.setName("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        client.setCpf("ABC");
        client.setEmail("a.com");
        client.setBirthday(LocalDate.now());
        client.setPassword("av");

        Client cliente2 = new Client();
        cliente2.setCpf(client.getCpf());

        when(clientRepository.findByEmail(any())).thenReturn(Optional.of(cliente2));

        var assertThrows = assertThrows(ConstraintViolationException.class,
                () -> clientService.putClient("a.com", client));

        assertEquals(5, assertThrows.getConstraintViolations().size());

        List<String> messages = assertThrows.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages, hasItems(
                "O nome é muito grande.",
                "Data de nascimento inválida.",
                "E-mail inválido.",
                "CPF inválido.",
                "Senha muito curta."
        ));

        client.setName("João Silva");
        client.setCpf("11111111111");
        client.setEmail("a@com");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        Client clientAlterado = new Client();
        clientAlterado.setName("João Silva");
        clientAlterado.setCpf("11111111112");
        clientAlterado.setEmail("a@com");
        clientAlterado.setBirthday(LocalDate.now().minusDays(1));
        clientAlterado.setPassword("abc");

        when(clientRepository.findByEmail(any())).thenReturn(Optional.of(client));

        clientAlterado.setName("Cristiano");
        clientAlterado.setCpf("11111111111");

        clientService.putClient("a@com", clientAlterado);

        then(clientRepository).should(times(1)).save(any());
        assertEquals(clientAlterado.getName(), client.getName());
    }

    @Test
    @DisplayName("Testa alteração do cadastro do cliente quando o e-mail novo já está sendo utilizado.")
    public void testa_alteracaoClienteQuandoEmailJaUtilizado() {
        assertNotNull(clientService);

        Client client = new Client();
        client.setCpf("11111111111");
        client.setEmail("a@A");

        Client oldClient = new Client();
        oldClient.setCpf("22222222222");
        oldClient.setEmail("b@B");

        Client newClient = new Client();
        newClient.setId(BigInteger.ONE);
        newClient.setCpf("22222222222");
        newClient.setEmail("a@A");
        newClient.setName("João Silva");
        newClient.setBirthday(LocalDate.now().minusDays(1));
        newClient.setPassword("abc");

        when(clientRepository.findByEmail(oldClient.getEmail())).thenReturn(Optional.of(oldClient));
        when(clientRepository.findByEmail(client.getEmail())).thenReturn(Optional.of(client));

        var assertThrows = assertThrows(DuplicateKeyException.class,
                () -> clientService.putClient(oldClient.getEmail(), newClient));
        assertEquals("Email já utilizado.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa delete do cliente quando encontra e não encontra.")
    public void testa_deleteClient() {
        assertNotNull(clientService);

        var assertThrows = assertThrows(NotFoundException.class,
                () -> clientService.deleteClient(""));

        assertEquals("Cliente não encontrado.", assertThrows.getMessage());

        Client client = new Client();
        client.setId(BigInteger.ONE);
        client.setName("João Silva");
        client.setCpf("11111111111");
        client.setEmail("a@com");
        client.setBirthday(LocalDate.now().minusDays(1));
        client.setPassword("abc");

        when(clientRepository.findByEmail(any())).thenReturn(Optional.of(client));

        clientService.deleteClient(client.getEmail());
        then(clientRepository).should(times(1)).delete(client);

        Set<Address> address = new HashSet<>();

        for(int x=0; x<10; x++) {
            address.add(new Address());
        }

        when(addressRepository.findByClient(any())).thenReturn(Optional.of(address));
        when(clientRepository.findByEmail(any())).thenReturn(Optional.of(client));

        clientService.deleteClient(client.getEmail());

        then(addressRepository).should(times(address.size())).delete(any());
        then(clientRepository).should(times(2)).delete(client);
    }
}