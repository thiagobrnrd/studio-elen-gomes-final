package com.studioelengomes.StudioElenGomes.repository;
import com.studioelengomes.StudioElenGomes.model.Remarcacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RemarcacaoRepository extends JpaRepository<Remarcacao, Long> { List<Remarcacao> findByAgendamentoIdOrderByCriadoEmDesc(String agendamentoId); }
