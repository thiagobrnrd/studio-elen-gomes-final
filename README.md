# Studio Elen Gomes — Sistema de gestão

Sistema interno do **Studio Elen Gomes**: agenda, clientes, serviços, financeiro e
lembretes por WhatsApp.

| Parte | Tecnologia | Pasta |
|---|---|---|
| Backend (API REST) | Java 21 + Spring Boot 3.4.5 + Spring Data JPA + Spring Security (JWT) | raiz do projeto |
| Frontend | Next.js 15 + React 19 + TypeScript | `frontend/` |
| Banco | PostgreSQL no Neon | — |

---

## 1. Criar o banco novo no Neon (2 minutos)

1. Entre em <https://console.neon.tech> → **New Project**.
   - Nome: `studio-elen-gomes` · Região: **AWS São Paulo (sa-east-1)** · Postgres 16 ou 17.
2. Na tela do projeto clique em **Connect** e copie a *connection string*. Ela tem este formato:
   ```
   postgresql://neondb_owner:SENHA@ep-xxxx-pooler.sa-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require
   ```
3. Cole **exatamente essa string** na variável `DATABASE_URL` (no `.env` ou no painel do deploy).
   Não precisa converter para JDBC nem separar usuário e senha — o backend faz isso sozinho.

**Não é preciso criar tabelas.** Na primeira vez que o backend sobe, o arquivo
`src/main/resources/schema.sql` cria todas elas (`clientes`, `servicos`, `agendamentos`,
`pagamentos`, `remarcacoes`, `notificacoes`, `fichas_capilares`). Nas próximas vezes ele não
altera nada (usa `CREATE TABLE IF NOT EXISTS`). Depois disso o Hibernate apenas **confere** se
as entidades batem com o banco (`ddl-auto=validate`); se algo estiver diferente, o backend
para na inicialização com uma mensagem clara, em vez de subir “meio funcionando”.

> ⚠️ A senha do banco antigo estava escrita no `application.properties`. Ela foi removida
> do código, mas se o projeto já foi para o GitHub, **apague o banco antigo** no Neon
> (ou troque a senha dele em *Roles → Reset password*).

---

## 2. Rodar no seu computador

### Backend
Crie o arquivo `.env` na raiz (ao lado do `pom.xml`) copiando o `.env.example`:

```properties
DATABASE_URL=postgresql://neondb_owner:SENHA@ep-xxxx-pooler.sa-east-1.aws.neon.tech/neondb?sslmode=require
ADMIN_USER=elen
ADMIN_PASSWORD=uma-senha-forte
JWT_SECRET=uma-frase-longa-e-aleatoria-com-mais-de-32-caracteres
FRONTEND_URL=http://localhost:3000
```

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

No console devem aparecer as linhas `Banco conectado: jdbc:postgresql://ep-...` e
`Clientes encontrados no banco: N`. Teste: <http://localhost:8080/api/health>.

### Frontend
```bash
cd frontend
cp .env.example .env.local    # NEXT_PUBLIC_API_URL=http://localhost:8080/api
npm install
npm run dev
```
Acesse <http://localhost:3000>.

---

## 3. Publicar (deploy)

### Backend — Render (Docker)
- **New → Web Service** apontando para o repositório. Runtime **Docker** (o `Dockerfile` já está pronto).
- Variáveis de ambiente:

| Variável | Valor |
|---|---|
| `DATABASE_URL` | connection string do Neon (como no passo 1) |
| `ADMIN_USER` / `ADMIN_PASSWORD` | login da Elen |
| `JWT_SECRET` | frase longa e aleatória |
| `FRONTEND_URL` | endereço do frontend, ex.: `https://studio-elen-gomes.vercel.app` (aceita vários separados por vírgula e curinga `https://*.vercel.app`) |

### Frontend — Vercel
- **Add New → Project** → importe o repositório.
- **Root Directory: `frontend`** ← sem isso a Vercel mostra `404 NOT_FOUND`.
- Variável: `NEXT_PUBLIC_API_URL=https://SEU-BACKEND.onrender.com/api`
- Depois do deploy, copie o endereço da Vercel para `FRONTEND_URL` no Render.

> No plano gratuito o Render “dorme” depois de 15 min sem uso; o primeiro acesso do dia pode
> levar ~40 s. O Neon também suspende o banco quando ocioso e acorda em ~1 s.

---

## 4. Como funciona

### Agenda e remarcação
- Novo agendamento mostra só os **horários livres** do dia, considerando a duração de cada serviço
  (expediente configurável: `STUDIO_ABERTURA` / `STUDIO_FECHAMENTO`, padrão 09:00–19:00).
- Conflito de horário é checado no servidor (inclusive serviços longos que começam antes).
- **Remarcar**: escolhe nova data → horários livres → confirma. O histórico fica salvo
  (`remarcacoes`), o status volta para *Agendado* e o sistema oferece a mensagem pronta para
  avisar a cliente no WhatsApp. O lembrete do horário antigo é liberado para o novo horário.
- **Receber** registra o pagamento e conclui o atendimento.

### Lembretes do WhatsApp (link wa.me)
Menu **Lembretes** — a mensagem já vem escrita; a Elen clica em **Enviar no WhatsApp**, o
WhatsApp do studio abre com o texto, ela aperta enviar. O lembrete sai da lista e vai para
*Enviados hoje* (com **Desfazer**).
- **Horários de hoje e amanhã** — atendimentos agendados/confirmados ainda não avisados.
- **Retornos** — serviços com “lembrar a cliente de voltar” cujo prazo venceu (até 30 dias) e a
  cliente ainda não reagendou.
- **Aniversários da semana** e **clientes sumidas** (mais de 60 dias).
- O telefone é convertido para `55 + DDD + número` automaticamente; se estiver incompleto, aparece
  um aviso para corrigir o cadastro.

Para trocar os textos: `STUDIO_WHATSAPP_MENSAGEM_HORARIO` e `STUDIO_WHATSAPP_MENSAGEM_RETORNO`
(campos: `{nome}`, `{quando}`, `{hora}`, `{servico}`, `{studio}`).

---

## 5. API (resumo)

| Método | Rota | O que faz |
|---|---|---|
| POST | `/api/auth/login` | login → token JWT |
| GET/POST/PUT/DELETE | `/api/clientes` | clientes (`?q=` busca por nome) |
| GET/POST/PUT/PATCH | `/api/servicos` | serviços (`/{id}/ativo` liga/desliga) |
| GET/POST | `/api/agendamentos` | `?data=AAAA-MM-DD` ou `?clienteId=` |
| PATCH | `/api/agendamentos/{id}/status?value=` | AGENDADO, CONFIRMADO, CONCLUIDO, CANCELADO |
| GET/POST | `/api/agendamentos/{id}/remarcacoes` | histórico / remarcar |
| GET | `/api/relacionamento/horarios-disponiveis` | `?servicoId=` ou `?agendamentoId=` + `&data=` |
| GET | `/api/whatsapp/central` | lembretes pendentes e enviados hoje |
| POST | `/api/whatsapp/marcar?agendamentoId=&tipo=` | marca lembrete como enviado |
| DELETE | `/api/whatsapp/enviados/{id}` | desfaz |
| POST | `/api/financeiro/pagamentos/{agendamentoId}` | registra pagamento |
| GET | `/api/financeiro/relatorio?mes=AAAA-MM` | relatório do mês |
| GET | `/api/dashboard` | números do dia |
