'use client';
import { useCallback, useEffect, useState } from 'react';
import Shell from '../../components/Shell';
import Guard from '../../components/Guard';
import { Modal, Loading, ErrorBox, toast, toastErro } from '../../components/ui';
import { api, Servico } from '../../lib/api';
import { money } from '../../lib/format';

const vazio: Servico = { nome: '', descricao: '', duracaoMin: 60, preco: 0, ativo: true, requerRetorno: false, diasParaRetorno: null, online: true, precoAPartir: true };

export default function Servicos() {
  const [list, setList] = useState<Servico[] | null>(null);
  const [err, setErr] = useState('');
  const [form, setForm] = useState<Servico | null>(null);

  const load = useCallback(async () => {
    setErr('');
    try { setList(await api<Servico[]>('/servicos')); } catch (e: any) { setErr(e.message); setList([]); }
  }, []);
  useEffect(() => { load(); }, [load]);

  async function alternar(s: Servico) {
    try { await api(`/servicos/${s.id}/ativo`, { method: 'PATCH' }); toast(s.ativo ? 'Serviço desativado.' : 'Serviço ativado.'); load(); }
    catch (e) { toastErro(e); }
  }

  return <Guard><Shell>
    <header className="topbar">
      <div style={{ minWidth: 0 }}><div className="eyebrow" style={{ whiteSpace: 'nowrap' }}>Catálogo</div><h1>Serviços</h1><p className="page-subtitle">Preço, duração e retorno de cada procedimento.</p></div>
      <button className="btn primary" onClick={() => setForm({ ...vazio })}>+ Novo serviço</button>
    </header>
    {err && <ErrorBox error={err} onRetry={load} />}
    {list === null ? <div className="card"><Loading /></div> : <>
      <div className="grid service-grid">{list.map((s) => <div className={'card service-card' + (s.ativo ? '' : ' off')} key={s.id}>
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10 }}>
          <div style={{ minWidth: 0 }}><div className="eyebrow" style={{ whiteSpace: 'nowrap' }}>{s.ativo ? 'Ativo' : 'Inativo'}{s.ativo && s.online !== false && <span className="tag-online" style={{ marginLeft: 8 }}>online</span>}</div><h3>{s.nome}</h3></div>
          <span className="price">{s.precoAPartir !== false && <small className="apartir">a partir de </small>}{money(s.preco)}</span>
        </div>
        <span className="muted" style={{ fontSize: 13 }}>{s.descricao || 'Sem descrição.'}</span>
        <span className="muted" style={{ fontSize: 12 }}>⏱ {s.duracaoMin} min{s.requerRetorno && s.diasParaRetorno ? ` · retorno em ${s.diasParaRetorno} dias` : ''}</span>
        <div className="row-actions">
          <button className="btn small" onClick={() => setForm({ ...s })}>Editar</button>
          <button className="btn small" onClick={() => alternar(s)}>{s.ativo ? 'Desativar' : 'Ativar'}</button>
        </div>
      </div>)}</div>
      {!list.length && !err && <div className="card empty">Nenhum serviço cadastrado ainda. Comece pelo botão “Novo serviço”.</div>}
    </>}
    {form && <FormServico inicial={form} onClose={() => setForm(null)} onSaved={() => { setForm(null); toast('Serviço salvo.'); load(); }} />}
  </Shell></Guard>;
}

function FormServico({ inicial, onClose, onSaved }: { inicial: Servico; onClose: () => void; onSaved: () => void }) {
  const [f, setF] = useState({ ...inicial, preco: String(inicial.preco ?? ''), duracaoMin: String(inicial.duracaoMin ?? ''), diasParaRetorno: String(inicial.diasParaRetorno ?? '') });
  const [saving, setSaving] = useState(false);
  async function save(e: React.FormEvent) {
    e.preventDefault(); setSaving(true);
    try {
      const body = {
        nome: f.nome, descricao: f.descricao || null, ativo: f.ativo,
        preco: Number(String(f.preco).replace(',', '.')), duracaoMin: Number(f.duracaoMin),
        online: f.online !== false, precoAPartir: f.precoAPartir !== false,
        requerRetorno: f.requerRetorno, diasParaRetorno: f.requerRetorno ? Number(f.diasParaRetorno) : null,
      };
      await api('/servicos' + (f.id ? `/${f.id}` : ''), { method: f.id ? 'PUT' : 'POST', body: JSON.stringify(body) });
      onSaved();
    } catch (e) { toastErro(e); setSaving(false); }
  }
  return <Modal title={f.id ? 'Editar serviço' : 'Novo serviço'} eyebrow="Serviços" onClose={onClose}>
    <form onSubmit={save}>
      <div className="form-grid">
        <div className="field full"><label>Nome</label><input required autoFocus value={f.nome} onChange={(e) => setF({ ...f, nome: e.target.value })} /></div>
        <div className="field"><label>Duração (min)</label><input type="number" required min={5} step={5} value={f.duracaoMin} onChange={(e) => setF({ ...f, duracaoMin: e.target.value })} /></div>
        <div className="field"><label>Preço (R$)</label><input inputMode="decimal" required value={f.preco} onChange={(e) => setF({ ...f, preco: e.target.value })} /></div>
        <div className="field full"><label>Descrição</label><textarea rows={2} value={f.descricao || ''} onChange={(e) => setF({ ...f, descricao: e.target.value })} /></div>
        <div className="field full"><label className="check"><input type="checkbox" checked={f.precoAPartir !== false} onChange={(e) => setF({ ...f, precoAPartir: e.target.checked })} /> Mostrar “a partir de” antes do preço (valor varia conforme o cabelo)</label></div>
        <div className="field full"><label className="check"><input type="checkbox" checked={f.online !== false} onChange={(e) => setF({ ...f, online: e.target.checked })} /> Aparece no agendamento online (link das clientes)</label></div>
        <div className="field full"><label className="check"><input type="checkbox" checked={f.requerRetorno} onChange={(e) => setF({ ...f, requerRetorno: e.target.checked })} /> Lembrar a cliente de voltar (retorno)</label></div>
        {f.requerRetorno && <div className="field"><label>Retorno após quantos dias?</label><input type="number" required min={1} value={f.diasParaRetorno} onChange={(e) => setF({ ...f, diasParaRetorno: e.target.value })} placeholder="ex.: 30" /></div>}
      </div>
      <div className="form-actions"><button type="button" className="btn" onClick={onClose}>Voltar</button><button className="btn primary" disabled={saving}>{saving ? 'Salvando...' : 'Salvar serviço'}</button></div>
    </form>
  </Modal>;
}
