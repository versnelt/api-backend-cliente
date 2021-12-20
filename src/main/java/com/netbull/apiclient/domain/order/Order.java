package com.netbull.apiclient.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.netbull.apiclient.domain.address.Address;
import com.netbull.apiclient.domain.client.Client;
import com.netbull.apiclient.domain.store.Store;
import com.netbull.apiclient.utility.JsonLocalDateDeserializer;
import com.netbull.apiclient.utility.JsonLocalDateSerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "order_client")
public class Order implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_order")
    @SequenceGenerator(name = "sequence_order",sequenceName = "sequence_order",
            allocationSize = 1,
            initialValue = 1)
    private BigInteger id;

    @NotNull(message = "O estado do pedido não pode ser nulo.")
    @Enumerated(EnumType.STRING)
    private OrderState state;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonSerialize(using = JsonLocalDateSerializer.class)
    @JsonDeserialize(using = JsonLocalDateDeserializer.class)
    @NotNull(message = "A data de criação do pedido não pode ser nula.")
    private LocalDate orderCreated;

    @JsonSerialize(using = JsonLocalDateSerializer.class)
    @JsonDeserialize(using = JsonLocalDateDeserializer.class)
    private LocalDate orderDispatched;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonSerialize(using = JsonLocalDateSerializer.class)
    @JsonDeserialize(using = JsonLocalDateDeserializer.class)
    private LocalDate orderDelivered;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @NotNull(message = "O valor total não pode ser nulo.")
    @Min(value = 0, message = "O valor total não pode ser menor do que zero.")
    private BigDecimal totalValue;

    @JsonIgnoreProperties({"client"})
    @NotNull(message = "O endereço do cliente não pode ser nulo.")
    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    @JsonIgnoreProperties({"password"})
    @NotNull(message = "O cliente não pode ser nulo.")
    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @NotNull(message = "A loja não pode ser nula.")
    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    @NotNull(message = "Os produtos não podem ser nulos.")
    @OneToMany(mappedBy = "order", fetch = FetchType.EAGER)
    private List<ProductOrder> products;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
