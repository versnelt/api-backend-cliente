package com.netbull.apiclient.service;

import com.netbull.apiclient.domain.address.AddressRepository;
import com.netbull.apiclient.domain.client.Client;
import com.netbull.apiclient.domain.client.ClientRepository;
import com.netbull.apiclient.utility.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.ws.rs.NotFoundException;
import java.math.BigInteger;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ClientService {

    private ClientRepository clientRepository;

    private Validator validator;

    private StringUtils stringUtils;

    private AddressRepository addressRepository;

    public ClientService(Validator validator, ClientRepository clientService,
                         AddressRepository addressRepository, StringUtils stringUtils) {
        this.clientRepository = clientService;
        this.addressRepository = addressRepository;
        this.stringUtils = stringUtils;
        this.validator = validator;
    }

    @Transactional
    public void persistClient(@NotNull(message = "Cliente não pode ser nulo.") Client client) {

        Set<ConstraintViolation<Client>> validate = this.validator.validate(client);

        if (!validate.isEmpty()) {
            throw new ConstraintViolationException("Cliente inválido.", validate);
        }

        if (!clientRepository.findByEmail(client.getEmail()).isEmpty()) {
            throw new DuplicateKeyException("Email já utilizado.");
        }

        if (!clientRepository.findByCpf(client.getCpf()).isEmpty()) {
            throw new DuplicateKeyException("CPF já cadastrado.");
        }

        client.setPassword(stringUtils.encryptPassword(client.getPassword()));


        if (clientRepository.save(client) != null) {
            log.info("Cliente cadastrado: {}", client.getName());
        }
    }

    public Page<Client> getAllClients(Pageable pageable) {
        Page<Client> clients = clientRepository.findAll(pageable);

        if (clients.isEmpty()) {
            throw new NotFoundException("Nenhum cliente foi encontrado.");
        }

        return new PageImpl<>(clients
                .getContent()
                .stream()
                .collect(Collectors.toList()), clients.getPageable(), clients.getTotalElements());
    }

    public Client getClientById(BigInteger id) {
        return clientRepository.findById(id).orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
    }

    public Client getClientByEmail(String email) {
        return clientRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
    }

    public Client getClientByCpf(String cpf) {
        return clientRepository.findByCpf(cpf).orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
    }

    @Transactional
    public void putClient(String useremail, Client client) {
        Client clientEntity = clientRepository.findByEmail(useremail)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));

        client.setCpf(clientEntity.getCpf());

        Set<ConstraintViolation<Client>> validate = this.validator.validate(client);

        if (!validate.isEmpty()) {
            throw new ConstraintViolationException("Cliente inválido.", validate);
        }

        Client clientOpt = clientRepository.findByEmail(client.getEmail()).orElse(null);

        if (clientOpt != null && !clientOpt.getCpf().equals(client.getCpf())) {
            throw new DuplicateKeyException("Email já utilizado.");
        }

        clientEntity.setEmail(client.getEmail());
        clientEntity.setName(client.getName());
        clientEntity.setBirthday(client.getBirthday());
        clientEntity.setPassword(stringUtils.encryptPassword(client.getPassword()));

        if (clientRepository.save(clientEntity) != null) {
            log.info("Cliente alterado: {}", client.getName());
        }
    }

    @Transactional
    public void deleteClient(String useremail) {
        Client client = clientRepository.findByEmail(useremail).orElseThrow(
                () -> new NotFoundException("Cliente não encontrado."));

        if (addressRepository.findByClient(client).isPresent()) {
            addressRepository.findByClient(client)
                    .get()
                    .stream()
                    .forEach(addressRepository::delete);
        }

        clientRepository.delete(client);
        log.info("Cliente deletado: {}", client.getName());
    }
}
