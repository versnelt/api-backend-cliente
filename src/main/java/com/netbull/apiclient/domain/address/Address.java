package com.netbull.apiclient.domain.address;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.netbull.apiclient.domain.client.Client;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.math.BigInteger;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "address")
public class Address implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_address")
    @SequenceGenerator(name = "sequence_address",sequenceName = "sequence_address",
            allocationSize = 1,
            initialValue = 1)
    private BigInteger id;

    @NotBlank(message = "A rua não pode ser vazia.")
    private String street;

    @NotNull(message = "O número não pode ser vazio.")
    @Pattern(regexp = "[0-9]{1,9}", message = "Número inválido.")
    private String number;

    @NotBlank(message = "O bairro não pode ser vazio.")
    private String district;

    @NotBlank(message = "A cidade não pode ser vazia.")
    private String city;

    @NotBlank(message = "O CEP não pode ser vazio.")
    @Pattern(regexp = "[0-9]{8}", message = "CEP inválido.")
    private String cep;

    @NotBlank(message = "O Estado não pode ser vazio.")
    private String state;

    @NotNull(message = "O tipo não pode ser nulo.")
    @ManyToOne
    @JoinColumn(name = "type_id")
    private Type type;

    @JsonIgnoreProperties({"name", "cpf", "email", "birthday", "password"})
    @NotNull(message = "O cliente não pode ser nulo.")
    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;
}
