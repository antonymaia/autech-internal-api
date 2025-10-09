package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, String> {

    Optional<Cliente> findByCnpjCpf(String cnpjCpf);

    @Query(value = "select * from CLIENTES C where C.CNPJ_CPF like %:searchTerm% ", nativeQuery = true)
    Page<Cliente> searchByCnpjCpf(
            @Param("searchTerm") String searchTerm,
            Pageable pageable);

    @Query(value = "SELECT * FROM CLIENTES C WHERE UPPER(C.NOME) LIKE :searchTerm ", nativeQuery = true)
    Page<Cliente> searchByNome(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query(value = "SELECT * FROM CLIENTES c " +
            " INNER JOIN " +
            " ENDERECO e " +
            " ON " +
            " c.cod_endereco = e.cod_endereco " +
            " WHERE " +
            " e.bairro LIKE :searchTerm ORDER BY e.bairro ", nativeQuery = true)
    Page<Cliente> findAllByBairro(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query(value = "SELECT * FROM CLIENTES cli " +
            " INNER JOIN " +
            " ENDERECO e " +
            " ON " +
            " cli.cod_endereco = e.cod_endereco " +
            " INNER JOIN " +
            " CIDADE ci " +
            " ON " +
            " e.cidade_id = ci.id " +
            " where ci.nome like :searchTerm ORDER BY ci.nome ", nativeQuery = true)
    Page<Cliente> findAllByCidade(@Param("searchTerm") String searchTerm, Pageable pageable);

    List<Cliente> findAllByDiaVencimentoAndAtivoAndEmailNotNull(String diaVencimento, String ativo);

    long countByAtivo(String ativo);

    long countByBloqueadoAndAtivo(String bloqueado, String ativo);

    List<Cliente> findAllByDiaVencimentoAndAtivo(String diaVencimento, String Ativo);
}
