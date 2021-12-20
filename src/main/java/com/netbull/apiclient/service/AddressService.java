package com.netbull.apiclient.service;

import com.netbull.apiclient.domain.address.Address;
import com.netbull.apiclient.domain.address.AddressRepository;
import com.netbull.apiclient.domain.address.Type;
import com.netbull.apiclient.domain.address.TypeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.ws.rs.NotFoundException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class AddressService {

    private AddressRepository addressRepository;

    private Validator validator;

    private ClientService clientService;

    private TypeRepository typeRepository;


    public AddressService(Validator validator, AddressRepository addressRepository,
                          ClientService clientService, TypeRepository typeRepository) {
        this.addressRepository = addressRepository;
        this.clientService = clientService;
        this.typeRepository = typeRepository;
        this.validator = validator;
    }

    @Transactional
    public void persistAddress(@NotNull(message = "Endereço não pode ser nulo.") Address address, String useremail) {

        Optional.ofNullable(address).orElseThrow(
                () -> new IllegalArgumentException("O endereço não pode ser nulo."));

        address.setClient(clientService.getClientByEmail(useremail));

        Set<ConstraintViolation<Address>> validate = this.validator.validate(address);

        if (!validate.isEmpty()) {
            throw new ConstraintViolationException("Endereço inválido.", validate);
        }

        if (!typeRepository.existsById(address.getType().getId())) {
            throw new NotFoundException("Não foi possível adicionar o endereço pois o tipo de endereço não foi encontrado.");
        }

        if (addressRepository.save(address) != null) {
            log.info("Endereço cadastrado: {}", address.getId());
        }
    }

    public List<Type> getTypeAddresses() {
        ArrayList<Type> typeList = new ArrayList<>();
        this.typeRepository.findAll().forEach(type -> typeList.add(type));
        return typeList;
    }

    public Set<Address> getAddressByClientEmail(String email) {
        Set<Address> addresses = this.addressRepository.findByClient(clientService.getClientByEmail(email))
                .orElseThrow(() -> new NotFoundException("Endereço não encontrado."));

        if (addresses.isEmpty()) {
            throw new NotFoundException("Endereço não encontrado.");
        }

        return addresses;
    }

    public Address getAddressById(BigInteger id) {
        return this.addressRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Endereço não encontrado."));
    }

    @Transactional
    public void patchAddressType(BigInteger id, Type type) {

        Address address = addressRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Endereço não encontrado."));
        address.setType(typeRepository.findById(type.getId()).orElseThrow(
                () -> new NotFoundException("O tipo de endereço não foi encontrado.")));

        if (addressRepository.save(address) != null) {
            log.info("Endereço alterado: {}", address.getId());
        }
    }

    @Transactional
    public void putAddress(BigInteger id, Address address) {
        Address address1 = addressRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Endereço não encontrado."));

        address1.setStreet(address.getStreet());
        address1.setNumber(address.getNumber());
        address1.setDistrict(address.getDistrict());
        address1.setCity(address.getCity());
        address1.setCep(address.getCep());
        address1.setState(address.getState());
        address1.setType(address.getType());

        Set<ConstraintViolation<Address>> validate = this.validator.validate(address1);

        if (!validate.isEmpty()) {
            throw new ConstraintViolationException("Endereço inválido.", validate);
        }

        if (addressRepository.save(address1) != null) {
            log.info("Endereço alterado: {}", address.getId());
        }
    }

    @Transactional
    public void deleteAddress(BigInteger id) {
        Address address = addressRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Endereço não encontrado."));
        addressRepository.delete(address);
        log.info("Endereço deletado: {}", address.getId());
    }
}
