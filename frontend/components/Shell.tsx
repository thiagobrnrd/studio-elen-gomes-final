'use client';
import Image from 'next/image';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useRef, useState } from 'react';
import { api, clearToken } from '../lib/api';
import { Toaster, toast } from './ui';

type Item = [href: string, icon: string, label: string];
const nav: Item[] = [
  ['/dashboard', '⌂', 'Visão geral'],
  ['/agenda', '◷', 'Agenda'],
  ['/pedidos', '✉', 'Pedidos online'],
  ['/clientes', '♙', 'Clientes'],
  ['/servicos', '✦', 'Serviços'],
  ['/financeiro', 'R$', 'Financeiro'],
  ['/lembretes', '◌', 'Lembretes'],
  ['/configuracoes', '⚙', 'Configurações'],
];
const mobileNav: Item[] = [
  ['/dashboard', '⌂', 'Início'],
  ['/agenda', '◷', 'Agenda'],
  ['/pedidos', '✉', 'Pedidos'],
  ['/lembretes', '◌', 'Lembretes'],
];
const mais: Item[] = [
  ['/clientes', '♙', 'Clientes'],
  ['/servicos', '✦', 'Serviços'],
  ['/financeiro', 'R$', 'Financeiro'],
  ['/configuracoes', '⚙', 'Configurações'],
];

function plim() {
  try {
    const Ctx = (window as any).AudioContext || (window as any).webkitAudioContext;
    const ctx = new Ctx();
    [880, 1320].forEach((f, i) => {
      const o = ctx.createOscillator(); const g = ctx.createGain();
      o.type = 'sine'; o.frequency.value = f;
      g.gain.setValueAtTime(0.0001, ctx.currentTime + i * 0.16);
      g.gain.exponentialRampToValueAtTime(0.25, ctx.currentTime + i * 0.16 + 0.02);
      g.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + i * 0.16 + 0.35);
      o.connect(g).connect(ctx.destination); o.start(ctx.currentTime + i * 0.16); o.stop(ctx.currentTime + i * 0.16 + 0.4);
    });
  } catch {}
}

export default function Shell({ children }: { children: React.ReactNode }) {
  const p = usePathname();
  const r = useRouter();
  const [pendentes, setPendentes] = useState(0);
  const [sheet, setSheet] = useState(false);
  const ultimo = useRef<string | null | undefined>(undefined);

  useEffect(() => {
    let vivo = true;
    async function tick() {
      if (document.visibilityState === 'hidden') return;
      try {
        const c = await api<{ pendentes: number; ultimo: string | null }>('/pedidos/contagem');
        if (!vivo) return;
        setPendentes(c.pendentes);
        if (ultimo.current !== undefined && c.ultimo && c.ultimo !== ultimo.current) {
          toast('🔔 Novo pedido de agendamento online!');
          plim();
          try {
            if ('Notification' in window && Notification.permission === 'granted') {
              new Notification('Novo pedido de agendamento', { body: 'Abra o painel para aprovar ou recusar.', icon: '/images/logo-elen-gomes.png' });
            }
          } catch {}
          window.dispatchEvent(new Event('pedidos-atualizados'));
        }
        ultimo.current = c.ultimo;
      } catch {}
    }
    tick();
    const t = setInterval(tick, 30000);
    const vis = () => { if (document.visibilityState === 'visible') tick(); };
    document.addEventListener('visibilitychange', vis);
    window.addEventListener('pedidos-respondidos', tick);
    return () => { vivo = false; clearInterval(t); document.removeEventListener('visibilitychange', vis); window.removeEventListener('pedidos-respondidos', tick); };
  }, []);

  useEffect(() => {
    const base = 'Studio Elen Gomes';
    document.title = pendentes > 0 ? `(${pendentes}) ${base}` : base;
  }, [pendentes]);

  useEffect(() => { setSheet(false); }, [p]);

  const sair = () => { clearToken(); r.replace('/login'); };
  const badge = (href: string) => href === '/pedidos' && pendentes > 0 ? <em className="nav-badge">{pendentes}</em> : null;

  return <div className="app">
    <aside className="sidebar">
      <Link href="/dashboard" className="sidebar-brand" aria-label="Studio Elen Gomes">
        <Image src="/images/logo-elen-gomes.png" alt="Elen Gomes Studio" width={170} height={102} priority className="sidebar-logo" />
      </Link>
      <div className="sidebar-divider" />
      <div className="sidebar-label">GESTÃO DO STUDIO</div>
      <nav className="nav">
        {nav.map(([href, icon, label]) => <Link className={p.startsWith(href) ? 'active' : ''} href={href} key={href}>
          <span className="nav-icon">{icon}</span><span>{label}</span>{badge(href)}
        </Link>)}
      </nav>
      <a className="sidebar-public" href="/agendar" target="_blank" rel="noreferrer">↗ Página de agendamento</a>
      <div className="sidebar-bottom">
        <div className="sidebar-mini-brand"><span>EG</span><div><b>Studio Elen Gomes</b><small>Gestão do salão</small></div></div>
        <button className="btn ghost logout" onClick={sair}>Sair <span>→</span></button>
      </div>
    </aside>
    <main className="main">
      <div className="mobile-header">
        <Image src="/images/logo-elen-gomes.png" alt="Elen Gomes Studio" width={116} height={70} priority />
        {pendentes > 0 && <Link href="/pedidos" className="btn small primary">✉ {pendentes} pedido{pendentes > 1 ? 's' : ''}</Link>}
      </div>
      {children}
    </main>
    <Toaster />
    <nav className="mobile-nav">
      {mobileNav.map(([href, icon, label]) => <Link href={href} key={href} className={p.startsWith(href) ? 'active' : ''}>
        <div>{icon}{badge(href)}</div><span>{label}</span>
      </Link>)}
      <button className={'mobile-more' + (mais.some(([h]) => p.startsWith(h)) ? ' active' : '')} onClick={() => setSheet(!sheet)}><div>☰</div><span>Mais</span></button>
    </nav>
    {sheet && <div className="sheet-overlay" onClick={() => setSheet(false)}>
      <div className="sheet" onClick={(e) => e.stopPropagation()}>
        {mais.map(([href, icon, label]) => <Link key={href} href={href} className={p.startsWith(href) ? 'active' : ''}><span>{icon}</span>{label}</Link>)}
        <a href="/agendar" target="_blank" rel="noreferrer"><span>↗</span>Página de agendamento</a>
        <button onClick={sair}><span>→</span>Sair</button>
      </div>
    </div>}
  </div>;
}
