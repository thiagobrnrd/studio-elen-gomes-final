'use client';
import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { publicApi, PedidoPublico } from '../../../../lib/api';
import { baixarIcs, dataHoraLonga, precoCliente, waLink } from '../../../../lib/format';
import { Pagina } from '../../../../components/publico';

const ETAPAS: Record<string, { titulo: string; texto: (p: PedidoPublico) => string; cor: string }> = {
  SOLICITADO: { titulo: 'Aguardando confirmação', cor: 'wait', texto: (p) => `Seu horário está reservado, ${p.clienteNome}. O studio vai confirmar pelo WhatsApp em breve.` },
  AGENDADO: { titulo: 'Horário agendado', cor: 'ok', texto: (p) => `Seu horário está marcado, ${p.clienteNome}.` },
  CONFIRMADO: { titulo: 'Horário confirmado', cor: 'ok', texto: (p) => `Tudo certo, ${p.clienteNome}! Esperamos você.` },
  CONCLUIDO: { titulo: 'Atendimento realizado', cor: 'ok', texto: () => 'Obrigada pela visita! Esperamos você novamente.' },
  CANCELADO: { titulo: 'Horário cancelado', cor: 'off', texto: (p) => p.motivo === 'Cancelado pela cliente' ? 'Você cancelou este horário.' : 'Infelizmente este horário não pôde ser confirmado.' },
};

export default function PedidoPage() {
  const { token } = useParams<{ token: string }>();
  const [p, setP] = useState<PedidoPublico | null>(null);
  const [erro, setErro] = useState('');
  const [cancelando, setCancelando] = useState(false);
  const [confirmarCancel, setConfirmarCancel] = useState(false);
  const [info, setInfo] = useState<any>(null);
  useEffect(() => { publicApi<any>('/info').then(setInfo).catch(() => {}); }, []);

  const carregar = useCallback(() => {
    publicApi<PedidoPublico>(`/pedidos/${token}`).then((x) => { setP(x); setErro(''); }).catch((e) => setErro(e.message));
  }, [token]);

  useEffect(() => { carregar(); }, [carregar]);
  useEffect(() => {
    if (p?.status !== 'SOLICITADO') return;
    const t = setInterval(carregar, 30000);
    return () => clearInterval(t);
  }, [p?.status, carregar]);

  async function cancelar() {
    setCancelando(true);
    try { setP(await publicApi<PedidoPublico>(`/pedidos/${token}/cancelar`, { method: 'POST' })); setConfirmarCancel(false); }
    catch (e: any) { setErro(e.message); }
    finally { setCancelando(false); }
  }

  const etapa = p ? ETAPAS[p.status] : null;
  const ativo = p && p.status !== 'CANCELADO' && p.status !== 'CONCLUIDO';

  return <Pagina info={info}>
    {!p && !erro && <div className="pb-loading"><span className="spinner" /> Carregando seu pedido…</div>}
    {erro && !p && <div className="pb-card pb-center"><h2 className="pb-h2">Pedido não encontrado</h2><p className="pb-muted">{erro}</p><Link className="pb-btn" href="/agendar">Fazer um agendamento</Link></div>}
    {p && etapa && <div className="pb-card">
      <div className={'pb-status ' + etapa.cor}><span className="dot" />{etapa.titulo}</div>
      <p className="pb-lead">{etapa.texto(p)}</p>
      {p.status === 'CANCELADO' && p.motivo && p.motivo !== 'Cancelado pela cliente' && <p className="pb-muted">Motivo: {p.motivo}</p>}

      {p.status !== 'CANCELADO' && <ol className="pb-timeline">
        <li className="done"><b>Pedido enviado</b></li>
        <li className={p.status === 'SOLICITADO' ? 'now' : 'done'}><b>{p.status === 'SOLICITADO' ? 'Aguardando o studio' : 'Confirmado pelo studio'}</b></li>
        <li className={p.status === 'CONCLUIDO' ? 'done' : ''}><b>Atendimento</b></li>
      </ol>}

      <div className="pb-summary">
        <div><small>Serviço</small><b>{p.servicoNome}</b></div>
        <div><small>Quando</small><b>{dataHoraLonga(p.dataHora)}</b></div>
        <div><small>Valor</small><b>{precoCliente(p.valor, p.precoAPartir)}</b></div>
        {p.endereco && <div><small>Endereço</small><b><a href={`https://maps.google.com/?q=${encodeURIComponent(p.endereco)}`} target="_blank" rel="noreferrer">{p.endereco}</a></b></div>}
        {p.observacoes && <div><small>Sua observação</small><b>{p.observacoes}</b></div>}
      </div>

      <div className="pb-actions">
        {ativo && <button className="pb-btn pb-full" onClick={() => baixarIcs(`${p.servicoNome} — ${p.studioNome}`, p.dataHora, p.duracaoMin, p.endereco, typeof window !== 'undefined' ? window.location.href : '')}>📅 Adicionar ao calendário</button>}
        {p.studioWhatsapp && <a className="pb-btn ghost pb-full" href={waLink(p.studioWhatsapp, `Olá! Sobre meu horário de ${p.servicoNome} em ${dataHoraLonga(p.dataHora)} (${p.clienteNome}).`)!} target="_blank" rel="noreferrer">💬 Falar com o studio</a>}
        {(p.status === 'CANCELADO' || p.status === 'CONCLUIDO') && <Link className="pb-btn pb-full" href="/agendar">Agendar novo horário</Link>}
        {p.podeCancelar && !confirmarCancel && <button className="pb-link pb-danger" onClick={() => setConfirmarCancel(true)}>Cancelar este horário</button>}
      </div>

      {confirmarCancel && <div className="pb-confirm">
        <b>Cancelar o horário?</b>
        {p.politica && <p className="pb-muted pb-tiny">{p.politica}</p>}
        <div className="pb-row">
          <button className="pb-btn ghost" onClick={() => setConfirmarCancel(false)}>Manter horário</button>
          <button className="pb-btn danger" disabled={cancelando} onClick={cancelar}>{cancelando ? 'Cancelando…' : 'Sim, cancelar'}</button>
        </div>
      </div>}
      {erro && <div className="pb-alert">{erro}</div>}
    </div>}
    {p && <p className="pb-center pb-tiny pb-muted" style={{ marginTop: 18 }}><Link href="/agendar">← Voltar para o agendamento</Link></p>}
  </Pagina>;
}
