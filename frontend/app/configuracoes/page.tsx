'use client';
import { useEffect, useState } from 'react';
import Shell from '../../components/Shell';
import Guard from '../../components/Guard';
import { Loading, ErrorBox, toast, toastErro } from '../../components/ui';
import { api } from '../../lib/api';

type Cfg = Record<string, string>;
const DIAS: [number, string][] = [[1, 'Seg'], [2, 'Ter'], [3, 'Qua'], [4, 'Qui'], [5, 'Sex'], [6, 'Sáb'], [7, 'Dom']];

function Switch({ checked, onChange, label, desc }: { checked: boolean; onChange: (v: boolean) => void; label: string; desc?: string }) {
  return <label className="switch-row">
    <span><b>{label}</b>{desc && <small>{desc}</small>}</span>
    <input type="checkbox" className="switch" checked={checked} onChange={(e) => onChange(e.target.checked)} />
  </label>;
}

export default function Configuracoes() {
  const [c, setC] = useState<Cfg | null>(null);
  const [err, setErr] = useState('');
  const [saving, setSaving] = useState(false);
  const [mudou, setMudou] = useState(false);

  async function load() {
    setErr('');
    try { setC(await api<Cfg>('/configuracoes')); setMudou(false); } catch (e: any) { setErr(e.message); }
  }
  useEffect(() => { load(); }, []);

  const set = (k: string, v: string) => { setC((x) => ({ ...(x || {}), [k]: v })); setMudou(true); };
  const dias = new Set((c?.dias_funcionamento || '').split(',').filter(Boolean).map(Number));
  const toggleDia = (d: number) => { const n = new Set(dias); n.has(d) ? n.delete(d) : n.add(d); set('dias_funcionamento', [...n].sort().join(',')); };

  async function salvar(e: React.FormEvent) {
    e.preventDefault(); if (!c) return;
    setSaving(true);
    try { setC(await api<Cfg>('/configuracoes', { method: 'PUT', body: JSON.stringify(c) })); setMudou(false); toast('Configurações salvas.'); }
    catch (e) { toastErro(e); } finally { setSaving(false); }
  }

  return <Guard><Shell>
    <header className="topbar">
      <div><div className="eyebrow">Studio</div><h1>Configurações</h1><p className="page-subtitle">Funcionamento do studio e regras do agendamento online.</p></div>
    </header>
    {err && <ErrorBox error={err} onRetry={load} />}
    {!c && !err ? <div className="card"><Loading /></div> : c && <form onSubmit={salvar} className="config">
      <section className="card">
        <div className="section-title"><div><h2>Agendamento online</h2><p className="muted">A página onde as clientes escolhem o horário sozinhas.</p></div>
          <a className="btn" href="/agendar" target="_blank" rel="noreferrer">Ver página ↗</a></div>
        <Switch checked={c.online_ativo === 'true'} onChange={(v) => set('online_ativo', String(v))} label="Aceitar agendamentos pelo link"
          desc="Desligado, a página mostra apenas um botão para falar no WhatsApp (bom para férias)." />
        <Switch checked={c.aprovacao_automatica === 'true'} onChange={(v) => set('aprovacao_automatica', String(v))} label="Confirmar pedidos automaticamente"
          desc="Ligado, o horário já entra confirmado na agenda. Desligado, você aprova cada pedido na aba Pedidos." />
        <div className="form-grid" style={{ marginTop: 16 }}>
          <div className="field"><label>Antecedência mínima (horas)</label><input type="number" min={0} max={168} value={c.antecedencia_min_horas} onChange={(e) => set('antecedencia_min_horas', e.target.value)} />
            <small>Ex.: 2 = a cliente não pode marcar para daqui a menos de 2 horas.</small></div>
          <div className="field"><label>Aceitar pedidos até quantos dias à frente</label><input type="number" min={1} max={180} value={c.dias_maximos} onChange={(e) => set('dias_maximos', e.target.value)} /></div>
        </div>
      </section>

      <section className="card">
        <div className="section-title"><div><h2>Funcionamento</h2><p className="muted">Usado para calcular os horários livres (no painel e no link).</p></div></div>
        <div className="field"><label>Dias em que o studio atende</label>
          <div className="dias">{DIAS.map(([n, l]) => <button type="button" key={n} className={'dia' + (dias.has(n) ? ' on' : '')} onClick={() => toggleDia(n)}>{l}</button>)}</div></div>
        <div className="form-grid" style={{ marginTop: 14 }}>
          <div className="field"><label>Abre às</label><input type="time" value={c.abertura} onChange={(e) => set('abertura', e.target.value)} /></div>
          <div className="field"><label>Fecha às</label><input type="time" value={c.fechamento} onChange={(e) => set('fechamento', e.target.value)} /></div>
          <div className="field"><label>Intervalo de almoço — início <span className="muted">(opcional)</span></label><input type="time" value={c.almoco_inicio} onChange={(e) => set('almoco_inicio', e.target.value)} /></div>
          <div className="field"><label>Intervalo de almoço — fim</label><input type="time" value={c.almoco_fim} onChange={(e) => set('almoco_fim', e.target.value)} /></div>
          <div className="field"><label>Horários oferecidos a cada</label>
            <select value={c.intervalo_minutos} onChange={(e) => set('intervalo_minutos', e.target.value)}>
              {['15', '20', '30', '45', '60'].map((m) => <option key={m} value={m}>{m} minutos</option>)}</select></div>
        </div>
      </section>

      <section className="card">
        <div className="section-title"><div><h2>Informações na página</h2><p className="muted">Aparecem no topo da página de agendamento.</p></div></div>
        <div className="form-grid">
          <div className="field full"><label>Mensagem de boas-vindas</label><input value={c.mensagem} onChange={(e) => set('mensagem', e.target.value)} maxLength={200} /></div>
          <div className="field full"><label>Endereço</label><input value={c.endereco} onChange={(e) => set('endereco', e.target.value)} placeholder="Rua, número — bairro, cidade" /></div>
          <div className="field"><label>Instagram</label><input value={c.instagram} onChange={(e) => set('instagram', e.target.value)} placeholder="@studioelengomes" /></div>
          <div className="field"><label>WhatsApp do studio</label><input value={c.whatsapp} onChange={(e) => set('whatsapp', e.target.value)} placeholder="(11) 98338-4655" /></div>
          <div className="field full"><label>Política de cancelamento</label><textarea rows={2} value={c.politica} onChange={(e) => set('politica', e.target.value)} maxLength={400} />
            <small>A cliente precisa marcar que leu antes de enviar o pedido.</small></div>
        </div>
      </section>

      <div className="save-bar"><span className="muted">{mudou ? 'Há alterações não salvas.' : 'Tudo salvo.'}</span>
        <button className="btn primary" disabled={saving || !mudou}>{saving ? 'Salvando…' : 'Salvar configurações'}</button></div>
    </form>}
  </Shell></Guard>;
}
