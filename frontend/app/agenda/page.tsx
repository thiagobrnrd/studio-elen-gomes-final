'use client';
import { useCallback, useEffect, useState } from 'react';
import Shell from '../../components/Shell';
import Guard from '../../components/Guard';
import { Modal, Loading, ErrorBox, toast, toastErro } from '../../components/ui';
import { api, Agendamento, Cliente, Servico, Slot } from '../../lib/api';
import { hojeISO, somarDias, hora, dataHora, dataLonga, money, waLink, primeiroNome, STATUS_LABEL } from '../../lib/format';

type Historico = { id: number; anterior: string; nova: string; motivo: string; criadoEm: string };
const MOTIVOS = ['Cliente solicitou', 'Cliente não poderá comparecer', 'Conflito de agenda', 'Imprevisto do studio', 'Outro'];

export default function Agenda() {
  const [date, setDate] = useState(hojeISO());
  const [list, setList] = useState<Agendamento[] | null>(null);
  const [err, setErr] = useState('');
  const [clients, setClients] = useState<Cliente[]>([]);
  const [services, setServices] = useState<Servico[]>([]);
  const [novo, setNovo] = useState(false);
  const [res, setRes] = useState<Agendamento | null>(null);
  const [pay, setPay] = useState<Agendamento | null>(null);
  const [aviso, setAviso] = useState<Agendamento | null>(null); // avisar cliente após remarcar

  const load = useCallback(async () => {
    setErr('');
    try { setList(await api<Agendamento[]>(`/agendamentos?data=${date}`)); }
    catch (e: any) { setErr(e.message); setList([]); }
  }, [date]);

  useEffect(() => { setList(null); load(); }, [load]);
  useEffect(() => {
    api<Cliente[]>('/clientes').then(setClients).catch(() => {});
    api<Servico[]>('/servicos?apenasAtivos=true').then(setServices).catch(() => {});
  }, []);

  async function status(a: Agendamento, value: string) {
    if (value === 'CANCELADO' && !confirm(`Cancelar o atendimento de ${a.clienteNome}?`)) return;
    try {
      await api(`/agendamentos/${a.id}/status?value=${value}`, { method: 'PATCH' });
      toast(value === 'CANCELADO' ? 'Atendimento cancelado.' : 'Status atualizado.');
      load();
    } catch (e) { toastErro(e); }
  }

  async function responder(a: Agendamento, acao: 'aprovar' | 'recusar') {
    if (acao === 'recusar' && !confirm(`Recusar o pedido de ${a.clienteNome}? O horário será liberado.`)) return;
    try {
      await api(`/pedidos/${a.id}/${acao}`, { method: 'POST', body: acao === 'recusar' ? JSON.stringify({ motivo: 'Horário indisponível' }) : undefined });
      toast(acao === 'aprovar' ? 'Pedido aprovado. Avise a cliente pela aba Pedidos ou pelo WhatsApp.' : 'Pedido recusado.');
      window.dispatchEvent(new Event('pedidos-respondidos'));
      load();
    } catch (e) { toastErro(e); }
  }

  const ativos = (list || []).filter((a) => a.status !== 'CANCELADO');
  const previsto = ativos.reduce((s, a) => s + Number(a.valor || 0), 0);

  return <Guard><Shell>
    <header className="topbar">
      <div><div className="eyebrow">Operação</div><h1>Agenda</h1><p className="page-subtitle">Horários, confirmações, remarcações e recebimentos.</p></div>
      <button className="btn primary" onClick={() => setNovo(true)}>+ Novo agendamento</button>
    </header>

    <div className="toolbar">
      <div className="toolbar-left day-nav">
        <button className="btn small" onClick={() => setDate(somarDias(date, -1))} aria-label="Dia anterior">‹</button>
        <input type="date" className="input" value={date} onChange={(e) => e.target.value && setDate(e.target.value)} />
        <button className="btn small" onClick={() => setDate(somarDias(date, 1))} aria-label="Próximo dia">›</button>
        {date !== hojeISO() && <button className="btn small" onClick={() => setDate(hojeISO())}>Hoje</button>}
        <span className="day-label">{dataLonga(date)}</span>
      </div>
      <div className="toolbar-right"><span className="muted">{ativos.length} atendimento(s) · previsto {money(previsto)}</span></div>
    </div>

    {err && <ErrorBox error={err} onRetry={load} />}

    <div className="card"><div className="table-wrap">
      {list === null ? <Loading /> : <>
        <table className="table cards agenda-table">
          <thead><tr><th>Horário</th><th>Cliente</th><th>Serviço</th><th>Valor</th><th>Status</th><th>Ações</th></tr></thead>
          <tbody>{list.map((a) => {
            const aberto = a.status === 'AGENDADO' || a.status === 'CONFIRMADO';
            const msg = `Olá, ${primeiroNome(a.clienteNome)}! Passando para confirmar seu horário no Studio Elen Gomes: ${dataHora(a.dataHora)} (${a.servicoNome}). Podemos confirmar? 💛`;
            const link = waLink(a.whatsapp, msg);
            return <tr key={a.id} className={'is-' + a.status.toLowerCase()}>
              <td className="time-cell"><b>{hora(a.dataHora)}</b><small>{a.duracaoMin} min</small></td>
              <td className="client-cell"><b>{a.clienteNome}</b>{a.origem === 'ONLINE' && <span className="tag-online" title="Agendado pela cliente no link">online</span>}<small>{a.telefone}</small>{a.observacoes && <div className="obs">{a.observacoes}</div>}</td>
              <td>{a.servicoNome}</td>
              <td>{money(a.valor)}</td>
              <td><span className={'badge ' + a.status.toLowerCase()}>{STATUS_LABEL[a.status]}</span>{a.pago && <> <span className="badge pago">Pago</span></>}</td>
              <td><div className="row-actions">
                {a.status === 'SOLICITADO' && <>
                  <button className="btn small primary" onClick={() => responder(a, 'aprovar')}>✓ Aprovar</button>
                  <button className="btn small danger" onClick={() => responder(a, 'recusar')}>Recusar</button>
                </>}
                {link && aberto && <a className="btn small whats" href={link} target="_blank" rel="noreferrer">WhatsApp</a>}
                {a.status === 'AGENDADO' && <button className="btn small" onClick={() => status(a, 'CONFIRMADO')}>Confirmar</button>}
                {aberto && <button className="btn small" onClick={() => setRes(a)}>Remarcar</button>}
                {!a.pago && a.status !== 'CANCELADO' && a.status !== 'SOLICITADO' && <button className="btn small" onClick={() => setPay(a)}>Receber</button>}
                {aberto && <button className="btn small danger" onClick={() => status(a, 'CANCELADO')}>Cancelar</button>}
                {a.status === 'CANCELADO' && <button className="btn small" onClick={() => status(a, 'AGENDADO')}>Reativar</button>}
              </div></td>
            </tr>;
          })}</tbody>
        </table>
        {!list.length && !err && <div className="empty">Nenhum horário para esta data.</div>}
      </>}
    </div></div>

    {novo && <NovoAgendamento date={date} clients={clients} services={services} onClose={() => setNovo(false)}
      onSaved={(d) => { setNovo(false); toast('Agendamento criado.'); if (d !== date) setDate(d); else load(); }} />}
    {res && <Remarcar a={res} onClose={() => setRes(null)}
      onSaved={(novoA) => { setRes(null); toast('Horário remarcado.'); setAviso(novoA); const d = novoA.dataHora.slice(0, 10); if (d !== date) setDate(d); else load(); }} />}
    {pay && <Pagamento a={pay} onClose={() => setPay(null)} onSaved={() => { setPay(null); toast('Pagamento registrado.'); load(); }} />}
    {aviso && <AvisarRemarcacao a={aviso} onClose={() => setAviso(null)} />}
  </Shell></Guard>;
}

// ─── Novo agendamento ─────────────────────────────────────────────
function NovoAgendamento({ date, clients, services, onClose, onSaved }: {
  date: string; clients: Cliente[]; services: Servico[]; onClose: () => void; onSaved: (d: string) => void;
}) {
  const [f, setF] = useState({ clienteId: '', servicoId: '', dia: date < hojeISO() ? hojeISO() : date, hora: '', observacoes: '' });
  const [slots, setSlots] = useState<Slot[] | null>(null);
  const [manual, setManual] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!f.servicoId || !f.dia) { setSlots(null); return; }
    setSlots(null);
    api<Slot[]>(`/relacionamento/horarios-disponiveis?servicoId=${f.servicoId}&data=${f.dia}`).then(setSlots).catch((e) => { toastErro(e); setSlots([]); });
  }, [f.servicoId, f.dia]);

  async function save(e: React.FormEvent) {
    e.preventDefault();
    if (!f.hora) { toast('Escolha um horário.', 'erro'); return; }
    setSaving(true);
    try {
      await api('/agendamentos', { method: 'POST', body: JSON.stringify({ clienteId: f.clienteId, servicoId: f.servicoId, dataHora: `${f.dia}T${f.hora}:00`, observacoes: f.observacoes }) });
      onSaved(f.dia);
    } catch (e) { toastErro(e); setSaving(false); }
  }

  return <Modal title="Novo agendamento" eyebrow="Agenda" onClose={onClose} wide>
    <form onSubmit={save}>
      {!clients.length && <div className="notice">Cadastre uma cliente primeiro em <a href="/clientes">Clientes</a>.</div>}
      {!services.length && <div className="notice">Cadastre um serviço ativo primeiro em <a href="/servicos">Serviços</a>.</div>}
      <div className="form-grid">
        <div className="field full"><label>Cliente</label>
          <select required value={f.clienteId} onChange={(e) => setF({ ...f, clienteId: e.target.value })}>
            <option value="">Selecione...</option>{clients.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
          </select></div>
        <div className="field full"><label>Serviço</label>
          <select required value={f.servicoId} onChange={(e) => setF({ ...f, servicoId: e.target.value, hora: '' })}>
            <option value="">Selecione...</option>{services.map((s) => <option key={s.id} value={s.id}>{s.nome} · {s.duracaoMin} min · {money(s.preco)}</option>)}
          </select></div>
        <div className="field"><label>Data</label><input type="date" required value={f.dia} min={hojeISO()} onChange={(e) => setF({ ...f, dia: e.target.value, hora: '' })} /></div>
        <div className="field"><label>Horário</label>
          {manual ? <input type="time" required value={f.hora} onChange={(e) => setF({ ...f, hora: e.target.value })} />
            : <input readOnly value={f.hora || 'Escolha abaixo'} />}
          <small><button type="button" className="btn ghost small" style={{ padding: 0, border: 0 }} onClick={() => setManual(!manual)}>{manual ? 'Ver horários livres' : 'Digitar outro horário'}</button></small>
        </div>
      </div>
      {!manual && f.servicoId && <div className="slot-grid">
        {slots === null ? <Loading text="Calculando horários..." /> : slots.length ? slots.map((s) =>
          <button type="button" key={s.dataHora} className={'slot ' + (f.hora === s.hora ? 'selected' : '')} onClick={() => setF({ ...f, hora: s.hora })}>{s.hora}</button>)
          : <div className="empty" style={{ gridColumn: '1/-1' }}>Nenhum horário livre nesta data.</div>}
      </div>}
      <div className="field" style={{ marginTop: 14 }}><label>Observações</label><textarea rows={2} value={f.observacoes} onChange={(e) => setF({ ...f, observacoes: e.target.value })} /></div>
      <div className="form-actions"><button type="button" className="btn" onClick={onClose}>Voltar</button><button className="btn primary" disabled={saving || !f.hora}>{saving ? 'Salvando...' : 'Criar agendamento'}</button></div>
    </form>
  </Modal>;
}

// ─── Remarcação ────────────────────────────────────────────────────
function Remarcar({ a, onClose, onSaved }: { a: Agendamento; onClose: () => void; onSaved: (a: Agendamento) => void }) {
  const inicial = a.dataHora.slice(0, 10) < hojeISO() ? hojeISO() : a.dataHora.slice(0, 10);
  const [dia, setDia] = useState(inicial);
  const [slots, setSlots] = useState<Slot[] | null>(null);
  const [escolhido, setEscolhido] = useState('');
  const [motivo, setMotivo] = useState(MOTIVOS[0]);
  const [outro, setOutro] = useState('');
  const [hist, setHist] = useState<Historico[]>([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => { api<Historico[]>(`/agendamentos/${a.id}/remarcacoes`).then(setHist).catch(() => {}); }, [a.id]);
  useEffect(() => {
    setSlots(null); setEscolhido('');
    api<Slot[]>(`/relacionamento/horarios-disponiveis?agendamentoId=${a.id}&data=${dia}`)
      .then((s) => setSlots(s.filter((x) => x.dataHora.slice(0, 16) !== a.dataHora.slice(0, 16))))
      .catch((e) => { toastErro(e); setSlots([]); });
  }, [a.id, a.dataHora, dia]);

  async function save(e: React.FormEvent) {
    e.preventDefault();
    if (!escolhido) return;
    setSaving(true);
    try {
      const r = await api<Agendamento>(`/agendamentos/${a.id}/remarcacoes`, {
        method: 'POST', body: JSON.stringify({ novaDataHora: escolhido, motivo: motivo === 'Outro' ? (outro || 'Outro') : motivo }),
      });
      onSaved(r);
    } catch (e) { toastErro(e); setSaving(false); }
  }

  return <Modal title={`Remarcar ${primeiroNome(a.clienteNome)}`} eyebrow="Remarcação" onClose={onClose} wide>
    <form onSubmit={save}>
      <div className="notice"><b>{a.servicoNome}</b> · {a.duracaoMin} min<br />Horário atual: <b>{dataHora(a.dataHora)}</b><br />
        <span className="muted">O horário antigo só é liberado quando você confirmar o novo.</span></div>
      <div className="form-grid">
        <div className="field"><label>Motivo</label><select value={motivo} onChange={(e) => setMotivo(e.target.value)}>{MOTIVOS.map((m) => <option key={m}>{m}</option>)}</select></div>
        <div className="field"><label>Nova data</label><input type="date" value={dia} min={hojeISO()} onChange={(e) => e.target.value && setDia(e.target.value)} /></div>
        {motivo === 'Outro' && <div className="field full"><label>Descreva o motivo</label><input value={outro} onChange={(e) => setOutro(e.target.value)} /></div>}
      </div>
      <div className="field" style={{ marginTop: 14 }}><label>Horários livres em {dataLonga(dia)}</label></div>
      <div className="slot-grid">
        {slots === null ? <Loading text="Calculando horários..." /> : slots.length ? slots.map((s) =>
          <button type="button" key={s.dataHora} className={'slot ' + (escolhido === s.dataHora ? 'selected' : '')} onClick={() => setEscolhido(s.dataHora)}>{s.hora}</button>)
          : <div className="empty" style={{ gridColumn: '1/-1' }}>Nenhum horário livre nesta data.</div>}
      </div>
      {hist.length > 0 && <div className="history"><b>Remarcações anteriores</b>{hist.map((h) => <span key={h.id}>{dataHora(h.anterior)} → {dataHora(h.nova)} · {h.motivo}</span>)}</div>}
      <div className="form-actions"><button type="button" className="btn" onClick={onClose}>Voltar</button>
        <button className="btn primary" disabled={!escolhido || saving}>{saving ? 'Remarcando...' : escolhido ? `Remarcar para ${dataHora(escolhido)}` : 'Escolha um horário'}</button></div>
    </form>
  </Modal>;
}

function AvisarRemarcacao({ a, onClose }: { a: Agendamento; onClose: () => void }) {
  const msg = `Olá, ${primeiroNome(a.clienteNome)}! Seu horário no Studio Elen Gomes foi remarcado para ${dataHora(a.dataHora)} (${a.servicoNome}). Qualquer coisa é só me chamar por aqui. 💛`;
  const link = waLink(a.whatsapp, msg);
  return <Modal title="Avisar a cliente?" eyebrow="Remarcado" onClose={onClose}>
    <p className="muted" style={{ marginTop: 0 }}>Novo horário: <b style={{ color: 'var(--gold2)' }}>{dataHora(a.dataHora)}</b></p>
    <div className="msg-preview">{msg}</div>
    {!link && <div className="notice" style={{ marginTop: 12 }}>O telefone desta cliente está incompleto. Corrija em Clientes para enviar pelo WhatsApp.</div>}
    <div className="form-actions"><button className="btn" onClick={onClose}>Agora não</button>
      {link && <a className="btn primary" href={link} target="_blank" rel="noreferrer" onClick={onClose}>Enviar no WhatsApp</a>}</div>
  </Modal>;
}

// ─── Pagamento ─────────────────────────────────────────────────────
function Pagamento({ a, onClose, onSaved }: { a: Agendamento; onClose: () => void; onSaved: () => void }) {
  const [valor, setValor] = useState(String(a.valor ?? 0));
  const [forma, setForma] = useState('PIX');
  const [saving, setSaving] = useState(false);
  async function save(e: React.FormEvent) {
    e.preventDefault(); setSaving(true);
    try { await api(`/financeiro/pagamentos/${a.id}`, { method: 'POST', body: JSON.stringify({ valor: Number(String(valor).replace(',', '.')), formaPagamento: forma }) }); onSaved(); }
    catch (e) { toastErro(e); setSaving(false); }
  }
  return <Modal title="Registrar pagamento" eyebrow="Recebimento" onClose={onClose}>
    <form onSubmit={save}>
      <div className="notice">{a.clienteNome} · {a.servicoNome}<br /><b>{dataHora(a.dataHora)}</b><br /><span className="muted">Ao registrar, o atendimento fica como concluído.</span></div>
      <div className="form-grid">
        <div className="field"><label>Valor (R$)</label><input inputMode="decimal" required value={valor} onChange={(e) => setValor(e.target.value)} /></div>
        <div className="field"><label>Forma de pagamento</label><select value={forma} onChange={(e) => setForma(e.target.value)}>
          <option value="PIX">Pix</option><option value="DINHEIRO">Dinheiro</option><option value="CARTAO_DEBITO">Débito</option><option value="CARTAO_CREDITO">Crédito</option></select></div>
      </div>
      <div className="form-actions"><button type="button" className="btn" onClick={onClose}>Voltar</button><button className="btn primary" disabled={saving}>{saving ? 'Salvando...' : 'Registrar recebimento'}</button></div>
    </form>
  </Modal>;
}
