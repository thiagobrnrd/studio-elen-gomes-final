'use client';
import { useEffect, useState } from 'react';
import Shell from '../../components/Shell';
import Guard from '../../components/Guard';
import { ErrorBox, Loading } from '../../components/ui';
import { api, Agendamento, Lembrete } from '../../lib/api';
import { hora, dataHora, money, STATUS_LABEL } from '../../lib/format';

type D = { atendimentosHoje: number; clientes: number; servicosAtivos: number; pendentes: number; lembretesPendentes: number; faturamentoHoje: number; proximos: Agendamento[] };
type R = { aniversarios: { clienteId: string; clienteNome: string; dias: number; link?: string }[] };
type C = { horarios: Lembrete[]; retornos: Lembrete[] };

export default function Dashboard() {
  const [d, setD] = useState<D | null>(null);
  const [r, setR] = useState<R | null>(null);
  const [c, setC] = useState<C | null>(null);
  const [err, setErr] = useState('');

  function load() {
    setErr('');
    Promise.all([api<D>('/dashboard'), api<R>('/relacionamento/resumo'), api<C>('/whatsapp/central')])
      .then(([a, b, x]) => { setD(a); setR(b); setC(x); })
      .catch((e: any) => setErr(e.message));
  }
  useEffect(load, []);

  const saudacao = (() => { const h = new Date().getHours(); return h < 12 ? 'Bom dia' : h < 18 ? 'Boa tarde' : 'Boa noite'; })();
  const lembretes = (c?.horarios.length ?? 0) + (c?.retornos.length ?? 0);

  return <Guard><Shell>
    <header className="topbar"><div><div className="eyebrow">Studio Elen Gomes</div><h1>{saudacao}, Elen</h1><p className="page-subtitle">O que merece atenção hoje, em um só lugar.</p></div>
      <div className="top-actions"><a className="btn primary" href="/agenda">+ Novo agendamento</a></div></header>
    {err && <ErrorBox error={err} onRetry={load} />}

    <div className="grid stats">{[
      ['Atendimentos hoje', d?.atendimentosHoje ?? '–', 'na agenda'],
      ['A confirmar', d?.pendentes ?? '–', 'próximos horários'],
      ['Lembretes', c ? lembretes : '–', 'para enviar'],
      ['Clientes', d?.clientes ?? '–', 'cadastradas'],
      ['Recebido hoje', d ? money(d.faturamentoHoje) : '–', 'faturamento'],
    ].map(([l, v, s]) => <div className="card" key={String(l)}><div className="stat-label">{l}</div><div className="stat-value">{v}</div><div className="stat-detail">{s}</div></div>)}</div>

    <div className="grid two dashboard-main">
      <section className="card"><div className="section-title"><div><h2>Agenda de hoje</h2><p className="muted">Atendimentos do dia</p></div><a className="btn" href="/agenda">Ver agenda</a></div>
        <div className="table-wrap">{!d && !err ? <Loading /> : <>
          <table className="table"><thead><tr><th>Horário</th><th>Cliente</th><th>Serviço</th><th>Status</th></tr></thead>
            <tbody>{d?.proximos?.map((a) => <tr key={a.id} className={'is-' + a.status.toLowerCase()}><td><b style={{ color: 'var(--gold2)' }}>{hora(a.dataHora)}</b></td><td>{a.clienteNome}</td><td>{a.servicoNome}</td><td><span className={'badge ' + a.status.toLowerCase()}>{STATUS_LABEL[a.status]}</span></td></tr>)}</tbody></table>
          {!d?.proximos?.length && <div className="empty">Nenhum atendimento para hoje.</div>}</>}
        </div></section>
      <section className="card"><div className="section-title"><div><h2>Ações rápidas</h2><p className="muted">Atalhos do dia</p></div></div>
        <div className="quick-grid"><a className="btn" href="/agenda">＋ Agendar</a><a className="btn" href="/clientes">♙ Clientes</a><a className="btn" href="/servicos">✦ Serviços</a><a className="btn" href="/financeiro">R$ Financeiro</a></div>
        <div className="notice" style={{ marginTop: 16, marginBottom: 0 }}>
          {c ? <>Há <b>{c.horarios.length}</b> lembrete(s) de horário, <b>{c.retornos.length}</b> retorno(s) e <b>{r?.aniversarios.length ?? 0}</b> aniversário(s) para revisar.</> : 'Carregando lembretes...'}
          <div style={{ marginTop: 10 }}><a className="btn small primary" href="/lembretes">Abrir central de lembretes</a></div>
        </div></section>
    </div>

    {c && (c.horarios.length > 0 || (r?.aniversarios.length ?? 0) > 0) && <section className="card relationship-card">
      <div className="section-title"><div><div className="eyebrow">WhatsApp</div><h2>Próximos lembretes</h2></div><a className="btn" href="/lembretes">Ver todos</a></div>
      <div className="action-grid">
        {c.horarios.slice(0, 3).map((x) => <div className="action-item" key={x.agendamentoId}><div className="action-icon">📅</div><div className="action-content">
          <span className="eyebrow">Lembrar horário</span><b>{x.clienteNome}</b><small>{x.servicoNome} · {dataHora(x.dataHora)}</small></div></div>)}
        {r?.aniversarios.slice(0, 3).map((x) => <div className="action-item" key={x.clienteId}><div className="action-icon">🎂</div><div className="action-content">
          <span className="eyebrow">Aniversário</span><b>{x.clienteNome}</b><small>{x.dias === 0 ? 'É hoje!' : `Em ${x.dias} dia(s)`}</small></div></div>)}
      </div>
    </section>}
  </Shell></Guard>;
}
