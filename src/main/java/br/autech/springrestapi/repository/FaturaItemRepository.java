package br.autech.springrestapi.repository;

import br.autech.springrestapi.model.FaturaItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaturaItemRepository extends JpaRepository<FaturaItem, Long> {
}
