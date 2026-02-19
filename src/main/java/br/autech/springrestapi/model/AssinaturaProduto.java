package br.autech.springrestapi.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "ASSINATURA_PRODUTO")
public class AssinaturaProduto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ASSINATURA_PRODUTO")
    private long idAssinaturaProduto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_assinatura", nullable = false)
    @JsonBackReference
    private Assinatura assinatura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PRODUTO", nullable = false )
    @JsonIgnore
    private Produto produto;

    @Column( name="QUANTIDADE", nullable = false)
    private Integer quantidade;

    @Column( name="VALOR_PRODUTO", nullable = false)
    private BigDecimal valorProduto;
    @Column(name = "data_inclusao")
    private LocalDateTime dataInclusao;



    @Column(name = "DESCONTO")
    private BigDecimal desconto;

    @Column(name = "ACRESCIMO")
    private BigDecimal acrescimo;

    protected AssinaturaProduto() {}

  private void validarPercentual(BigDecimal percentual) {
        if(percentual != null  && (percentual.compareTo(BigDecimal.ZERO) < 0 ||
                percentual.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("Percentual inválido");
        }
  }
  public BigDecimal getValorUnitarioFinal(){
        if(this.valorProduto == null){
            return BigDecimal.ZERO;
        }
        BigDecimal cem = BigDecimal.valueOf(100);

        BigDecimal descontoDecimal = this.desconto != null ? this.desconto.divide(cem, 10, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal acrescimoDecimal = this.acrescimo != null ? this.acrescimo.divide(cem, 10 , RoundingMode.HALF_UP) : BigDecimal.ZERO;



        BigDecimal fator = BigDecimal.ONE
                .subtract(descontoDecimal)
                .multiply(BigDecimal.ONE.add(acrescimoDecimal));

      return this.valorProduto.multiply(fator);

  }

  public BigDecimal getValor(){
        BigDecimal  quantidadeDecimal = this.quantidade != null ? BigDecimal.valueOf(this.quantidade) : BigDecimal.ZERO;

        return getValorUnitarioFinal().multiply(quantidadeDecimal).setScale(2, RoundingMode.HALF_UP);
  }



}
