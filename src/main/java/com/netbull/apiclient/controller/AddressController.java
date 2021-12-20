package com.netbull.apiclient.controller;

import com.netbull.apiclient.domain.address.Address;
import com.netbull.apiclient.domain.address.Type;
import com.netbull.apiclient.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.validation.Validator;
import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;
import java.util.Set;

@RestController
@Controller
@RequestMapping(path = "v1/clients/addresses")
public class AddressController {

    @Autowired
    Validator validation;

    @Autowired
    AddressService addressService;

    @Operation(summary = "Criar um endereço para um cliente existente.")
    @PostMapping(produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> createAddress(@RequestBody Address address) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        this.addressService.persistAddress(address, auth.getName());

        URI uri = MvcUriComponentsBuilder.fromController(getClass())
                .path("/{id}")
                .buildAndExpand(address.getId())
                .toUri();

        return ResponseEntity.created(uri).body("Endereço criado.");
    }

    @Operation(summary = "Buscar os endereços de cliente.")
    @GetMapping(produces = {MediaType.APPLICATION_JSON})
    public ResponseEntity<Set<Address>> getAddressByClient() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Set<Address> addresses = this.addressService.getAddressByClientEmail(auth.getName());

        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Buscar os tipos de endereços permitidos.")
    @GetMapping(path = "/types", produces = {MediaType.APPLICATION_JSON})
    public ResponseEntity<List<Type>> getAllTypeAddresses() {
        return ResponseEntity.ok(this.addressService.getTypeAddresses());
    }

    @Operation(summary = "Buscar um endereço por id.")
    @GetMapping(path = "/{id}", produces = {MediaType.APPLICATION_JSON})
    public ResponseEntity<Address> getAddressById(@PathVariable BigInteger id) {

        Address address = this.addressService.getAddressById(id);

        return ResponseEntity.ok(address);
    }

    @Operation(summary = "Alterar uma modalidade de endereço.")
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> patchAddressType(@PathVariable BigInteger id, @RequestBody Type type) {

        this.addressService.patchAddressType(id, type);

        return ResponseEntity.ok("Endereço alterado.");

    }

    @Operation(summary = "Alterar um endereço.")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<String> alterAddress(@PathVariable BigInteger id, @RequestBody Address address) {

        this.addressService.putAddress(id, address);

        return ResponseEntity.ok("Endereço alterado.");

    }

    @Operation(summary = "Deletar um endereço.")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<String> deleteAddress(@PathVariable BigInteger id) {

        addressService.deleteAddress(id);

        return ResponseEntity.ok("Endereço deletado.");
    }
}
