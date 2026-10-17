// Datas sempre no horário local do navegador (o studio fica em São Paulo).
// Antes o sistema usava toISOString(), que é UTC: depois das 21h a agenda
// abria no dia seguinte.

const pad = (n: number) => String(n).padStart(2, '0');

export function hojeISO(d = new Date()) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
export function mesISO(d = new Date()) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
}
export function somarDias(iso: string, dias: number) {
  const [y, m, d] = iso.split('-').map(Number);
  return hojeISO(new Date(y, m - 1, d + dias));
}

/** "2026-10-16T14:30:00" → Date local (sem conversão de fuso). */
export function parseLocal(s: string) {
  const [data, hora = '00:00'] = s.split('T');
  const [y, m, d] = data.split('-').map(Number);
  const [hh, mm] = hora.split(':').map(Number);
  return new Date(y, m - 1, d, hh || 0, mm || 0);
}

export const hora = (s: string) => parseLocal(s).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
export const diaMes = (s: string) => parseLocal(s).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' });
export const dataHora = (s: string) => `${diaMes(s)} às ${hora(s)}`;
export const dataLonga = (iso: string) => {
  const t = parseLocal(iso).toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long' });
  return t.charAt(0).toUpperCase() + t.slice(1);
};

export const money = (n: number | null | undefined) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(n) || 0);

export const primeiroNome = (nome: string) => (nome || '').trim().split(/\s+/)[0] || '';

/** Link wa.me com a mensagem pronta. `numero` já vem do backend no formato 55DDDNUMERO. */
export function waLink(numero: string | null | undefined, mensagem: string) {
  if (!numero) return null;
  return `https://wa.me/${numero}?text=${encodeURIComponent(mensagem)}`;
}

/** Máscara simples de telefone brasileiro enquanto digita. */
export function mascaraTelefone(v: string) {
  const d = v.replace(/\D/g, '').slice(0, 11);
  if (d.length <= 2) return d ? `(${d}` : '';
  if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
  if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
}

export const STATUS_LABEL: Record<string, string> = {
  AGENDADO: 'Agendado', CONFIRMADO: 'Confirmado', CONCLUIDO: 'Concluído', CANCELADO: 'Cancelado',
};
