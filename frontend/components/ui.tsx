'use client';
import { useEffect, useState } from 'react';

// ─── Toast (avisos rápidos no canto da tela) ─────────────────────
type ToastMsg = { id: number; text: string; kind: 'ok' | 'erro' };
const listeners = new Set<(t: ToastMsg) => void>();
let seq = 0;

export function toast(text: string, kind: 'ok' | 'erro' = 'ok') {
  const t = { id: ++seq, text, kind };
  listeners.forEach((l) => l(t));
}
export const toastErro = (e: unknown) => toast(e instanceof Error ? e.message : String(e), 'erro');

export function Toaster() {
  const [list, setList] = useState<ToastMsg[]>([]);
  useEffect(() => {
    const l = (t: ToastMsg) => {
      setList((x) => [...x, t]);
      setTimeout(() => setList((x) => x.filter((i) => i.id !== t.id)), t.kind === 'erro' ? 6000 : 3200);
    };
    listeners.add(l);
    return () => { listeners.delete(l); };
  }, []);
  return <div className="toaster" role="status" aria-live="polite">
    {list.map((t) => <div key={t.id} className={'toast ' + t.kind}>{t.kind === 'ok' ? '✓' : '!'} <span>{t.text}</span></div>)}
  </div>;
}

// ─── Modal ───────────────────────────────────────────────────────
export function Modal({ title, eyebrow, onClose, children, wide }: {
  title: string; eyebrow?: string; onClose: () => void; children: React.ReactNode; wide?: boolean;
}) {
  useEffect(() => {
    const k = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', k);
    document.body.style.overflow = 'hidden';
    return () => { window.removeEventListener('keydown', k); document.body.style.overflow = ''; };
  }, [onClose]);
  return <div className="modal-overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
    <div className={'modal-card' + (wide ? ' wide' : '')} role="dialog" aria-modal="true" aria-label={title}>
      <div className="modal-head">
        <div>{eyebrow && <div className="eyebrow">{eyebrow}</div>}<h2>{title}</h2></div>
        <button type="button" className="icon-btn" onClick={onClose} aria-label="Fechar">×</button>
      </div>
      {children}
    </div>
  </div>;
}

export function Loading({ text = 'Carregando...' }: { text?: string }) {
  return <div className="empty"><span className="spinner" /> {text}</div>;
}

export function ErrorBox({ error, onRetry }: { error: string; onRetry?: () => void }) {
  return <div className="notice error-notice">
    <span>{error}</span>
    {onRetry && <button className="btn" onClick={onRetry}>Tentar de novo</button>}
  </div>;
}
