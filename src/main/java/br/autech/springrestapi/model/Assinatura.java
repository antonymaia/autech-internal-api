package br.autech.springrestapi.model;

import br.autech.springrestapi.dtos.AssinaturaProdutoDTO;
import br.autech.springrestapi.model.enums.StatusAssinatura;
import br.autech.springrestapi.model.enums.TipoAssinatura;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;


import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Table(name = "ASSINATURA")
@Getter
@Setter
@Entity
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ID_ASSINATURA")
    private Long id;
    private LocalDate data_inicio;
    private LocalDate data_fim;
    @Column(precision = 18, scale = 2)
    private BigDecimal valor_total;
    private int quantidade_caixa;

    @Enumerated(EnumType.STRING)
    private TipoAssinatura tipo_assinatura;

    @OneToOne
    @JoinColumn(name = "id_cliente")
    @JsonManagedReference
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    private StatusAssinatura status;
    @OneToMany(
            mappedBy = "assinatura", fetch = FetchType.LAZY
    )
    @JsonManagedReference
    private List<AssinaturaProduto> assinaturaProdutos;

    public Assinatura() {
    }

    public Assinatura(LocalDate data_inicio, LocalDate data_fim, BigDecimal valor_total, TipoAssinatura tipo_assinatura,  Cliente cliente, StatusAssinatura status, int quantidade_caixa) {
        this.data_inicio =  data_inicio;
        this.data_fim = data_fim;
        this.valor_total = valor_total;
        this.tipo_assinatura = tipo_assinatura;
        this.cliente = cliente;
        this.status = status;
        this.quantidade_caixa = quantidade_caixa;

    }


}
