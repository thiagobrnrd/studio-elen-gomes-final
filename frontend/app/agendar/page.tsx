'use client';
import { useEffect, useMemo, useRef, useState } from 'react';
import Link from 'next/link';
import { publicApi, PedidoPublico } from '../../lib/api';
import { Pagina, lerPedidos, salvarPedido, PedidoSalvo } from '../../components/publico';
import {
  baixarIcs, dataHoraLonga, dataLonga, diaSemanaCurto, mascaraTelefone, mesCurto, money, waLink,
  precoCliente,
} from '../../lib/format';

type ServicoPub = { id: string; nome: string; descricao?: string | null; duracaoMin: number; preco: number; precoAPartir?: boolean };
type Info = {
  nome: string; onlineAtivo: boolean; mensagem: string; endereco: string; instagram: string; whatsapp: string;
  politica: string; abertura: string; fechamento: string; diasFuncionamento: number[]; servicos: ServicoPub[];
};
type Dia = { data: string; vagas: number };
type Slot = { dataHora: string; hora: string };

const duracao = (m: number) => (m < 60 ? `${m} min` : `${Math.floor(m / 60)}h${m % 60 ? String(m % 60).padStart(2, '0') : ''}`);

export default function Agendar() {
  const [info, setInfo] = useState<Info | null>(null);
  const [erroInfo, setErroInfo] = useState('');
  const [servico, setServico] = useState<ServicoPub | null>(null);
  const [dias, setDias] = useState<Dia[] | null>(null);
  const [dia, setDia] = useState('');
  const [slots, setSlots] = useState<Slot[] | null>(null);
  const [slot, setSlot] = useState<Slot | null>(null);
  const [nome, setNome] = useState('');
  const [tel, setTel] = useState('');
  const [obs, setObs] = useState('');
  const [aceite, setAceite] = useState(false);
  const [website, setWebsite] = useState('');
  const [privacidade, setPrivacidade] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');
  const [pedido, setPedido] = useState<PedidoPublico | null>(null);
  const [meus, setMeus] = useState<PedidoSalvo[]>([]);
  const refs = { dia: useRef<HTMLDivElement>(null), hora: useRef<HTMLDivElement>(null), dados: useRef<HTMLDivElement>(null) };

  function carregar() {
    setErroInfo('');
    publicApi<Info>('/info').then(setInfo).catch((e) => setErroInfo(e.message));
  }
  useEffect(() => {
    carregar();
    setMeus(lerPedidos().filter((p) => new Date(p.dataHora) > new Date(Date.now() - 86400000)));
    try {
      const n = localStorage.getItem('studio_cliente_nome'); const t = localStorage.getItem('studio_cliente_tel');
      if (n) setNome(n); if (t) setTel(t);
    } catch {}
  }, []);

  const rolar = (r: React.RefObject<HTMLDivElement | null>) => setTimeout(() => r.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 80);

  function escolherServico(s: ServicoPub) {
    setServico(s); setDia(''); setSlots(null); setSlot(null); setDias(null); setErro('');
    publicApi<Dia[]>(`/dias?servicoId=${s.id}`).then((d) => {
      setDias(d);
      const primeiro = d.find((x) => x.vagas > 0);
      if (primeiro) escolherDia(primeiro.data, s);
    }).catch((e) => setErro(e.message));
    rolar(refs.dia);
  }

  function escolherDia(d: string, s = servico) {
    if (!s) return;
    setDia(d); setSlot(null); setSlots(null);
    publicApi<Slot[]>(`/horarios?servicoId=${s.id}&data=${d}`).then(setSlots).catch((e) => setErro(e.message));
  }

  function escolherSlot(s: Slot) { setSlot(s); rolar(refs.dados); }

  const periodos = useMemo(() => {
    const g: Record<string, Slot[]> = { Manhã: [], Tarde: [], Noite: [] };
    (slots || []).forEach((s) => { const h = Number(s.hora.slice(0, 2)); (h < 12 ? g.Manhã : h < 18 ? g.Tarde : g.Noite).push(s); });
    return Object.entries(g).filter(([, v]) => v.length);
  }, [slots]);

  async function enviar(e: React.FormEvent) {
    e.preventDefault();
    if (!servico || !slot) return;
    if (nome.trim().split(/\s+/).length < 2) { setErro('Informe seu nome e sobrenome.'); return; }
    if (tel.replace(/\D/g, '').length < 10) { setErro('Informe seu WhatsApp com DDD.'); return; }
    if (info?.politica && !aceite) { setErro('Confirme que leu a política de cancelamento.'); return; }
    if (!privacidade) { setErro('Para agendar, aceite a Política de Privacidade.'); return; }
    setEnviando(true); setErro('');
    try {
      const p = await publicApi<PedidoPublico>('/agendamentos', {
        method: 'POST',
        body: JSON.stringify({ servicoId: servico.id, dataHora: slot.dataHora, nome, telefone: tel, observacoes: obs, hp: website, aceitePrivacidade: privacidade }),
      });
      salvarPedido({ token: p.token, servico: p.servicoNome, dataHora: p.dataHora });
      try { localStorage.setItem('studio_cliente_nome', nome); localStorage.setItem('studio_cliente_tel', tel); } catch {}
      setPedido(p);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (e: any) {
      setErro(e.message);
      if (/reservado|escolha outro/i.test(e.message)) { escolherDia(dia); rolar(refs.hora); }
    } finally { setEnviando(false); }
  }

  if (pedido) return <Pagina info={info}><Sucesso p={pedido} info={info} /></Pagina>;

  return <Pagina info={info}>
    {erroInfo && <div className="pb-alert">{erroInfo} <button className="pb-link" onClick={carregar}>Tentar de novo</button></div>}
    {!info && !erroInfo && <div className="pb-loading"><span className="spinner" /> Carregando a agenda do studio…</div>}

    {info && !info.onlineAtivo && <div className="pb-card pb-center">
      <h2 className="pb-h2">Agendamento online pausado</h2>
      <p className="pb-muted">No momento os horários estão sendo marcados pelo WhatsApp.</p>
      {info.whatsapp && <a className="pb-btn" href={waLink(info.whatsapp, 'Olá! Gostaria de agendar um horário.')!} target="_blank" rel="noreferrer">Falar no WhatsApp</a>}
    </div>}

    {meus.length > 0 && info?.onlineAtivo && <div className="pb-mine">
      <span>Você tem {meus.length === 1 ? 'um pedido' : `${meus.length} pedidos`} recente{meus.length > 1 ? 's' : ''}:</span>
      {meus.slice(0, 3).map((m) => <Link key={m.token} href={`/agendar/pedido/${m.token}`}>{m.servico} · {dataHoraLonga(m.dataHora)} →</Link>)}
    </div>}

    {info?.onlineAtivo && <>
      <Passo n={1} titulo="Escolha o serviço" feito={!!servico}>
        <div className="pb-services">
          {info.servicos.map((s) => <button key={s.id} type="button" className={'pb-service' + (servico?.id === s.id ? ' on' : '')} onClick={() => escolherServico(s)}>
            <div><b>{s.nome}</b>{s.descricao && <small>{s.descricao}</small>}<span className="pb-tag">⏱ {duracao(s.duracaoMin)}</span></div>
            <strong>{s.precoAPartir && <small className="pb-apartir">a partir de </small>}{money(s.preco)}</strong>
          </button>)}
          {!info.servicos.length && <p className="pb-muted">Nenhum serviço disponível online no momento.</p>}
        </div>
      </Passo>

      {servico && <div ref={refs.dia}><Passo n={2} titulo="Escolha o dia" feito={!!dia}>
        {dias === null ? <div className="pb-loading"><span className="spinner" /> Buscando dias disponíveis…</div>
          : dias.every((d) => d.vagas === 0) ? <p className="pb-muted">Sem horários livres nos próximos dias. Fale com o studio pelo WhatsApp.</p>
          : <div className="pb-days">
            {dias.map((d) => <button key={d.data} type="button" disabled={!d.vagas} className={'pb-day' + (dia === d.data ? ' on' : '')} onClick={() => { escolherDia(d.data); rolar(refs.hora); }}>
              <small>{diaSemanaCurto(d.data)}</small><b>{d.data.slice(8, 10)}</b><small>{mesCurto(d.data)}</small>
              {!d.vagas && <em>lotado</em>}
            </button>)}
          </div>}
      </Passo></div>}

      {servico && dia && <div ref={refs.hora}><Passo n={3} titulo="Escolha o horário" feito={!!slot}>
        <p className="pb-muted pb-sub">{dataLonga(dia)}</p>
        {slots === null ? <div className="pb-loading"><span className="spinner" /> Buscando horários…</div>
          : !slots.length ? <p className="pb-muted">Não há horários livres neste dia. Escolha outra data.</p>
          : periodos.map(([p, lista]) => <div key={p} className="pb-period"><span>{p}</span>
            <div className="pb-slots">{lista.map((s) => <button key={s.dataHora} type="button" className={'pb-slot' + (slot?.dataHora === s.dataHora ? ' on' : '')} onClick={() => escolherSlot(s)}>{s.hora}</button>)}</div>
          </div>)}
      </Passo></div>}

      {servico && slot && <div ref={refs.dados}><Passo n={4} titulo="Seus dados" feito={false}>
        <div className="pb-summary">
          <div><small>Serviço</small><b>{servico.nome}</b></div>
          <div><small>Quando</small><b>{dataHoraLonga(slot.dataHora)}</b></div>
          <div><small>Duração · Valor</small><b>{duracao(servico.duracaoMin)} · {precoCliente(servico.preco, servico.precoAPartir)}</b></div>
        </div>
        <form onSubmit={enviar} className="pb-form" noValidate>
          <label>Nome completo<input value={nome} onChange={(e) => setNome(e.target.value)} autoComplete="name" placeholder="Ex.: Ana Souza" /></label>
          <label>WhatsApp<input value={tel} onChange={(e) => setTel(mascaraTelefone(e.target.value))} inputMode="tel" autoComplete="tel-national" placeholder="(11) 99999-9999" /></label>
          <label>Observação <span className="pb-opt">(opcional)</span><textarea rows={2} value={obs} onChange={(e) => setObs(e.target.value)} maxLength={500} placeholder="Ex.: cabelo longo, quero manter o comprimento" />
            <span className="pb-hint">Tem alergia ou sensibilidade a algum produto? Pode contar aqui — essa informação é usada só para a segurança do seu atendimento.</span></label>
          <input className="pb-hp" tabIndex={-1} autoComplete="one-time-code" value={website} onChange={(e) => setWebsite(e.target.value)} aria-hidden="true" name="zq_hp_92" id="zq_hp_92" />
          {info.politica && <label className="pb-check"><input type="checkbox" checked={aceite} onChange={(e) => setAceite(e.target.checked)} /><span><b>Política de cancelamento:</b> {info.politica}</span></label>}
          <label className="pb-check"><input type="checkbox" checked={privacidade} onChange={(e) => setPrivacidade(e.target.checked)} /><span>Li e aceito a <a href="/agendar/privacidade" target="_blank" rel="noreferrer">Política de Privacidade</a> e autorizo o uso dos meus dados para agendar e confirmar meu atendimento.</span></label>
          {erro && <div className="pb-alert">{erro}</div>}
          <button className="pb-btn pb-full" disabled={enviando}>{enviando ? <><span className="spinner dark" /> Enviando…</> : 'Solicitar agendamento'}</button>
          <p className="pb-muted pb-tiny pb-lgpd">🔒 Seus dados são protegidos conforme a LGPD (Lei 13.709/2018) e nunca são vendidos ou compartilhados para publicidade.</p>
        </form>
      </Passo></div>}
      {erro && !slot && <div className="pb-alert">{erro}</div>}
    </>}
  </Pagina>;
}

function Passo({ n, titulo, feito, children }: { n: number; titulo: string; feito: boolean; children: React.ReactNode }) {
  return <section className="pb-step">
    <div className="pb-step-head"><span className={'pb-num' + (feito ? ' ok' : '')}>{feito ? '✓' : n}</span><h2>{titulo}</h2></div>
    {children}
  </section>;
}

function Sucesso({ p, info }: { p: PedidoPublico; info: Info | null }) {
  const confirmado = p.status === 'CONFIRMADO';
  const link = typeof window !== 'undefined' ? `${window.location.origin}/agendar/pedido/${p.token}` : '';
  const msg = `Olá! Acabei de ${confirmado ? 'agendar' : 'solicitar'} pelo site: ${p.servicoNome} em ${dataHoraLonga(p.dataHora)}. Meu nome é ${p.clienteNome}.`;
  return <div className="pb-card pb-success">
    <div className="pb-check-big">✓</div>
    <h2 className="pb-h2">{confirmado ? 'Horário confirmado!' : 'Pedido enviado!'}</h2>
    <p className="pb-muted">{confirmado
      ? `Tudo certo, ${p.clienteNome}. Seu horário está reservado.`
      : `Obrigada, ${p.clienteNome}! Seu horário está reservado e o studio vai confirmar pelo WhatsApp em breve.`}</p>
    <div className="pb-summary">
      <div><small>Serviço</small><b>{p.servicoNome}</b></div>
      <div><small>Quando</small><b>{dataHoraLonga(p.dataHora)}</b></div>
      <div><small>Valor</small><b>{precoCliente(p.valor, p.precoAPartir)}</b></div>
      {info?.endereco && <div><small>Endereço</small><b>{info.endereco}</b></div>}
    </div>
    <div className="pb-actions">
      <Link className="pb-btn pb-full" href={`/agendar/pedido/${p.token}`}>Acompanhar meu pedido</Link>
      <button className="pb-btn ghost pb-full" onClick={() => baixarIcs(`${p.servicoNome} — Studio Elen Gomes`, p.dataHora, p.duracaoMin, info?.endereco, `Acompanhe ou cancele: ${link}`)}>📅 Adicionar ao calendário</button>
      {p.studioWhatsapp && <a className="pb-btn ghost pb-full" href={waLink(p.studioWhatsapp, msg)!} target="_blank" rel="noreferrer">💬 Avisar o studio no WhatsApp</a>}
    </div>
    <p className="pb-muted pb-tiny">Guarde o link “Acompanhar meu pedido”: por ele você vê a confirmação e pode cancelar, se precisar.</p>
  </div>;
}
