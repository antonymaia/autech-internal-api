package br.autech.springrestapi.model;

import br.autech.springrestapi.model.enums.TipoAssinatura;
import org.apache.tomcat.jni.Local;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Table(name = "ASSINATURAS")
@Entity
public class Assinatura {

    @Id
    private Long id;
    private LocalDate dataInicial;
    private LocalDate dataFinal;
    @Column(precision = 18, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    private TipoAssinatura tipoAssinatura;
    @OneToOne
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;
}
