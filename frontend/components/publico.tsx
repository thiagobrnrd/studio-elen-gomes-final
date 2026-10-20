'use client';
import { resumoDias, waLink } from '../lib/format';

export type PedidoSalvo = { token: string; servico: string; dataHora: string };
const CHAVE = 'studio_pedidos';
export function lerPedidos(): PedidoSalvo[] {
  try { return JSON.parse(localStorage.getItem(CHAVE) || '[]'); } catch { return []; }
}
export function esquecerDados() {
  try { ['studio_pedidos', 'studio_cliente_nome', 'studio_cliente_tel'].forEach((k) => localStorage.removeItem(k)); } catch {}
}
export function salvarPedido(p: PedidoSalvo) {
  try { localStorage.setItem(CHAVE, JSON.stringify([p, ...lerPedidos()].slice(0, 10))); } catch {}
}

type InfoTopo = { nome?: string; endereco?: string | null; instagram?: string; whatsapp?: string | null; abertura?: string; fechamento?: string; diasFuncionamento?: number[]; mensagem?: string };

export function Pagina({ info, children, titulo = 'Agende seu horário', semMensagem = false }: { info: InfoTopo | null; children: React.ReactNode; titulo?: string; semMensagem?: boolean }) {
  const i: any = info || {};
  const insta = (i.instagram || '').replace(/^@/, '').replace(/^https?:\/\/(www\.)?instagram\.com\//, '').replace(/\/$/, '');
  return <div className="pb">
    <header className="pb-hero">
      <div className="pb-hero-art" aria-hidden="true" />
      <div className="pb-hero-in">
        <img src="/images/logo-elen-gomes.png" alt="Elen Gomes Studio" className="pb-logo" />
        <h1>{titulo}</h1>
        {i.mensagem && !semMensagem && <p>{i.mensagem}</p>}
        <div className="pb-chips">
          {i.diasFuncionamento?.length > 0 && <span>🕘 {resumoDias(i.diasFuncionamento)} · {i.abertura}–{i.fechamento}</span>}
          {i.endereco && <a href={`https://maps.google.com/?q=${encodeURIComponent(i.endereco)}`} target="_blank" rel="noreferrer">📍 {i.endereco}</a>}
          {insta && <a href={`https://instagram.com/${insta}`} target="_blank" rel="noreferrer">◎ @{insta}</a>}
          {i.whatsapp && <a href={waLink(i.whatsapp, 'Olá! Tenho uma dúvida sobre agendamento.')!} target="_blank" rel="noreferrer">💬 WhatsApp</a>}
        </div>
      </div>
    </header>
    <main className="pb-main">{children}</main>
    <footer className="pb-foot">Studio Elen Gomes · Beleza · Autoestima · Você<br /><a href="/agendar/privacidade">Política de Privacidade</a></footer>
  </div>;
}
