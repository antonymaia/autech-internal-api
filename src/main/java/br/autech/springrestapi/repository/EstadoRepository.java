package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Long> {

    public Optional<Estado> findByCodigoUf(String codigo);
}
