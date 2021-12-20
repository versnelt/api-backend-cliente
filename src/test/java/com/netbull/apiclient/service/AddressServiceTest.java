package com.netbull.apiclient.service;

import com.netbull.apiclient.domain.address.Address;
import com.netbull.apiclient.domain.address.AddressRepository;
import com.netbull.apiclient.domain.address.Type;
import com.netbull.apiclient.domain.address.TypeRepository;
import com.netbull.apiclient.domain.client.Client;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;

import javax.validation.*;
import javax.ws.rs.NotFoundException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AddressServiceTest {

    private AddressService addressService;
    private Validator validation;
    private AddressRepository addressRepository;
    private ClientService clientService;
    private TypeRepository typeRepository;
    private User user;

    @BeforeAll
    public void setupBeforAll() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        this.validation = validatorFactory.getValidator();

    }

    @BeforeEach
    public void setupBeforEach() {
        this.user = Mockito.mock(User.class);
        this.addressRepository = Mockito.mock(AddressRepository.class);
        this.clientService = Mockito.mock(ClientService.class);
        this.typeRepository = Mockito.mock(TypeRepository.class);
        this.addressService = new AddressService(validation, addressRepository, clientService, typeRepository);
    }

    @Test
    @DisplayName("Testa quando o endereço for nulo.")
    public void testa_quando_EnderecoNull_lancaExcecao() {
        assertNotNull(addressService);

        Address address = null;

        var assertThrows = assertThrows(IllegalArgumentException.class,
                () -> addressService.persistAddress(address, "email"));

        assertNotNull(assertThrows);
        assertEquals("O endereço não pode ser nulo." , assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa todos os atributos do endereço quando forem nulos.")
    public void testa_quando_atributosDoEnderecoEhNull_lancaException() {
        assertNotNull(addressService);

        Address address = new Address();

        var assertThrows = assertThrows(ConstraintViolationException.class,
                () -> addressService.persistAddress(address, "email"));

        assertEquals(8, assertThrows.getConstraintViolations().size());

        List<String> messages = assertThrows.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages, hasItems(
                "O cliente não pode ser nulo.",
                "A rua não pode ser vazia.",
                "O número não pode ser vazio.",
                "O bairro não pode ser vazio.",
                "A cidade não pode ser vazia.",
                "O CEP não pode ser vazio.",
                "O Estado não pode ser vazio.",
                "O tipo não pode ser nulo."
        ));
    }

    @Test
    @DisplayName("Testa todos os atributos do endereço quando não estão no padrão.")
    public void testa_quando_atributosEstaoForaDoPadrao_lancaException() {
        assertNotNull(addressService);

        Address address = new Address();

        address.setStreet(null);
        address.setNumber("aaa");
        address.setDistrict(null);
        address.setCity(null);
        address.setCep("aaaaaaaaa");
        address.setState(null);
        address.setType(null);

        var assertThrows1 = assertThrows(ConstraintViolationException.class,
                () -> addressService.persistAddress(address, ""));
        assertEquals(8, assertThrows1.getConstraintViolations().size());

        List<String> messages = assertThrows1.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages, hasItems(
                "O cliente não pode ser nulo.",
                "A rua não pode ser vazia.",
                "Número inválido.",
                "O bairro não pode ser vazio.",
                "A cidade não pode ser vazia.",
                "CEP inválido.",
                "O Estado não pode ser vazio.",
                "O tipo não pode ser nulo."
        ));

        address.setStreet("Rua");
        address.setNumber("123");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("RS");
        address.setType(new Type());

        when(clientService.getClientByEmail(any())).thenReturn(new Client());

        var assertThrows3 = assertThrows(NotFoundException.class,
                () -> addressService.persistAddress(address, "email"));

        assertEquals("Não foi possível adicionar o endereço pois o tipo de endereço não foi encontrado.",
                assertThrows3.getMessage());

    }

    @Test
    @DisplayName("Testa todos os atributos do endereço quando estão no padrão.")
    public void testa_quando_atributosEstaoDentroDoPadrao() {
        assertNotNull(addressService);

        Address address = new Address();

        address.setClient(new Client());
        address.setStreet("rua");
        address.setNumber("123456");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("Rio Grande do Sul");
        address.setType(new Type());

        when(clientService.getClientByEmail(any())).thenReturn(new Client());
        when(typeRepository.existsById(any())).thenReturn(true);

        addressService.persistAddress(address, "email");

        then(addressRepository).should(times(1)).save(any());
    }

    @Test
    @DisplayName("Testa busca de endereço pelo email do cliente quando id não existe.")
    public void testa_buscaDeEndereçoPeloIdClienteNaoExiste_lancaException() {
        assertNotNull(this.addressService);

        var assertThrows = assertThrows(NotFoundException.class,
                () -> addressService.getAddressByClientEmail(""));

        assertEquals("Endereço não encontrado.", assertThrows.getMessage());
    }

    @Test
    @DisplayName("Testa busca de endereço pelo email do cliente quando id existe.")
    public void testa_buscaDeEndereçoPeloIdClienteExiste() {
        assertNotNull(this.addressService);

        Set<Address> addresses = new HashSet<>();
        for (int x = 0; x < 10; x++) {
            Address address = new Address();
            address.setId(BigInteger.valueOf(x));
            addresses.add(address);
        }
        when(addressRepository.findByClient(any())).thenReturn(Optional.of(addresses));

        Set<Address> addressesResponse = addressService.getAddressByClientEmail("");

        then(addressRepository).should(times(1)).findByClient(any());

        assertEquals(10, addressesResponse.size());
    }

    @Test
    @DisplayName("Testa alteração de endereço quando ele existe e quando ele não existe.")
    public void testa_alteracaoDoEndereco() {
        assertNotNull(this.addressService);

        var assertThrows1 = assertThrows(NotFoundException.class,
                () -> addressService.putAddress(BigInteger.ONE, new Address()));

        assertEquals("Endereço não encontrado.", assertThrows1.getMessage());

        when(addressRepository.findById(BigInteger.ONE)).thenReturn(Optional.of(new Address()));

        var assertThrows2 = assertThrows(ConstraintViolationException.class,
                () -> addressService.putAddress(BigInteger.ONE, new Address()));

        assertEquals(8, assertThrows2.getConstraintViolations().size());

        List<String> messages = assertThrows2.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        assertThat(messages, hasItems(
                "O cliente não pode ser nulo.",
                "A rua não pode ser vazia.",
                "O número não pode ser vazio.",
                "O bairro não pode ser vazio.",
                "A cidade não pode ser vazia.",
                "O CEP não pode ser vazio.",
                "O Estado não pode ser vazio.",
                "O tipo não pode ser nulo."
        ));

        Address address = new Address();

        address.setId(BigInteger.TEN);
        address.setClient(new Client());
        address.setStreet("rua");
        address.setNumber("123456");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("Rio Grande do Sul");
        address.setType(new Type());

        Address newAddress = new Address();
        newAddress.setStreet("Avenida");
        newAddress.setNumber("123456");
        newAddress.setDistrict("Centro");
        newAddress.setCity("Feliz");
        newAddress.setCep("95773000");
        newAddress.setState("Rio Grande do Sul");
        newAddress.setType(new Type());

        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));

        addressService.putAddress(address.getId(), newAddress);

        assertEquals(address.getCity(), newAddress.getCity());
        assertEquals(address.getStreet(), newAddress.getStreet());

        then(addressRepository).should(times(2)).findById(BigInteger.ONE);
        then(addressRepository).should(times(1)).findById(BigInteger.TEN);
        then(addressRepository).should(times(1)).save(address);
    }

    @Test
    @DisplayName("Testa alteração do tipo de endereço quando ele existe e quando ele não existe.")
    public void testa_alteracaoDoTipoEndereco() {
        assertNotNull(this.addressService);

        var assertThrows1 = assertThrows(NotFoundException.class,
                () -> addressService.patchAddressType(BigInteger.ONE, new Type()));

        assertEquals("Endereço não encontrado.", assertThrows1.getMessage());

        when(addressRepository.findById(BigInteger.ONE)).thenReturn(Optional.of(new Address()));

        var assertThrows2 = assertThrows(NotFoundException.class,
                () -> addressService.patchAddressType(BigInteger.ONE, new Type()));

        assertEquals("O tipo de endereço não foi encontrado.", assertThrows2.getMessage());

        Type type = new Type();
        type.setId(Integer.valueOf(1));
        type.setDescription("Escritório");

        Address address = new Address();
        address.setId(BigInteger.TEN);
        address.setClient(new Client());
        address.setStreet("rua");
        address.setNumber("123456");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("Rio Grande do Sul");
        address.setType(type);

        Type newtype = new Type();
        newtype.setId(Integer.valueOf(1));
        newtype.setDescription("Casa");

        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));
        when(typeRepository.findById(any())).thenReturn(Optional.of(newtype));

        addressService.patchAddressType(address.getId(), newtype);

        assertEquals(address.getType().getDescription(), newtype.getDescription());

        then(addressRepository).should(times(3)).findById(any());
        then(typeRepository).should(times(2)).findById(any());
        then(addressRepository).should(times(1)).save(address);
    }

    @Test
    @DisplayName("Testa o método delete.")
    public void testa_metodoDelete() {
        assertNotNull(addressService);

        var assertThrow = assertThrows(NotFoundException.class,
                () -> addressService.deleteAddress(BigInteger.ONE));

        assertEquals("Endereço não encontrado.", assertThrow.getMessage());

        Address address = new Address();

        address.setId(BigInteger.TEN);
        address.setClient(new Client());
        address.setStreet("rua");
        address.setNumber("123456");
        address.setDistrict("Centro");
        address.setCity("Alto Feliz");
        address.setCep("95773000");
        address.setState("Rio Grande do Sul");
        address.setType(new Type());

        when(clientService.getClientByEmail(any())).thenReturn(new Client());
        when(typeRepository.existsById(any())).thenReturn(true);

        addressService.persistAddress(address, "email");

        then(addressRepository).should(times(1)).save(address);

        when(addressRepository.findById(any())).thenReturn(Optional.of(address));

        addressService.deleteAddress(BigInteger.ONE);

        then(addressRepository).should(times(1)).delete(address);
    }
}
