package com.studioelengomes.StudioElenGomes.repository;

import com.studioelengomes.StudioElenGomes.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, String> {
    List<Agendamento> findByDataHoraBetweenOrderByDataHoraAsc(LocalDateTime inicio, LocalDateTime fim);

    List<Agendamento> findByClienteIdOrderByDataHoraDesc(String clienteId);

    List<Agendamento> findByStatusOrderByDataHoraAsc(Agendamento.Status status);

    // Usado pelos lembretes de WhatsApp (retorno): atendimentos concluídos no período
    List<Agendamento> findByStatusAndDataHoraBetweenOrderByDataHoraAsc(
            Agendamento.Status status, LocalDateTime inicio, LocalDateTime fim);

    // A cliente já tem outro atendimento depois deste? (então já voltou)
    boolean existsByClienteIdAndDataHoraAfterAndStatusIn(
            String clienteId, LocalDateTime dataHora, Collection<Agendamento.Status> status);

    long countByStatusAndDataHoraAfter(Agendamento.Status status, LocalDateTime dataHora);

    boolean existsByClienteId(String clienteId);

    boolean existsByServicoId(String servicoId);

    long countByClienteId(String clienteId);
}
