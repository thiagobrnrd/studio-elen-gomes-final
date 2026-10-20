'use client';
import { useCallback, useEffect, useState } from 'react';
import Shell from '../../components/Shell';
import Guard from '../../components/Guard';
import { Loading, ErrorBox, toast, toastErro } from '../../components/ui';
import { api, Lembrete } from '../../lib/api';
import { dataHora } from '../../lib/format';

type Central = { horarios: Lembrete[]; retornos: Lembrete[]; enviadosHoje: Lembrete[] };
type Resumo = {
  aniversarios: { clienteId: string; clienteNome: string; telefone: string; dias: number; link?: string; mensagem: string }[];
  sugestoes: { tipo: string; titulo: string; descricao: string; clienteId: string; clienteNome: string; link?: string }[];
};

export default function Lembretes() {
  const [c, setC] = useState<Central | null>(null);
  const [r, setR] = useState<Resumo | null>(null);
  const [err, setErr] = useState('');

  const load = useCallback(async () => {
    setErr('');
    try {
      const [a, b] = await Promise.all([api<Central>('/whatsapp/central'), api<Resumo>('/relacionamento/resumo')]);
      setC(a); setR(b);
    } catch (e: any) { setErr(e.message); }
  }, []);
  useEffect(() => { load(); }, [load]);

  async function marcar(l: Lembrete) {
    try {
      await api(`/whatsapp/marcar?agendamentoId=${l.agendamentoId}&tipo=${l.tipo}`, { method: 'POST' });
      toast(`Lembrete de ${l.clienteNome} marcado como enviado.`);
      load();
    } catch (e) { toastErro(e); }
  }
  async function desfazer(l: Lembrete) {
    try { await api(`/whatsapp/enviados/${l.notificacaoId}`, { method: 'DELETE' }); toast('Lembrete voltou para a lista.'); load(); }
    catch (e) { toastErro(e); }
  }

  const card = (l: Lembrete, enviado = false) => <div className={'action-item' + (enviado ? ' done' : '')} key={l.tipo + l.agendamentoId}>
    <div className="action-icon">{l.tipo === 'LEMBRETE_24H' ? '📅' : '🔁'}</div>
    <div className="action-content">
      <span className="eyebrow">{l.tipo === 'LEMBRETE_24H' ? 'Lembrete de horário' : 'Lembrete de retorno'}</span>
      <b>{l.clienteNome}</b>
      <small>{l.servicoNome} · {l.tipo === 'LEMBRETE_24H' ? dataHora(l.dataHora) : `último atendimento em ${dataHora(l.dataHora)}`} · {l.telefone}</small>
      {!enviado && <div className="msg-preview">{l.mensagem}</div>}
      <div className="row-actions">
        {enviado ? <button className="btn small" onClick={() => desfazer(l)}>Desfazer</button>
          : l.link ? <>
            <a className="btn small whats" href={l.link} target="_blank" rel="noreferrer" onClick={() => marcar(l)}>💬 Enviar no WhatsApp</a>
            <button className="btn small" onClick={() => marcar(l)}>Já avisei</button>
          </> : <span className="muted" style={{ fontSize: 11 }}>⚠ Telefone incompleto — corrija em Clientes.</span>}
      </div>
    </div>
  </div>;

  return <Guard><Shell>
    <header className="topbar"><div><div className="eyebrow">Relacionamento</div><h1>Central de lembretes</h1>
      <p className="page-subtitle">Ao clicar em “Enviar no WhatsApp”, a conversa abre com a mensagem pronta. A Elen revisa e envia pelo próprio WhatsApp do studio — e o lembrete sai da lista.</p></div>
      <button className="btn" onClick={load}>↻ Atualizar</button></header>

    {err && <ErrorBox error={err} onRetry={load} />}
    {!c && !err ? <div className="card"><Loading /></div> : c && <>
      <section className="reminder-section">
        <div className="section-title"><div><h2>Horários de hoje e amanhã <span className="count-pill">{c.horarios.length}</span></h2><p className="muted">Clientes que ainda não receberam o lembrete do horário.</p></div></div>
        <div className="action-grid">{c.horarios.map((l) => card(l))}{!c.horarios.length && <div className="card empty" style={{ gridColumn: '1/-1' }}>Todos os horários já foram lembrados. ✓</div>}</div>
      </section>

      <section className="reminder-section">
        <div className="section-title"><div><h2>Retornos <span className="count-pill">{c.retornos.length}</span></h2><p className="muted">Clientes que já passaram do prazo de retorno do serviço e ainda não reagendaram.</p></div></div>
        <div className="action-grid">{c.retornos.map((l) => card(l))}{!c.retornos.length && <div className="card empty" style={{ gridColumn: '1/-1' }}>Nenhum retorno pendente.</div>}</div>
      </section>

      {r && <section className="reminder-section">
        <div className="section-title"><div><h2>Aniversários da semana <span className="count-pill">{r.aniversarios.length}</span></h2><p className="muted">Um carinho no dia certo traz a cliente de volta.</p></div></div>
        <div className="action-grid">{r.aniversarios.map((x) => <div className="action-item" key={x.clienteId}><div className="action-icon">🎂</div><div className="action-content">
          <span className="eyebrow">Aniversário</span><b>{x.clienteNome}</b><small>{x.dias === 0 ? 'É hoje!' : `Em ${x.dias} dia(s)`}</small>
          <div className="msg-preview">{x.mensagem}</div>
          <div className="row-actions">{x.link ? <a className="btn small whats" href={x.link} target="_blank" rel="noreferrer">💬 Enviar parabéns</a> : <span className="muted" style={{ fontSize: 11 }}>⚠ Telefone incompleto</span>}</div>
        </div></div>)}{!r.aniversarios.length && <div className="card empty" style={{ gridColumn: '1/-1' }}>Nenhum aniversário nos próximos 7 dias.</div>}</div>
      </section>}

      {r && r.sugestoes.filter((s) => s.tipo === 'REATIVACAO').length > 0 && <section className="reminder-section">
        <div className="section-title"><div><h2>Clientes sumidas</h2><p className="muted">Mais de 60 dias sem atendimento.</p></div></div>
        <div className="action-grid">{r.sugestoes.filter((s) => s.tipo === 'REATIVACAO').map((x, i) => <div className="action-item" key={x.clienteId + i}><div className="action-icon">💡</div><div className="action-content">
          <span className="eyebrow">Reativar</span><b>{x.clienteNome}</b><small>{x.descricao}</small>
          {x.link && <div className="row-actions"><a className="btn small whats" href={x.link} target="_blank" rel="noreferrer">💬 Chamar no WhatsApp</a></div>}
        </div></div>)}</div>
      </section>}

      {c.enviadosHoje.length > 0 && <section className="reminder-section">
        <div className="section-title"><div><h2>Enviados hoje</h2><p className="muted">Clicou sem querer? Use “Desfazer” para o lembrete voltar.</p></div></div>
        <div className="action-grid">{c.enviadosHoje.map((l) => card(l, true))}</div>
      </section>}
    </>}
  </Shell></Guard>;
}
