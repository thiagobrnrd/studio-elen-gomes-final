'use client';
import { useCallback, useEffect, useState } from 'react';
import Shell from '../../components/Shell';
import Guard from '../../components/Guard';
import { Modal, Loading, ErrorBox, toast, toastErro } from '../../components/ui';
import { api, Agendamento, Cliente } from '../../lib/api';
import { dataHora, mascaraTelefone, money, STATUS_LABEL } from '../../lib/format';

const vazio: Cliente = { nome: '', telefone: '', email: '', dataNascimento: '' };

export default function Clientes() {
  const [list, setList] = useState<Cliente[] | null>(null);
  const [q, setQ] = useState('');
  const [err, setErr] = useState('');
  const [form, setForm] = useState<Cliente | null>(null);
  const [hist, setHist] = useState<Cliente | null>(null);

  const load = useCallback(async (busca = '') => {
    setErr('');
    try { setList(await api<Cliente[]>(`/clientes${busca ? `?q=${encodeURIComponent(busca)}` : ''}`)); }
    catch (e: any) { setErr(e.message); setList([]); }
  }, []);
  useEffect(() => { load(); }, [load]);
  useEffect(() => { const t = setTimeout(() => load(q.trim()), 300); return () => clearTimeout(t); }, [q, load]);

  async function excluir(c: Cliente) {
    if (!confirm(`Excluir ${c.nome}?`)) return;
    try { await api(`/clientes/${c.id}`, { method: 'DELETE' }); toast('Cliente excluída.'); load(q); }
    catch (e) { toastErro(e); }
  }

  return <Guard><Shell>
    <header className="topbar">
      <div><div className="eyebrow">Relacionamento</div><h1>Clientes</h1><p className="page-subtitle">Cadastro, contato e histórico de cada cliente.</p></div>
      <button className="btn primary" onClick={() => setForm({ ...vazio })}>+ Nova cliente</button>
    </header>
    {err && <ErrorBox error={err} onRetry={() => load(q)} />}
    <div className="card">
      <div className="toolbar"><div className="toolbar-left"><input className="input search" placeholder="Buscar por nome..." value={q} onChange={(e) => setQ(e.target.value)} /></div>
        <div className="toolbar-right muted">{list?.length ?? 0} cliente(s)</div></div>
      <div className="table-wrap">
        {list === null ? <Loading /> : <>
          <table className="table cards clients-table">
            <thead><tr><th>Cliente</th><th>WhatsApp</th><th>E-mail</th><th>Aniversário</th><th>Atendimentos</th><th></th></tr></thead>
            <tbody>{list.map((c) => <tr key={c.id}>
              <td><b>{c.nome}</b></td>
              <td>{c.whatsapp ? <a href={`https://wa.me/${c.whatsapp}`} target="_blank" rel="noreferrer">{c.telefone}</a> : <span title="Telefone incompleto — o WhatsApp não vai funcionar">{c.telefone} ⚠</span>}</td>
              <td>{c.email || '—'}</td>
              <td>{c.dataNascimento ? c.dataNascimento.split('-').reverse().slice(0, 2).join('/') : '—'}</td>
              <td>{c.totalAtendimentos || 0}</td>
              <td><div className="row-actions">
                <button className="btn small" onClick={() => setHist(c)}>Histórico</button>
                <button className="btn small" onClick={() => setForm({ ...c, email: c.email || '', dataNascimento: c.dataNascimento || '' })}>Editar</button>
                {!c.totalAtendimentos && <button className="btn small danger" onClick={() => excluir(c)}>Excluir</button>}
              </div></td>
            </tr>)}</tbody>
          </table>
          {!list.length && !err && <div className="empty">{q ? 'Nenhuma cliente encontrada.' : 'Nenhuma cliente cadastrada ainda.'}</div>}
        </>}
      </div>
    </div>
    {form && <FormCliente inicial={form} onClose={() => setForm(null)} onSaved={() => { setForm(null); toast('Cliente salva.'); load(q); }} />}
    {hist && <HistoricoCliente c={hist} onClose={() => setHist(null)} />}
  </Shell></Guard>;
}

function FormCliente({ inicial, onClose, onSaved }: { inicial: Cliente; onClose: () => void; onSaved: () => void }) {
  const [f, setF] = useState<Cliente>(inicial);
  const [saving, setSaving] = useState(false);
  async function save(e: React.FormEvent) {
    e.preventDefault();
    if (f.telefone.replace(/\D/g, '').length < 10) { toast('Informe o WhatsApp com DDD.', 'erro'); return; }
    setSaving(true);
    try {
      const body = { nome: f.nome, telefone: f.telefone, email: f.email || null, dataNascimento: f.dataNascimento || null };
      await api('/clientes' + (f.id ? `/${f.id}` : ''), { method: f.id ? 'PUT' : 'POST', body: JSON.stringify(body) });
      onSaved();
    } catch (e) { toastErro(e); setSaving(false); }
  }
  return <Modal title={f.id ? 'Editar cliente' : 'Nova cliente'} eyebrow="Clientes" onClose={onClose}>
    <form onSubmit={save}>
      <div className="form-grid">
        <div className="field full"><label>Nome</label><input required autoFocus value={f.nome} onChange={(e) => setF({ ...f, nome: e.target.value })} /></div>
        <div className="field"><label>WhatsApp (com DDD)</label><input required inputMode="tel" value={f.telefone} onChange={(e) => setF({ ...f, telefone: mascaraTelefone(e.target.value) })} placeholder="(11) 99999-9999" /></div>
        <div className="field"><label>Data de nascimento</label><input type="date" value={f.dataNascimento || ''} onChange={(e) => setF({ ...f, dataNascimento: e.target.value })} /><small>Usada nos lembretes de aniversário.</small></div>
        <div className="field full"><label>E-mail (opcional)</label><input type="email" value={f.email || ''} onChange={(e) => setF({ ...f, email: e.target.value })} /></div>
      </div>
      <div className="form-actions"><button type="button" className="btn" onClick={onClose}>Voltar</button><button className="btn primary" disabled={saving}>{saving ? 'Salvando...' : 'Salvar cliente'}</button></div>
    </form>
  </Modal>;
}

function HistoricoCliente({ c, onClose }: { c: Cliente; onClose: () => void }) {
  const [list, setList] = useState<Agendamento[] | null>(null);
  useEffect(() => { api<Agendamento[]>(`/agendamentos?clienteId=${c.id}`).then(setList).catch((e) => { toastErro(e); setList([]); }); }, [c.id]);
  return <Modal title={c.nome} eyebrow="Histórico" onClose={onClose} wide>
    {list === null ? <Loading /> : list.length ? <div className="table-wrap"><table className="table">
      <thead><tr><th>Data</th><th>Serviço</th><th>Valor</th><th>Status</th></tr></thead>
      <tbody>{list.map((a) => <tr key={a.id}><td>{dataHora(a.dataHora)}</td><td>{a.servicoNome}</td><td>{money(a.valor)}</td>
        <td><span className={'badge ' + a.status.toLowerCase()}>{STATUS_LABEL[a.status]}</span>{a.pago && <> <span className="badge pago">Pago</span></>}</td></tr>)}</tbody>
    </table></div> : <div className="empty">Nenhum atendimento ainda.</div>}
  </Modal>;
}
