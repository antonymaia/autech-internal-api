package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.Cidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CidadeRepositoy extends JpaRepository<Cidade, Long> {
    public Cidade findByCodigoMunicipio(String codigoMunicipio);
}
