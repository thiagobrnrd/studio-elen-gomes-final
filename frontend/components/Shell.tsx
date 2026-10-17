'use client';
import Image from 'next/image';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { clearToken } from '../lib/api';
import { Toaster } from './ui';

const nav = [
  ['/dashboard','⌂','Visão geral'],
  ['/agenda','◷','Agenda'],
  ['/clientes','♙','Clientes'],
  ['/servicos','✦','Serviços'],
  ['/financeiro','R$','Financeiro'],
  ['/lembretes','◌','Lembretes'],
];

export default function Shell({children}:{children:React.ReactNode}) {
  const p = usePathname();
  const r = useRouter();
  return <div className="app">
    <aside className="sidebar">
      <Link href="/dashboard" className="sidebar-brand" aria-label="Studio Elen Gomes">
        <Image src="/images/logo-elen-gomes.png" alt="Elen Gomes Studio" width={170} height={102} priority className="sidebar-logo" />
      </Link>
      <div className="sidebar-divider" />
      <div className="sidebar-label">GESTÃO DO STUDIO</div>
      <nav className="nav">
        {nav.map(([href,icon,label]) => <Link className={p.startsWith(href) ? 'active' : ''} href={href} key={href}>
          <span className="nav-icon">{icon}</span><span>{label}</span>
        </Link>)}
      </nav>
      <div className="sidebar-bottom">
        <div className="sidebar-mini-brand"><span>EG</span><div><b>Studio Elen Gomes</b><small>Gestão do salão</small></div></div>
        <button className="btn ghost logout" onClick={()=>{clearToken();r.replace('/login')}}>Sair <span>→</span></button>
      </div>
    </aside>
    <main className="main">
      <div className="mobile-header">
        <Image src="/images/logo-elen-gomes.png" alt="Elen Gomes Studio" width={116} height={70} priority />
        <button className="btn ghost" onClick={()=>{clearToken();r.replace('/login')}}>Sair</button>
      </div>
      {children}
    </main>
    <Toaster />
    <nav className="mobile-nav">
      {nav.map(([href,icon,label]) => <Link href={href} key={href} className={p.startsWith(href)?'active':''}><div>{icon}</div><span>{label}</span></Link>)}
    </nav>
  </div>
}
