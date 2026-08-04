package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findByNomeStartingWithIgnoreCaseOrderByNome(String nome);
    List<Produto> findByNomeContainingIgnoreCaseOrderByNome(String nome);
}
