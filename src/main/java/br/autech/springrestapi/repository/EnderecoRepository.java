package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.Cidade;
import br.autech.springrestapi.model.Endereco;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, Long> {
    Endereco findByCep(String cep);

    @Query(value = "select * from ENDERECO E where E.ENDERECO like %:searchTerm% ", nativeQuery = true)
    Page<Endereco> search(@Param("searchTerm") String searchTerm, Pageable pageable);

    Endereco findByLogradouroAndBairroAndCidade(String logradouro, String bairro, Cidade cidade);
}
