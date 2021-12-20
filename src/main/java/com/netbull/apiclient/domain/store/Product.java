package com.netbull.apiclient.domain.store;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "product_store")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product implements Serializable {

    @Id
    private BigInteger id;

    private BigDecimal price;

    private BigInteger quantity;

    private String code;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return code.equals(product.code) && store.equals(product.store);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, store);
    }
}
