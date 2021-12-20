package com.netbull.apiclient.domain.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.netbull.apiclient.utility.JsonLocalDateDeserializer;
import com.netbull.apiclient.utility.JsonLocalDateSerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Objects;

@NoArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "cliente")
public class Client implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_client")
    @SequenceGenerator(name = "sequence_client",sequenceName = "sequence_client",
            allocationSize = 1,
            initialValue = 1)
    private BigInteger id;

    @NotBlank(message = "O nome não pode ser vazio.")
    @Size(max = 50, message = "O nome é muito grande.")
    private String name;

    @NotBlank(message = "O CPF não pode ser vazio.")
    @Pattern(regexp = "[0-9]{11}", message = "CPF inválido.")
    private String cpf;

    @NotBlank(message = "O e-mail não pode ser vazio.")
    @Email(message = "E-mail inválido.")
    private String email;

    @JsonSerialize(using = JsonLocalDateSerializer.class)
    @JsonDeserialize(using = JsonLocalDateDeserializer.class)
    @NotNull(message = "A data de nascimento não pode ser vazia.")
    @Past(message = "Data de nascimento inválida.")
    private LocalDate birthday;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "A senha não pode ser vazia.")
    @Size(min = 3, message = "Senha muito curta.")
    private String password;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return cpf.equals(client.cpf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpf);
    }
}
