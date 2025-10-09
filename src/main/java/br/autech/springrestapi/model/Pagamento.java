package br.autech.springrestapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Inheritance(strategy=InheritanceType.JOINED)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tipo" )
@JsonSubTypes({
        @JsonSubTypes.Type(value = PagamentoCartaoCredito.class, name = "cartaoCredito"),
        @JsonSubTypes.Type(value = PagamentoCartaoDebito.class, name = "cartaoDebito"),
        @JsonSubTypes.Type(value = PagamentoPix.class, name = "pix"),
        @JsonSubTypes.Type(value = PagamentoDinheiro.class, name = "dinheiro")
})
public class Pagamento implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    private BigDecimal valorPagamento;
    @JsonFormat(pattern="dd/MM/yyyy HH:mm:ss")
    private LocalDateTime createdAt;
    private String estado;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Pagamento that = (Pagamento) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
