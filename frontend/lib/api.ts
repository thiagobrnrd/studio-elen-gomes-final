export const API = (process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api').replace(/\/+$/, '');

const TOKEN_KEY = 'studio_token';

export function getToken() {
  if (typeof window === 'undefined') return '';
  try { return localStorage.getItem(TOKEN_KEY) || ''; } catch { return ''; }
}
export function setToken(token: string) { try { localStorage.setItem(TOKEN_KEY, token); } catch {} }
export function clearToken() { try { localStorage.removeItem(TOKEN_KEY); } catch {} }

function logout(): never {
  clearToken();
  if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) window.location.href = '/login';
  throw new Error('Sessão expirada. Faça login novamente.');
}

/**
 * Chama a API do Spring Boot com o token JWT.
 * O backend no plano gratuito (Render/Neon) pode "acordar" devagar, por isso
 * uma falha de rede é tentada mais uma vez antes de mostrar erro.
 */
export async function api<T>(path: string, options: RequestInit = {}, tentativa = 1): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body) headers.set('Content-Type', 'application/json');
  const token = getToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);

  let res: Response;
  try {
    res = await fetch(`${API}${path}`, { ...options, headers, cache: 'no-store' });
  } catch {
    if (tentativa < 2) {
      await new Promise((r) => setTimeout(r, 2500));
      return api<T>(path, options, tentativa + 1);
    }
    throw new Error('Não foi possível conectar ao servidor. Verifique sua internet ou se o sistema está no ar.');
  }

  if (res.status === 401) logout();

  const text = await res.text();
  let data: any = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (!res.ok) throw new Error(data?.message || `Erro ${res.status} ao comunicar com o servidor.`);
  return data as T;
}

// ─── Tipos compartilhados ──────────────────────────────────────────
export type Status = 'AGENDADO' | 'CONFIRMADO' | 'CONCLUIDO' | 'CANCELADO';
export type Agendamento = {
  id: string; clienteId: string; clienteNome: string; telefone: string; whatsapp: string | null;
  servicoId: string; servicoNome: string; duracaoMin: number; valor: number;
  dataHora: string; status: Status; observacoes?: string | null; pago: boolean;
};
export type Cliente = {
  id?: string; nome: string; telefone: string; email?: string | null; dataNascimento?: string | null;
  totalAtendimentos?: number; whatsapp?: string | null;
};
export type Servico = {
  id?: string; nome: string; descricao?: string | null; duracaoMin: number; preco: number;
  ativo: boolean; requerRetorno: boolean; diasParaRetorno?: number | null;
};
export type Slot = { dataHora: string; hora: string };
export type Lembrete = {
  agendamentoId: string; notificacaoId?: number | null; tipo: 'LEMBRETE_24H' | 'LEMBRETE_RETORNO';
  clienteId: string; clienteNome: string; telefone: string; servicoNome: string; dataHora: string;
  mensagem: string; link: string | null;
};
