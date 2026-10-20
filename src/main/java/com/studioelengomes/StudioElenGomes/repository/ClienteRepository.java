package com.studioelengomes.StudioElenGomes.repository;

import com.studioelengomes.StudioElenGomes.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, String> {
    List<Cliente> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);
}
