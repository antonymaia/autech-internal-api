package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.Fatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FaturaRepository extends JpaRepository<Fatura, Long> {
    List<Fatura> findAllByCliente(Cliente cliente);
    List<Fatura> findAllByDataVencimentoIsBeforeAndEstado(LocalDate dataAtual, Integer estado);
}