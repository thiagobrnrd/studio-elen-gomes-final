package com.studioelengomes.StudioElenGomes.repository;

import com.studioelengomes.StudioElenGomes.model.FichaCapilar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FichaCapilarRepository extends JpaRepository<FichaCapilar, String> {
    Optional<FichaCapilar> findByClienteId(String clienteId);
}
