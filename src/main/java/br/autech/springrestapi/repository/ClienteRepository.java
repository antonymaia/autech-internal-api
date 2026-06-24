package br.autech.springrestapi.repository;

import br.autech.springrestapi.dtos.ClienteDTO;
import br.autech.springrestapi.dtos.ClienteDadosCobrancaDto;
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

   @Query(value = "" +
      " SELECT new br.autech.springrestapi.dtos.ClienteDTO(" +
      " c.cnpjCpf, " +
      " c.razaoSocialNome," +
      " c.nome," +
      " c.nomeResponsavel, " +
      " e.bairro," +
      " e.cidade.nome," +
      " e.cidade.estado.uf," +
      " c.telefone, " +
      " c.bloqueado," +
      " c.ativo," +
      " c.valorMensalidade," +
      " c.diaVencimento" +
      ") FROM Cliente c " +
      " LEFT JOIN c.endereco e " +
      " WHERE c.ativo LIKE 'S' AND c.diaVencimento LIKE :diaVencimento ")
   List<ClienteDTO> buscarResumoClientePorDiaVencimento(String diaVencimento);

   @Query(value = "" +
      " SELECT new br.autech.springrestapi.dtos.ClienteDTO(" +
      " c.cnpjCpf, " +
      " c.razaoSocialNome," +
      " c.nome," +
      " c.nomeResponsavel, " +
      " e.bairro," +
      " cd.nome," +
      " ed.uf," +
      " c.telefone, " +
      " c.bloqueado," +
      " c.ativo," +
      " c.valorMensalidade," +
      " c.diaVencimento" +
      ") FROM Cliente c " +
      " LEFT JOIN c.endereco e " +
      " LEFT JOIN e.cidade cd " +
      " LEFT JOIN cd.estado ed" +
      " WHERE c.cnpjCpf LIKE :termoBusca ")
   Page<ClienteDTO> buscarClienteDtoPorCnpjCpfComecando(String termoBusca, Pageable pageable);

   @Query(value = "" +
      " SELECT new br.autech.springrestapi.dtos.ClienteDTO(" +
      " c.cnpjCpf, " +
      " c.razaoSocialNome," +
      " c.nome," +
      " c.nomeResponsavel, " +
      " e.bairro," +
      " cd.nome," +
      " ed.uf," +
      " c.telefone, " +
      " c.bloqueado," +
      " c.ativo," +
      " c.valorMensalidade," +
      " c.diaVencimento" +
      ") FROM Cliente c " +
      " LEFT JOIN c.endereco e " +
      " LEFT JOIN e.cidade cd " +
      " LEFT JOIN cd.estado ed" +
      " WHERE c.nome LIKE :termoBusca ")
   Page<ClienteDTO> buscarClienteDtoPorNomeComecando(String termoBusca, Pageable pageable);

   @Query(value = "" +
      " SELECT new br.autech.springrestapi.dtos.ClienteDTO(" +
      " c.cnpjCpf, " +
      " c.razaoSocialNome," +
      " c.nome," +
      " c.nomeResponsavel, " +
      " e.bairro," +
      " cd.nome," +
      " ed.uf," +
      " c.telefone, " +
      " c.bloqueado," +
      " c.ativo," +
      " c.valorMensalidade," +
      " c.diaVencimento" +
      ") FROM Cliente c " +
      " LEFT JOIN c.endereco e " +
      " LEFT JOIN e.cidade cd " +
      " LEFT JOIN cd.estado ed" +
      " WHERE e.bairro LIKE :termoBusca ")
   Page<ClienteDTO> buscarClienteDtoPorBairro(String termoBusca, Pageable pageable);

   @Query(value = "" +
      " SELECT new br.autech.springrestapi.dtos.ClienteDTO(" +
      " c.cnpjCpf, " +
      " c.razaoSocialNome," +
      " c.nome," +
      " c.nomeResponsavel, " +
      " e.bairro," +
      " cd.nome," +
      " ed.uf," +
      " c.telefone, " +
      " c.bloqueado," +
      " c.ativo," +
      " c.valorMensalidade," +
      " c.diaVencimento" +
      ") FROM Cliente c " +
      " LEFT JOIN c.endereco e " +
      " LEFT JOIN e.cidade cd " +
      " LEFT JOIN cd.estado ed" +
      " WHERE e.cidade.nome LIKE :termoBusca ")
   Page<ClienteDTO> buscarClienteDtoPorCidade(String termoBusca, Pageable pageable);

   @Query(value = "" +
      " SELECT new br.autech.springrestapi.dtos.ClienteDadosCobrancaDto(" +
      " c.cnpjCpf, " +
      " c.nomeResponsavel, " +
      " c.razaoSocialNome," +
      " c.valorMensalidade," +
      " c.diaVencimento, " +
      " c.telefone " +
      ") FROM Cliente c " +
      " WHERE c.cnpjCpf = :cnpjCpf ")
   ClienteDadosCobrancaDto buscarDadosCobranca(String cnpjCpf);
}
