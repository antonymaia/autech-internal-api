package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.Assinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura,Integer> {

    @Query(value = "SELECT * FROM ASSINATURA   WHERE id_cliente = :clienteId" , nativeQuery = true)
    Optional<Assinatura> findByClienteId(@Param("clienteId") String clienteId);
}
