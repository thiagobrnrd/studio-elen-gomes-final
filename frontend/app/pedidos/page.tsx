'use client';
import { useCallback, useEffect, useState } from 'react';
import Shell from '../../components/Shell';
import Guard from '../../components/Guard';
import { Modal, Loading, ErrorBox, toast, toastErro } from '../../components/ui';
import { api, Agendamento } from '../../lib/api';
import { dataHora, dataHoraLonga, money, primeiroNome, waLink, STATUS_LABEL } from '../../lib/format';

type Pedido = { agendamento: Agendamento; clienteNova: boolean };
type Lista = { pendentes: Pedido[]; recentes: Pedido[] };

const MOTIVOS = ['Horário indisponível', 'Studio fechado nesse dia', 'Esse serviço precisa de uma avaliação antes', 'Outro motivo'];

function ha(iso?: string | null) {
  if (!iso) return '';
  const min = Math.round((Date.now() - new Date(iso).getTime()) / 60000);
  if (min < 1) return 'agora mesmo';
  if (min < 60) return `há ${min} min`;
  const h = Math.round(min / 60);
  if (h < 24) return `há ${h}h`;
  return `há ${Math.round(h / 24)} dia(s)`;
}

export default function Pedidos() {
  const [lista, setLista] = useState<Lista | null>(null);
  const [err, setErr] = useState('');
  const [ocupado, setOcupado] = useState('');
  const [recusar, setRecusar] = useState<Agendamento | null>(null);
  const [aviso, setAviso] = useState<{ a: Agendamento; tipo: 'aprovado' | 'recusado' } | null>(null);
  const [notif, setNotif] = useState<string>('default');
  const origem = typeof window !== 'undefined' ? window.location.origin : '';
  const linkAgendar = `${origem}/agendar`;

  const load = useCallback(async () => {
    setErr('');
    try { setLista(await api<Lista>('/pedidos')); } catch (e: any) { setErr(e.message); }
  }, []);

  useEffect(() => {
    load();
    try { setNotif('Notification' in window ? Notification.permission : 'unsupported'); } catch { setNotif('unsupported'); }
    const h = () => load();
    window.addEventListener('pedidos-atualizados', h);
    const t = setInterval(load, 30000);
    return () => { window.removeEventListener('pedidos-atualizados', h); clearInterval(t); };
  }, [load]);

  async function aprovar(a: Agendamento) {
    setOcupado(a.id);
    try {
      const r = await api<Agendamento>(`/pedidos/${a.id}/aprovar`, { method: 'POST' });
      toast(`Horário de ${primeiroNome(a.clienteNome)} confirmado.`);
      setAviso({ a: { ...a, ...r }, tipo: 'aprovado' });
      window.dispatchEvent(new Event('pedidos-respondidos'));
      load();
    } catch (e) { toastErro(e); load(); } finally { setOcupado(''); }
  }

  async function ativarNotificacoes() {
    try { const p = await Notification.requestPermission(); setNotif(p); if (p === 'granted') toast('Notificações ativadas neste aparelho.'); }
    catch { toast('Este navegador não permite notificações.', 'erro'); }
  }

  async function copiar() {
    try { await navigator.clipboard.writeText(linkAgendar); toast('Link copiado!'); }
    catch { toast(linkAgendar); }
  }

  return <Guard><Shell>
    <header className="topbar">
      <div><div className="eyebrow">Agendamento online</div><h1>Pedidos</h1>
        <p className="page-subtitle">Horários que as clientes pediram pelo link. O horário fica reservado até você aprovar ou recusar.</p></div>
      <div className="top-actions">
        {notif === 'default' && <button className="btn" onClick={ativarNotificacoes}>🔔 Ativar notificações</button>}
        {notif === 'granted' && <span className="muted" style={{ fontSize: 12 }}>🔔 Notificações ativas</span>}
      </div>
    </header>

    <section className="card share-card">
      <div><div className="eyebrow">Seu link de agendamento</div>
        <div className="share-link">{linkAgendar}</div>
        <p className="muted" style={{ fontSize: 12, margin: '6px 0 0' }}>Coloque na bio do Instagram e na mensagem automática do WhatsApp Business.</p></div>
      <div className="row-actions">
        <button className="btn" onClick={copiar}>Copiar link</button>
        <a className="btn whats" href={`https://wa.me/?text=${encodeURIComponent(`Agende seu horário no Studio Elen Gomes: ${linkAgendar}`)}`} target="_blank" rel="noreferrer">Compartilhar</a>
        <a className="btn" href="/agendar" target="_blank" rel="noreferrer">Ver página ↗</a>
      </div>
    </section>

    {err && <ErrorBox error={err} onRetry={load} />}
    {!lista && !err ? <div className="card"><Loading /></div> : lista && <>
      <div className="section-title" style={{ marginTop: 22 }}><div><h2>Aguardando sua resposta <span className="count-pill">{lista.pendentes.length}</span></h2></div></div>
      {!lista.pendentes.length && <div className="card empty">Nenhum pedido pendente. Quando uma cliente agendar pelo link, ele aparece aqui. ✨</div>}
      <div className="pedido-grid">
        {lista.pendentes.map(({ agendamento: a, clienteNova }) => {
          const passou = new Date(a.dataHora) < new Date();
          return <article className="pedido" key={a.id}>
            <div className="pedido-top">
              <div><b className="pedido-nome">{a.clienteNome}</b>{clienteNova && <span className="tag-nova">Cliente nova</span>}
                <small>{a.telefone} · pedido {ha(a.criadoEm)}</small></div>
            </div>
            <div className="pedido-quando"><span>{dataHoraLonga(a.dataHora)}</span></div>
            <div className="pedido-info">{a.servicoNome} · {a.duracaoMin} min · {money(a.valor)}</div>
            {a.observacoes && <div className="obs">“{a.observacoes}”</div>}
            {passou && <div className="notice" style={{ margin: '10px 0 0', fontSize: 12 }}>⚠ Esse horário já passou. Recuse ou remarque pela Agenda.</div>}
            <div className="row-actions" style={{ marginTop: 12 }}>
              <button className="btn primary" disabled={!!ocupado} onClick={() => aprovar(a)}>{ocupado === a.id ? 'Aprovando…' : '✓ Aprovar'}</button>
              <button className="btn danger" disabled={!!ocupado} onClick={() => setRecusar(a)}>Recusar</button>
              {a.whatsapp && <a className="btn whats" href={waLink(a.whatsapp, `Olá, ${primeiroNome(a.clienteNome)}! Recebi seu pedido de ${a.servicoNome} para ${dataHoraLonga(a.dataHora)}.`)!} target="_blank" rel="noreferrer">WhatsApp</a>}
            </div>
          </article>;
        })}
      </div>

      {lista.recentes.length > 0 && <section className="card" style={{ marginTop: 22 }}>
        <div className="section-title"><div><h2>Atividade recente</h2><p className="muted">Últimos pedidos online respondidos ou cancelados.</p></div></div>
        <div className="table-wrap"><table className="table cards recent-table">
          <thead><tr><th>Cliente</th><th>Serviço</th><th>Horário</th><th>Situação</th></tr></thead>
          <tbody>{lista.recentes.map(({ agendamento: a }) => <tr key={a.id}>
            <td><b>{a.clienteNome}</b></td><td>{a.servicoNome}</td><td>{dataHora(a.dataHora)}</td>
            <td><span className={'badge ' + a.status.toLowerCase()}>{STATUS_LABEL[a.status]}</span>
              {a.motivoCancelamento && <small className={'motivo' + (a.motivoCancelamento === 'Cancelado pela cliente' ? ' alerta' : '')}>{a.motivoCancelamento}</small>}</td>
          </tr>)}</tbody>
        </table></div>
      </section>}
    </>}

    {recusar && <RecusarModal a={recusar} onClose={() => setRecusar(null)} onDone={(r) => {
      setRecusar(null); toast('Pedido recusado. O horário foi liberado.');
      setAviso({ a: { ...recusar, ...r }, tipo: 'recusado' }); window.dispatchEvent(new Event('pedidos-respondidos')); load();
    }} />}
    {aviso && <AvisoModal a={aviso.a} tipo={aviso.tipo} origem={origem} onClose={() => setAviso(null)} />}
  </Shell></Guard>;
}

function RecusarModal({ a, onClose, onDone }: { a: Agendamento; onClose: () => void; onDone: (r: Agendamento) => void }) {
  const [motivo, setMotivo] = useState(MOTIVOS[0]);
  const [outro, setOutro] = useState('');
  const [saving, setSaving] = useState(false);
  async function enviar(e: React.FormEvent) {
    e.preventDefault(); setSaving(true);
    try {
      const r = await api<Agendamento>(`/pedidos/${a.id}/recusar`, { method: 'POST', body: JSON.stringify({ motivo: motivo === 'Outro motivo' ? (outro || 'Horário indisponível') : motivo }) });
      onDone(r);
    } catch (e) { toastErro(e); setSaving(false); }
  }
  return <Modal title={`Recusar pedido de ${primeiroNome(a.clienteNome)}`} eyebrow="Pedido online" onClose={onClose}>
    <form onSubmit={enviar}>
      <div className="notice">{a.servicoNome}<br /><b>{dataHoraLonga(a.dataHora)}</b></div>
      <div className="field"><label>Motivo (a cliente vai ver)</label>
        <select value={motivo} onChange={(e) => setMotivo(e.target.value)}>{MOTIVOS.map((m) => <option key={m}>{m}</option>)}</select></div>
      {motivo === 'Outro motivo' && <div className="field" style={{ marginTop: 12 }}><label>Descreva</label><input value={outro} onChange={(e) => setOutro(e.target.value)} maxLength={200} /></div>}
      <div className="form-actions"><button type="button" className="btn" onClick={onClose}>Voltar</button>
        <button className="btn danger" disabled={saving}>{saving ? 'Recusando…' : 'Recusar e liberar horário'}</button></div>
    </form>
  </Modal>;
}

function AvisoModal({ a, tipo, origem, onClose }: { a: Agendamento; tipo: 'aprovado' | 'recusado'; origem: string; onClose: () => void }) {
  const nome = primeiroNome(a.clienteNome);
  const acompanhar = a.token ? `${origem}/agendar/pedido/${a.token}` : '';
  const msg = tipo === 'aprovado'
    ? `Olá, ${nome}! Seu horário no Studio Elen Gomes está confirmado ✨\n\n📅 ${dataHoraLonga(a.dataHora)}\n💇‍♀️ ${a.servicoNome}\n\n${acompanhar ? `Detalhes e cancelamento: ${acompanhar}\n\n` : ''}Te espero! 💛`
    : `Olá, ${nome}! Infelizmente não consegui confirmar seu horário de ${a.servicoNome} em ${dataHoraLonga(a.dataHora)}${a.motivoCancelamento ? ` (${a.motivoCancelamento.toLowerCase()})` : ''}.\n\nVocê pode escolher outro horário por aqui: ${origem}/agendar\n\nQualquer dúvida, é só me chamar 💛`;
  const link = waLink(a.whatsapp, msg);
  return <Modal title="Avisar a cliente?" eyebrow={tipo === 'aprovado' ? 'Pedido aprovado' : 'Pedido recusado'} onClose={onClose}>
    <div className="msg-preview">{msg}</div>
    {!link && <div className="notice" style={{ marginTop: 12 }}>O telefone desta cliente está incompleto. Corrija em Clientes.</div>}
    <p className="muted" style={{ fontSize: 12, marginBottom: 0 }}>A cliente também vê a atualização pelo link de acompanhamento dela.</p>
    <div className="form-actions"><button className="btn" onClick={onClose}>Agora não</button>
      {link && <a className="btn primary" href={link} target="_blank" rel="noreferrer" onClick={onClose}>Enviar no WhatsApp</a>}</div>
  </Modal>;
}
