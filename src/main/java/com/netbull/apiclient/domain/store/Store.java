package com.netbull.apiclient.domain.store;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "store")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Store implements Serializable {

    @Id
    private BigInteger id;

    private String cnpj;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Store store = (Store) o;
        return cnpj.equals(store.cnpj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cnpj);
    }
}
