// Datas sempre no horário local do navegador (o studio fica em São Paulo).
// Antes o sistema usava toISOString(), que é UTC: depois das 21h a agenda
// abria no dia seguinte.

const pad = (n: number) => String(n).padStart(2, '0');

/** Preço para a cliente: "a partir de R$ 90,00" quando o valor varia. */
export function precoCliente(v: number, aPartir?: boolean) { return (aPartir ? 'a partir de ' : '') + money(v); }

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
  SOLICITADO: 'Aguardando', AGENDADO: 'Agendado', CONFIRMADO: 'Confirmado', CONCLUIDO: 'Concluído', CANCELADO: 'Cancelado',
};

export const diaSemanaCurto = (iso: string) =>
  parseLocal(iso).toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', '');
export const mesCurto = (iso: string) =>
  parseLocal(iso).toLocaleDateString('pt-BR', { month: 'short' }).replace('.', '');
export const dataHoraLonga = (s: string) => `${dataLonga(s.slice(0, 10))}, às ${hora(s)}`;

const DIAS = ['', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];
/** [2,3,4,5,6] → "Ter a Sáb"; [1,3,5] → "Seg, Qua, Sex". */
export function resumoDias(dias: number[]) {
  const d = [...dias].sort((a, b) => a - b);
  if (!d.length) return '';
  const seguidos = d.every((v, i) => i === 0 || v === d[i - 1] + 1);
  if (seguidos && d.length > 2) return `${DIAS[d[0]]} a ${DIAS[d[d.length - 1]]}`;
  return d.map((x) => DIAS[x]).join(', ');
}

/** Arquivo .ics para "Adicionar ao calendário" (funciona no iPhone, Android e Outlook). */
export function baixarIcs(titulo: string, inicioIso: string, duracaoMin: number, local?: string | null, descricao?: string) {
  const ini = parseLocal(inicioIso);
  const fim = new Date(ini.getTime() + duracaoMin * 60000);
  const f = (d: Date) => `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}T${pad(d.getHours())}${pad(d.getMinutes())}00`;
  const esc = (t: string) => t.replace(/[\\,;]/g, (m) => '\\' + m).replace(/\n/g, '\\n');
  const ics = [
    'BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//Studio Elen Gomes//Agenda//PT-BR', 'CALSCALE:GREGORIAN',
    'BEGIN:VTIMEZONE', 'TZID:America/Sao_Paulo', 'BEGIN:STANDARD', 'DTSTART:19700101T000000', 'TZOFFSETFROM:-0300', 'TZOFFSETTO:-0300', 'END:STANDARD', 'END:VTIMEZONE',
    'BEGIN:VEVENT',
    `UID:${f(ini)}-${Math.random().toString(36).slice(2)}@studioelengomes`,
    `DTSTAMP:${f(new Date())}`,
    `DTSTART;TZID=America/Sao_Paulo:${f(ini)}`,
    `DTEND;TZID=America/Sao_Paulo:${f(fim)}`,
    `SUMMARY:${esc(titulo)}`,
    local ? `LOCATION:${esc(local)}` : '',
    descricao ? `DESCRIPTION:${esc(descricao)}` : '',
    'BEGIN:VALARM', 'TRIGGER:-PT2H', 'ACTION:DISPLAY', `DESCRIPTION:${esc(titulo)}`, 'END:VALARM',
    'END:VEVENT', 'END:VCALENDAR',
  ].filter(Boolean).join('\r\n');
  const url = URL.createObjectURL(new Blob([ics], { type: 'text/calendar;charset=utf-8' }));
  const a = document.createElement('a');
  a.href = url; a.download = 'studio-elen-gomes.ics';
  document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
