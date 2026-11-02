package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.ExecucaoTarefa;
import br.autech.springrestapi.model.enums.StatusExecucaoTarefa;
import br.autech.springrestapi.model.enums.TarefaAgendada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ExecucaoTarefaRepository extends JpaRepository<ExecucaoTarefa, Long> {

    @Query("SELECT e FROM ExecucaoTarefa e" +
            " WHERE (:tarefa IS NULL OR e.tarefa = :tarefa)" +
            " AND (:status IS NULL OR e.status = :status)" +
            " AND (:inicioDe IS NULL OR e.inicio >= :inicioDe)" +
            " AND (:inicioAntesDe IS NULL OR e.inicio < :inicioAntesDe)" +
            " ORDER BY e.inicio DESC, e.id DESC")
    Page<ExecucaoTarefa> buscar(@Param("tarefa") TarefaAgendada tarefa,
                                @Param("status") StatusExecucaoTarefa status,
                                @Param("inicioDe") LocalDateTime inicioDe,
                                @Param("inicioAntesDe") LocalDateTime inicioAntesDe,
                                Pageable pageable);

    /** As ocorrências saem junto pelo ON DELETE CASCADE da FK. */
    @Modifying
    @Query("DELETE FROM ExecucaoTarefa e WHERE e.inicio < :limite")
    int apagarIniciadasAntesDe(@Param("limite") LocalDateTime limite);
}
