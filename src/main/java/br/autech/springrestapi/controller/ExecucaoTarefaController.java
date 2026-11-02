package br.autech.springrestapi.controller;

import br.autech.springrestapi.dtos.OcorrenciaExecucaoDTO;
import br.autech.springrestapi.model.ExecucaoTarefa;
import br.autech.springrestapi.model.enums.StatusExecucaoTarefa;
import br.autech.springrestapi.model.enums.TarefaAgendada;
import br.autech.springrestapi.model.enums.TipoOcorrenciaExecucao;
import br.autech.springrestapi.service.ExecucaoTarefaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Execuções das tarefas agendadas. Exige o token de um usuário com PODE_VER_EXECUCOES (SecurityConfig). */
@RestController
@RequestMapping("/execucoes-tarefa")
@RequiredArgsConstructor
public class ExecucaoTarefaController {

    private final ExecucaoTarefaService execucaoTarefaService;

    /** Da mais recente para a mais antiga. "de" e "ate" (yyyy-MM-dd) filtram pela data de início, ambos inclusive. */
    @GetMapping
    public ResponseEntity<Page<ExecucaoTarefa>> buscar(
            @RequestParam(required = false) TarefaAgendada tarefa,
            @RequestParam(required = false) StatusExecucaoTarefa status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(execucaoTarefaService.buscar(tarefa, status, de, ate, page, size));
    }

    @GetMapping("/{id}/ocorrencias")
    public ResponseEntity<Page<OcorrenciaExecucaoDTO>> buscarOcorrencias(
            @PathVariable Long id,
            @RequestParam(required = false) TipoOcorrenciaExecucao tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(execucaoTarefaService.buscarOcorrencias(id, tipo, page, size));
    }
}
