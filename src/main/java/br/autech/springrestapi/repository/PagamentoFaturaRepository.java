package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.PagamentoFatura;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoFaturaRepository extends JpaRepository<PagamentoFatura, Long> {
}