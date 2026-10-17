package com.studioelengomes.StudioElenGomes.repository;

import com.studioelengomes.StudioElenGomes.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, String> {

    Optional<Pagamento> findByAgendamentoId(String agendamentoId);

    boolean existsByAgendamentoId(String agendamentoId);

    // Pagamentos num período
    @Query("SELECT p FROM Pagamento p WHERE p.pagoEm BETWEEN :inicio AND :fim ORDER BY p.pagoEm DESC")
    List<Pagamento> findByPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    // Total arrecadado num período
    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p WHERE p.pagoEm BETWEEN :inicio AND :fim")
    BigDecimal totalPorPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    // Total por forma de pagamento num período
    @Query("""
                SELECT p.formaPagamento, COALESCE(SUM(p.valor), 0)
                FROM Pagamento p
                WHERE p.pagoEm BETWEEN :inicio AND :fim
                GROUP BY p.formaPagamento
            """)
    List<Object[]> totalPorFormaPagamento(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    // Serviços mais realizados num período
    @Query("""
                SELECT p.agendamento.servico.nome, COUNT(p), COALESCE(SUM(p.valor), 0)
                FROM Pagamento p
                WHERE p.pagoEm BETWEEN :inicio AND :fim
                GROUP BY p.agendamento.servico.nome
                ORDER BY COUNT(p) DESC
            """)
    List<Object[]> servicosMaisRealizados(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    // Clientes que mais gastaram num período
    @Query("""
                SELECT p.agendamento.cliente.nome, COUNT(p), COALESCE(SUM(p.valor), 0)
                FROM Pagamento p
                WHERE p.pagoEm BETWEEN :inicio AND :fim
                GROUP BY p.agendamento.cliente.nome
                ORDER BY SUM(p.valor) DESC
            """)
    List<Object[]> clientesQueMaisGastaram(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}