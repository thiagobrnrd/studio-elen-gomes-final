'use client';

import { useEffect, useState } from 'react';
import { API, getToken, setToken } from '../../lib/api';

/**
 * Login sobre a arte oficial do Studio Elen Gomes.
 *
 * A imagem fica num "palco" com a mesma proporção dela (1670×941). O formulário
 * é posicionado em porcentagem desse palco, exatamente sobre o quadro dourado,
 * então continua alinhado em qualquer tamanho de tela. Em celular (tela em pé)
 * a arte vai para o topo e o formulário fica logo abaixo.
 */
export default function Login() {
  const [u, setU] = useState('');
  const [p, setP] = useState('');
  const [err, setErr] = useState('');
  const [loading, setLoading] = useState(false);
  const [show, setShow] = useState(false);

  useEffect(() => {
    if (getToken()) window.location.replace('/dashboard');
  }, []);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!u.trim() || !p) { setErr('Informe usuário e senha.'); return; }
    setLoading(true);
    setErr('');
    try {
      let r: Response;
      try {
        r = await fetch(`${API}/auth/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: u.trim(), password: p }),
        });
      } catch {
        throw new Error('Não foi possível conectar ao servidor. Tente novamente em alguns segundos.');
      }
      const d = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(d.message || 'Usuário ou senha inválidos.');
      setToken(d.token);
      window.location.replace('/dashboard');
    } catch (e: any) {
      setErr(e.message || 'Não foi possível entrar.');
      setLoading(false);
    }
  }

  return (
    <main className="login">
      <div className="login-stage">
        <div className="login-art" role="img" aria-label="Elen Gomes Studio — Beleza, autoestima, você" />

        <form className="login-frame" onSubmit={submit} noValidate>
          <div className="login-head">
            <span className="login-eyebrow">Área restrita</span>
            <h1>Bem-vinda</h1>
            <p>Acesse o sistema do studio</p>
          </div>

          <label className="login-field">
            <span className="sr-only">Usuário</span>
            <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="4" /><path d="M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7" /></svg>
            <input
              value={u}
              onChange={(e) => setU(e.target.value)}
              autoComplete="username"
              autoCapitalize="none"
              spellCheck={false}
              placeholder="Usuário"
              autoFocus
            />
          </label>

          <label className="login-field">
            <span className="sr-only">Senha</span>
            <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="10.5" width="14" height="10" rx="2" /><path d="M8 10.5V7.5a4 4 0 0 1 8 0v3" /></svg>
            <input
              type={show ? 'text' : 'password'}
              value={p}
              onChange={(e) => setP(e.target.value)}
              autoComplete="current-password"
              placeholder="Senha"
            />
            <button type="button" className="login-eye" onClick={() => setShow(!show)} aria-label={show ? 'Ocultar senha' : 'Mostrar senha'}>
              {show ? 'Ocultar' : 'Mostrar'}
            </button>
          </label>

          <p className="login-error" role="alert">{err}</p>

          <button className="login-submit" disabled={loading}>
            {loading ? <><span className="spinner dark" /> Entrando...</> : 'Entrar'}
          </button>

        </form>
      </div>
    </main>
  );
}
