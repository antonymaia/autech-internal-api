package br.autech.springrestapi.model;

import br.autech.springrestapi.model.enums.EstadoFatura;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "FATURAS")
public class Fatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer estado;
    private BigDecimal valor;
    @JsonFormat(pattern="dd/MM/yyyy")
    private LocalDate dataVencimento;
    @JsonFormat(pattern="dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataPagamento;
    @Column(name = "CREATED_AT")
    @JsonFormat(pattern="dd/MM/yyyy HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
    @OneToMany(mappedBy = "id.fatura")
    private Set<PagamentoFatura> pagamentos = new HashSet<>();

    public Fatura(Long id, EstadoFatura estadoFatura, BigDecimal valor, LocalDate dataVencimento, LocalDateTime dataPagamento, LocalDateTime createdAt, Cliente cliente, Set<PagamentoFatura> pagamentos) {
        this.id = id;
        this.estado = estadoFatura.getCod();
        this.valor = valor;
        this.dataVencimento = dataVencimento;
        this.dataPagamento = dataPagamento;
        this.createdAt = createdAt;
        this.cliente = cliente;
        this.pagamentos = pagamentos;
    }

    public EstadoFatura getEstado() {
        return EstadoFatura.toEnum(estado);
    }

    public void setEstado(EstadoFatura estado) {
        this.estado = estado.getCod();
    }
}
