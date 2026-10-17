'use client';
import {useEffect,useState} from 'react';
import Shell from '../../components/Shell';
import Guard from '../../components/Guard';
import {api} from '../../lib/api';
import {mesISO,dataHora} from '../../lib/format';
import {ErrorBox,Loading} from '../../components/ui';

type Payment={id:string,clienteNome:string,servicoNome:string,valor:number,formaPagamento:string,pagoEm:string};
type Report={mesAno:string,total:number,quantidadeAtendimentos:number,pagamentos:Payment[],porFormaPagamento:Record<string,number>,servicosMaisRealizados:{nome:string,quantidade:number,total:number}[],clientesDestaque:{nome:string,quantidade:number,total:number}[]};
const money=(n:number)=>new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(Number(n)||0);
const forma=(v:string)=>({DINHEIRO:'Dinheiro',PIX:'Pix',CARTAO_DEBITO:'Débito',CARTAO_CREDITO:'Crédito'} as Record<string,string>)[v]||v;
export default function Financeiro(){
  const [month,setMonth]=useState(mesISO());
  const [data,setData]=useState<Report|null>(null); const [loading,setLoading]=useState(true); const [err,setErr]=useState('');
  async function load(){setLoading(true);setErr('');try{setData(await api<Report>(`/financeiro/relatorio?mes=${month}`))}catch(e:any){setErr(e.message)}finally{setLoading(false)}}
  useEffect(()=>{load()},[month]);
  const maxForma=Math.max(...Object.values(data?.porFormaPagamento||{}),1);
  const ranking=Object.entries(data?.porFormaPagamento||{}).sort((a,b)=>b[1]-a[1]);
  return <Guard><Shell><header className="topbar"><div><div className="eyebrow">Controle financeiro</div><h1>Financeiro</h1><p className="page-subtitle">Acompanhe o que entrou no studio e onde o faturamento está concentrado.</p></div><div className="top-actions"><input type="month" className="input" value={month} onChange={e=>e.target.value&&setMonth(e.target.value)}/></div></header>
    {err&&<ErrorBox error={err} onRetry={load}/>}
    {loading?<div className="card"><Loading text="Carregando relatório..."/></div>:<>
      <div className="grid stats finance-stats">
        <div className="card stat-feature"><div className="stat-label">Faturamento do mês</div><div className="stat-value">{money(data?.total||0)}</div><div className="stat-detail">recebido no período</div></div>
        <div className="card"><div className="stat-label">Atendimentos pagos</div><div className="stat-value">{data?.quantidadeAtendimentos||0}</div><div className="stat-detail">transações registradas</div></div>
        <div className="card"><div className="stat-label">Ticket médio</div><div className="stat-value">{money((data?.total||0)/(data?.quantidadeAtendimentos||1))}</div><div className="stat-detail">por atendimento</div></div>
        <div className="card"><div className="stat-label">Forma principal</div><div className="stat-value finance-method">{ranking[0] ? forma(ranking[0][0]) : '—'}</div><div className="stat-detail">maior participação</div></div>
      </div>
      <div className="grid finance-grid">
        <section className="card"><div className="section-title"><div><h2>Formas de pagamento</h2><p className="muted">Distribuição do faturamento</p></div></div><div className="finance-bars">{Object.entries(data?.porFormaPagamento||{}).map(([k,v])=><div className="bar-row" key={k}><div className="bar-label"><span>{forma(k)}</span><b>{money(v)}</b></div><div className="bar-track"><div className="bar-fill" style={{width:`${Math.max((v/maxForma)*100,4)}%`}}/></div></div>)}{!Object.keys(data?.porFormaPagamento||{}).length&&<div className="empty">Nenhum pagamento no período.</div>}</div></section>
        <section className="card"><div className="section-title"><div><h2>Serviços em destaque</h2><p className="muted">Mais realizados no período</p></div></div><div className="rank-list">{(data?.servicosMaisRealizados||[]).slice(0,5).map((s,i)=><div className="rank-item" key={s.nome}><span className="rank-number">{String(i+1).padStart(2,'0')}</span><div><b>{s.nome}</b><small>{s.quantidade} atendimento(s)</small></div><strong>{money(s.total)}</strong></div>)}{!data?.servicosMaisRealizados?.length&&<div className="empty">Sem dados.</div>}</div></section>
      </div>
      <div className="grid finance-grid">
        <section className="card"><div className="section-title"><div><h2>Clientes que mais gastaram</h2><p className="muted">Relacionamento e recorrência</p></div></div><div className="rank-list">{(data?.clientesDestaque||[]).slice(0,5).map((c,i)=><div className="rank-item" key={c.nome}><span className="rank-number">{String(i+1).padStart(2,'0')}</span><div><b>{c.nome}</b><small>{c.quantidade} atendimento(s)</small></div><strong>{money(c.total)}</strong></div>)}{!data?.clientesDestaque?.length&&<div className="empty">Sem dados.</div>}</div></section>
        <section className="card"><div className="section-title"><div><h2>Últimos recebimentos</h2><p className="muted">Pagamentos registrados</p></div></div><div className="table-wrap"><table className="table"><thead><tr><th>Cliente</th><th>Data</th><th>Forma</th><th>Valor</th></tr></thead><tbody>{(data?.pagamentos||[]).slice(0,12).map(p=><tr key={p.id}><td><b>{p.clienteNome}</b><br/><span className="muted">{p.servicoNome}</span></td><td>{p.pagoEm?dataHora(p.pagoEm):'—'}</td><td>{forma(p.formaPagamento)}</td><td><b>{money(p.valor)}</b></td></tr>)}</tbody></table>{!data?.pagamentos?.length&&<div className="empty">Nenhum recebimento.</div>}</div></section>
      </div>
    </>}
  </Shell></Guard>
}
