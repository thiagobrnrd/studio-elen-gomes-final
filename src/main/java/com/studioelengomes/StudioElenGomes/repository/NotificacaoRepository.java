package com.studioelengomes.StudioElenGomes.repository;

import com.studioelengomes.StudioElenGomes.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    // Corrigido: String em vez de Long (Agendamento usa UUID/String)
    boolean existsByAgendamentoIdAndTipo(String agendamentoId, Notificacao.TipoNotificacao tipo);

    List<Notificacao> findByStatus(Notificacao.StatusNotificacao status);

    boolean existsByAgendamentoIdAndTipoAndCanalAndStatus(
            String agendamentoId, Notificacao.TipoNotificacao tipo, String canal,
            Notificacao.StatusNotificacao status);

    void deleteByAgendamentoIdAndTipoAndCanal(String agendamentoId, Notificacao.TipoNotificacao tipo, String canal);

    List<Notificacao> findByCanalAndStatusAndEnviadaEmBetweenOrderByEnviadaEmDesc(
            String canal, Notificacao.StatusNotificacao status, LocalDateTime inicio, LocalDateTime fim);
}