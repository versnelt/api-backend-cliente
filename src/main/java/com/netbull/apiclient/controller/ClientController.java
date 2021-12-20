package com.netbull.apiclient.controller;

import com.netbull.apiclient.domain.client.Client;
import com.netbull.apiclient.service.AddressService;
import com.netbull.apiclient.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.net.URI;

@RestController
@Controller
@Slf4j
@RequestMapping(path = "/v1/clients")
public class ClientController {

    @Autowired
    ClientService clientService;

    @Autowired
    AddressService addressService;

    @Operation(summary = "Criar um cliente.")
    @PostMapping(produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> createClient(@RequestBody Client client) {

        this.clientService.persistClient(client);

        URI uri = MvcUriComponentsBuilder.fromController(getClass())
                .path("/{id}")
                .buildAndExpand(client.getId())
                .toUri();

        return ResponseEntity.created(uri).body("Cliente salvo.");
    }

    @Operation(summary = "Buscar todos os clientes.")
    @GetMapping( produces = {MediaType.APPLICATION_JSON})
    public ResponseEntity<Page<Client>> getAllClients(
            @ParameterObject @PageableDefault(sort = {"id"}, direction = Sort.Direction.ASC,
            page = 0, size = 10) Pageable pageable) {
        Page<Client> clients = clientService.getAllClients(pageable);

        return ResponseEntity.ok(clients);
    }

    @Operation(summary = "Buscar um cliente pelo id.")
    @GetMapping(path = "/{id}", produces = {MediaType.APPLICATION_JSON})
    public ResponseEntity<Client> getClientById(@PathVariable BigInteger id) {

        Client client = clientService.getClientById(id);
        return ResponseEntity.ok(client);
    }

    @Operation(summary = "Buscar um cliente por email.")
    @GetMapping(path = "/email/{email}", produces = {MediaType.APPLICATION_JSON})
    public ResponseEntity<Client> getClientByEmail(@PathVariable String email) {
        Client client = clientService.getClientByEmail(email);
        return ResponseEntity.ok(client);
    }

    @Operation(summary = "Buscar um cliente pelo cpf.")
    @GetMapping(path = "/cpf/{cpf}", produces = {MediaType.APPLICATION_JSON})
    public ResponseEntity<Client> getClientByCpf(@PathVariable String cpf) {

        Client client = clientService.getClientByCpf(cpf);
        return ResponseEntity.ok(client);
    }
    
    @Operation(summary = "Alterar cliente logado.")
    @PutMapping(consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> alterClient(@RequestBody Client client) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        this.clientService.putClient(auth.getName(), client);
        return ResponseEntity.status(HttpStatus.CREATED).body("Cliente alterado.");
    }

    @Operation(summary = "Deletar o cliente logado.")
    @DeleteMapping()
    public ResponseEntity<String> deleteClient() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        clientService.deleteClient(auth.getName());
        return ResponseEntity.ok("Cliente deletado.");
    }
}
