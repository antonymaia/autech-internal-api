package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.Fatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface FaturaRepository extends JpaRepository<Fatura, Long> {
    List<Fatura> findAllByCliente(Cliente cliente);
    List<Fatura> findAllByDataVencimentoIsBeforeAndEstado(LocalDate dataAtual, Integer estado);
    List<Fatura> findAllByDataVencimentoAndEstado(LocalDate dataVencimento, Integer estado);
    List<Fatura> findAllByDataVencimentoAndEstadoIn(LocalDate dataVencimento, Collection<Integer> estados);
    List<Fatura> findAllByEstadoAndDataVencimentoBetween(Integer estado, LocalDate inicio, LocalDate fim);
    List<Fatura> findAllByCliente_CnpjCpfAndEstadoAndDataVencimentoBetween(String cnpjCpf, Integer estado, LocalDate inicio, LocalDate fim);
    List<Fatura> findAllByCliente_CnpjCpfAndEstadoInOrderByDataVencimento(String cnpjCpf, Collection<Integer> estados);
    List<Fatura> findAllByDataVencimentoLessThanEqualAndEstadoIn(LocalDate dataVencimento, Collection<Integer> estados);
    boolean existsByCliente_CnpjCpfAndDataVencimento(String cnpjCpf, LocalDate dataVencimento);
    boolean existsByCliente_CnpjCpfAndDataVencimentoLessThanEqualAndEstadoIn(String cnpjCpf, LocalDate dataVencimento, Collection<Integer> estados);
}
