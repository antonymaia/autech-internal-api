package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.Assinatura;
import br.autech.springrestapi.model.AssinaturaProduto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssinaturaProdutoRepository extends JpaRepository<AssinaturaProduto, Long> {
    List<AssinaturaProduto> findAllByAssinatura(Assinatura assinatura);
    Optional<AssinaturaProduto> findByAssinaturaAndProduto_IdProduto(Assinatura assinatura, Long produtoId);
}
