package com.netbull.apiclient.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "product_order")
public class ProductOrder implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_productOrder")
    @SequenceGenerator(name = "sequence_productOrder",sequenceName = "sequence_productOrder",
            allocationSize = 1,
            initialValue = 1)
    private BigInteger id;

    private BigDecimal price;

    @NotNull(message = "Quantidade não pode ser nula.")
    @Min(value = 1, message = "Quantidade não pode ser menor que um.")
    private BigInteger quantity;

    @NotBlank(message = "Código não pode ser nulo.")
    private String code;

    @JsonIgnoreProperties({"state", "orderCreated", "orderDispatched", "orderDelivered",
    "totalValue", "address", "client", "store", "products"})
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductOrder that = (ProductOrder) o;
        return code.equals(that.code) && order.equals(that.order);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, order);
    }
}
