package com.netbull.apiclient.domain.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "address_type")
public class Type implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_Type")
    @SequenceGenerator(name = "sequence_Type",sequenceName = "sequence_Type",
            allocationSize = 1,
            initialValue = 1)
    private Integer id;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @NotBlank(message = "A descrição não pode ser nula.")
    @Size(max = 100, message = "Descrição muito longa, só é permitido 100 caracteres.")
    private String description;
}