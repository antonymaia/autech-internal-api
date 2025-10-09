package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.Ncm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NcmRepository extends JpaRepository<Ncm, String> {
}