package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.OcorrenciaExecucao;
import br.autech.springrestapi.model.enums.TipoOcorrenciaExecucao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OcorrenciaExecucaoRepository extends JpaRepository<OcorrenciaExecucao, Long> {

    @Query("SELECT o FROM OcorrenciaExecucao o" +
            " WHERE o.execucao.id = :execucaoId" +
            " AND (:tipo IS NULL OR o.tipo = :tipo)" +
            " ORDER BY o.id")
    Page<OcorrenciaExecucao> buscar(@Param("execucaoId") Long execucaoId,
                                    @Param("tipo") TipoOcorrenciaExecucao tipo,
                                    Pageable pageable);
}
