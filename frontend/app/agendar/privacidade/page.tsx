'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { publicApi } from '../../../lib/api';
import { waLink } from '../../../lib/format';
import { Pagina, esquecerDados } from '../../../components/publico';

export default function Privacidade() {
  const [info, setInfo] = useState<any>(null);
  const [apagado, setApagado] = useState(false);
  useEffect(() => { publicApi<any>('/info').then(setInfo).catch(() => {}); }, []);
  const studio = info?.nome || 'Studio Elen Gomes';
  const zap = info?.whatsapp ? waLink(info.whatsapp, 'Olá! Quero fazer uma solicitação sobre meus dados pessoais (LGPD).') : null;
  const contato = zap ? <a href={zap} target="_blank" rel="noreferrer">WhatsApp do studio</a> : 'WhatsApp do studio';

  return <Pagina info={info} titulo="Política de Privacidade" semMensagem>
    <article className="pb-card pb-legal">
      <p className="pb-upd">Última atualização: outubro de 2026</p>
      <p>Esta política explica, de forma simples, como o <b>{studio}</b> trata os seus dados pessoais quando você agenda um horário, conforme a <b>Lei Geral de Proteção de Dados (LGPD – Lei nº 13.709/2018)</b>.</p>

      <h2>1. Quem cuida dos seus dados</h2>
      <p>O controlador dos dados é o <b>{studio}</b>{info?.endereco ? <>, localizado em {info.endereco}</> : null}. Para qualquer assunto sobre seus dados, fale conosco pelo {contato}.</p>

      <h2>2. Quais dados coletamos</h2>
      <ul>
        <li><b>Nome e WhatsApp</b> — para identificar você e confirmar o horário.</li>
        <li><b>Serviço, data e horário escolhidos</b> — para organizar a agenda.</li>
        <li><b>Observação (opcional)</b> — o que você quiser contar sobre o atendimento.</li>
        <li><b>Histórico de atendimentos e pagamentos</b> — registrado pelo studio após cada visita.</li>
      </ul>
      <p>Não pedimos CPF, endereço, documentos nem dados de cartão.</p>

      <h2>3. Dados sensíveis</h2>
      <p>Se você informar voluntariamente alguma condição de saúde (por exemplo, <b>alergia ou sensibilidade a produtos</b>), essa informação é considerada <b>dado pessoal sensível</b> pela LGPD. Ela será usada <b>exclusivamente</b> para a segurança do seu procedimento, com base no seu consentimento (art. 11, I), e não será compartilhada com ninguém. Você não é obrigada a informar.</p>

      <h2>4. Para que usamos</h2>
      <ul>
        <li>Agendar, confirmar, remarcar ou cancelar seus horários;</li>
        <li>Enviar lembretes e avisos sobre o atendimento pelo WhatsApp;</li>
        <li>Manter o histórico dos serviços realizados e cumprir obrigações legais e fiscais.</li>
      </ul>
      <p>As bases legais são a execução do serviço que você solicitou (art. 7º, V), o seu consentimento (art. 7º, I) e o cumprimento de obrigação legal (art. 7º, II). <b>Não vendemos seus dados e não os usamos para publicidade de terceiros.</b></p>

      <h2>5. Com quem compartilhamos</h2>
      <p>Somente com os serviços de tecnologia que hospedam este sistema (servidores e banco de dados), que agem apenas sob nossas instruções e com medidas de segurança. Alguns desses provedores podem armazenar dados fora do Brasil, sempre com garantias adequadas de proteção (art. 33).</p>

      <h2>6. Por quanto tempo guardamos</h2>
      <p>Enquanto você for cliente do studio. Se pedir a exclusão, apagamos seus dados, exceto registros que a lei exige manter (como os financeiros), que ficam guardados apenas pelo prazo legal.</p>

      <h2>7. Segurança</h2>
      <p>O acesso ao sistema é restrito ao studio e protegido por senha, a conexão é criptografada (HTTPS) e o link de acompanhamento do seu pedido é único e secreto.</p>

      <h2>8. Seus direitos</h2>
      <p>Você pode, a qualquer momento e sem custo: confirmar se tratamos seus dados, acessá-los, corrigi-los, pedir a exclusão, revogar o consentimento e pedir informações sobre o compartilhamento (art. 18). Basta chamar no {contato}. Você também pode reclamar à Autoridade Nacional de Proteção de Dados (ANPD).</p>

      <h2>9. Dados salvos no seu aparelho</h2>
      <p>Para facilitar os próximos agendamentos, seu nome, WhatsApp e os links dos seus pedidos ficam guardados <b>apenas neste navegador</b>. Não usamos cookies de rastreamento ou publicidade.</p>
      <button className="pb-btn ghost pb-full" disabled={apagado} onClick={() => { esquecerDados(); setApagado(true); }}>
        {apagado ? '✓ Dados apagados deste aparelho' : 'Apagar meus dados deste aparelho'}
      </button>
    </article>
    <p className="pb-center pb-tiny pb-muted" style={{ marginTop: 18 }}><Link href="/agendar">← Voltar para o agendamento</Link></p>
  </Pagina>;
}
