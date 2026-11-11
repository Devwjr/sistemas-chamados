# Núcleo · Central de suporte TI

Aplicação interna para abrir e acompanhar chamados de TI. O sistema usa Spring Boot, React/TypeScript e PostgreSQL, executados diretamente no computador.

## Estado atual

- Interface: login, troca obrigatória de senha, painel, listagem, busca, filtro de status, abertura e detalhe de chamados, comentários, histórico e ações de atendimento.
- Administração: cadastro de usuários, setores e categorias, edição, desativação e redefinição de senha.
- Ainda faltam testes automatizados de negócio e validação ponta a ponta das permissões. A interface não expõe todos os filtros disponíveis na API nem a alteração de categoria no detalhe.

## Requisitos

- JDK 21.
- Node.js 22.12 ou superior e npm.
- PostgreSQL 17 instalado e em execução.
- O script `backend/mvnw` baixa Maven 3.9.11 na primeira execução e requer `curl`, `tar` e `sha512sum`.

## Preparar o PostgreSQL

Crie um usuário e um banco para a aplicação. No Linux, por exemplo:

```bash
sudo -u postgres psql
```

No console do PostgreSQL, substitua `SENHA_DO_BANCO` por uma senha própria:

```sql
CREATE ROLE suporte LOGIN PASSWORD 'SENHA_DO_BANCO';
CREATE DATABASE suporte OWNER suporte;
\q
```

Na raiz do projeto, copie o exemplo de ambiente e edite as variáveis:

```bash
cp .env.example .env
chmod 600 .env
nano .env
```

Preencha `DB_PASSWORD` com a mesma senha usada ao criar o usuário do banco e escolha uma senha inicial forte para `BOOTSTRAP_ADMIN_PASSWORD` (mínimo 12 caracteres, máximo 72 bytes UTF-8). Para desenvolvimento local por HTTP, mantenha `COOKIE_SECURE=false`.

| Variável | Uso |
|---|---|
| `DB_URL` | Endereço JDBC; por padrão use `jdbc:postgresql://localhost:5432/suporte`. |
| `DB_USERNAME` | Usuário PostgreSQL; padrão `suporte`. |
| `DB_PASSWORD` | Senha do usuário PostgreSQL. |
| `BOOTSTRAP_ADMIN_EMAIL` | E-mail inicial; padrão `admin@empresa.local`. |
| `BOOTSTRAP_ADMIN_PASSWORD` | Senha inicial obrigatória do administrador. |
| `COOKIE_SECURE` | `false` para desenvolvimento HTTP; produção exige HTTPS e `true`. |

O `.env` é ignorado pelo Git. Se não quiser carregar as variáveis pelo shell, configure-as no ambiente antes de iniciar o backend.

## Iniciar a aplicação

Em um terminal Bash ou Zsh na raiz do projeto, carregue as variáveis e inicie o backend:

```bash
set -a
. ./.env
set +a
export DB_URL=jdbc:postgresql://localhost:5432/suporte
cd backend
./mvnw spring-boot:run
```

Em outro terminal, inicie o frontend:

```bash
cd frontend
npm ci
npm run dev
```

Abra a URL informada pelo Vite (normalmente **http://127.0.0.1:5173**). O proxy de desenvolvimento encaminha `/api` ao backend em `localhost:8080`.

Na primeira inicialização com o banco vazio, as migrações criam o esquema e o backend provisiona o administrador inicial, um setor de TI e categorias padrão. Entre com as credenciais definidas no `.env` e troque a senha quando solicitado.

Para parar, use Ctrl+C nos terminais do frontend e backend. Os dados continuam no PostgreSQL local. Alterar `BOOTSTRAP_ADMIN_PASSWORD` não redefine uma conta existente.

## Verificações

```bash
cd frontend
npm run build
npm run lint
npm test
```

```bash
cd backend
./mvnw test
./mvnw package
```

Os comandos de teste ainda não cobrem os critérios funcionais: consulte o [checklist manual de permissões e atendimento](docs/07-validacao-manual.md).
Os testes PostgreSQL de migração, carregamento JPA e versão otimista são ativados na CI. Para executá-los localmente, configure um banco descartável e defina `RUN_POSTGRES_INTEGRATION=true` junto com as variáveis de conexão.

## Documentação

- [Referência da API](docs/06-api.md).
- [Requisitos](docs/01-requisitos.md).
- [Arquitetura e dados](docs/02-arquitetura-e-dados.md).
- [Guia do backend](docs/03-guia-backend.md).
- [Guia do frontend](docs/04-guia-frontend.md).
- [Roteiro de desenvolvimento](docs/05-roteiro-de-desenvolvimento.md).
- [Checklist manual](docs/07-validacao-manual.md).
- [Preparação para produção, HTTPS e backup](docs/08-producao.md).

Antes de produção, configure HTTPS e gestão segura de segredos, e defina e teste o processo de backup e restauração.
