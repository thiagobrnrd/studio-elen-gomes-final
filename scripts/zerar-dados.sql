-- ZERAR DADOS DE TESTE (antes de entregar o sistema)
-- Apaga clientes, serviços, agendamentos, pagamentos (financeiro),
-- remarcações, lembretes e fichas. As Configurações do studio são mantidas.
-- Rode no Neon: Console > SQL Editor > cole tudo > Run.  NÃO TEM DESFAZER.
TRUNCATE TABLE pagamentos, remarcacoes, notificacoes, fichas_capilares,
               agendamentos, clientes, servicos CASCADE;

-- Conferência (tudo deve dar 0):
SELECT (SELECT count(*) FROM clientes)     AS clientes,
       (SELECT count(*) FROM servicos)     AS servicos,
       (SELECT count(*) FROM agendamentos) AS agendamentos,
       (SELECT count(*) FROM pagamentos)   AS pagamentos;
