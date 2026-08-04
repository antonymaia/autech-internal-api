package br.autech.springrestapi.model;

import br.autech.springrestapi.model.enums.StatusAssinatura;
import br.autech.springrestapi.model.enums.TipoAssinatura;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;


import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Table(name = "ASSINATURA")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ID_ASSINATURA")
    private Long id;
    @Column(precision = 18, scale = 2)
    private BigDecimal valor_total;
    @Column(name = "DATA_INICIO")
    private LocalDate dataInicial;

    @Enumerated(EnumType.STRING)
    private TipoAssinatura tipo_assinatura;

    @OneToOne
    @JoinColumn(name = "id_cliente")
    @JsonIgnoreProperties("assinatura")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    private StatusAssinatura status;

    @OneToMany(mappedBy = "assinatura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssinaturaProduto> produtos = new ArrayList<>();

}
