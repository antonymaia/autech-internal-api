package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.PagamentoFatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PagamentoFaturaRepository extends JpaRepository<PagamentoFatura, Long> {
    @Query("SELECT COALESCE(SUM(pf.id.pagamento.valorPagamento), 0) FROM PAGAMENTO_FATURA pf "
        + "WHERE pf.id.pagamento.createdAt >= :inicio AND pf.id.pagamento.createdAt < :fim")
    BigDecimal somarPagamentosEntre(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}