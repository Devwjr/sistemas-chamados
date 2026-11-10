# Núcleo · Central de suporte TI

Aplicação interna para abrir e acompanhar chamados de TI. O repositório contém backend Spring Boot, frontend React/TypeScript e instalação local com Docker Compose.

## Estado atual

- Interface: login, troca obrigatória de senha, indicadores, busca, filtro de status, listagem, abertura, detalhe, comentários, histórico e ações de atendimento. Administradores também gerenciam usuários, setores e categorias.
- API: usuários, setores, categorias, atribuição, classificação, transições, comentários públicos, notas internas e histórico.
- A implementação ainda precisa de testes automatizados e validação ponta a ponta das permissões e do atendimento. A interface ainda não expõe todos os filtros avançados da API nem a troca de categoria no detalhe do chamado.

## Instalação com Docker

Requer Git, Docker Engine em execução e o plugin Docker Compose. Não é necessário instalar Java ou Node no computador para este modo.

```bash
git clone git@github.com:Devwjr/sistemas-chamados.git
cd sistemas-chamados
```

Execute os próximos comandos na pasta que contém `compose.yaml`. Se já tiver clonado o projeto, entre nessa pasta em vez de cloná-lo novamente.

### 1. Configurar o ambiente

Em uma instalação nova:

```bash
cp .env.example .env
chmod 600 .env
nano .env
```

Se o `.env` já existir, edite-o sem sobrescrevê-lo. Preencha `DB_PASSWORD` e `BOOTSTRAP_ADMIN_PASSWORD` com senhas próprias. A senha inicial do administrador deve ter pelo menos 12 caracteres e no máximo 72 bytes UTF-8.

| Variável no `.env` | Uso |
|---|---|
| `DB_USERNAME` | Usuário PostgreSQL; padrão `suporte`. |
| `DB_PASSWORD` | Senha obrigatória do banco, compartilhada por db e backend. |
| `BOOTSTRAP_ADMIN_EMAIL` | E-mail inicial; padrão do Compose `admin@empresa.local`. |
| `BOOTSTRAP_ADMIN_PASSWORD` | Senha inicial obrigatória do administrador. |
| `COOKIE_SECURE` | `false` para HTTP local; `true` exige HTTPS. |

O `.env` é ignorado pelo Git. O Compose valida as variáveis obrigatórias mesmo ao iniciar apenas `db`.

### 2. Iniciar os serviços

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=50 backend
```

Abra **http://localhost:8088**. O Nginx serve o frontend e encaminha `/api` ao backend. O backend não publica a porta 8080 no host; o PostgreSQL está disponível em `127.0.0.1:5432` e usa o banco `suporte`.

As migrações Flyway são executadas na inicialização. Na primeira execução com a tabela de usuários vazia, o backend cria o administrador, um setor de TI e cinco categorias.

### 3. Primeiro acesso

Entre com o e-mail configurado e a senha inicial do `.env`. A tela exige uma nova senha diferente da atual. Use ao menos 12 caracteres, respeitando o máximo de 72 bytes UTF-8; caracteres acentuados podem ocupar mais de um byte.

Depois da troca, entre usando a nova senha. Alterar `BOOTSTRAP_ADMIN_PASSWORD` no `.env` não redefine uma conta existente. Alterar `DB_PASSWORD` também não troca a senha de um PostgreSQL já inicializado no volume.

### Operação local

```bash
# Acompanhar logs; Ctrl+C sai dos logs e mantém os serviços ativos
docker compose logs -f backend

# Reconstruir a interface após mudanças
docker compose up -d --build --no-deps frontend

# Reconstruir o backend após mudanças
docker compose up -d --build backend

# Parar os serviços mantendo os dados
docker compose down
```

Os dados ficam no volume `postgres_data`. `docker compose down -v` remove o volume e apaga os dados; não use esse comando para resolver problemas de login.

### Problemas comuns

| Mensagem ou sintoma | Como resolver |
|---|---|
| `no configuration file provided` | Entre na pasta que contém `compose.yaml`. |
| Variável obrigatória sem valor | Preencha as duas senhas no `.env`. |
| `permission denied` no socket Docker | Se seu ambiente exige sudo, use `sudo docker compose …` em cada comando, na mesma linha. |
| E-mail ou senha inválidos | Use a senha inicial apenas até concluir a troca; depois use a nova senha. |
| Escolha uma senha diferente da atual | Apague o campo da nova senha e digite outra; confira o preenchimento automático. |
| Layout antigo após reconstrução | Recarregue com Ctrl+Shift+R. |
| API indisponível logo após subir | Consulte os logs do backend e aguarde a inicialização. |

## Desenvolvimento fora dos containers

Requer JDK 21, Node.js 22.12 ou superior, npm e PostgreSQL 17. O script `backend/mvnw` baixa Maven 3.9.11 na primeira execução e requer `curl`, `tar` e `sha512sum`.

Inicie o banco com o Compose:

```bash
docker compose up -d db
```

Em um terminal Bash ou Zsh na raiz do projeto, exporte os valores do `.env` (o arquivo precisa usar sintaxe compatível com o shell):

```bash
set -a
. ./.env
set +a
export DB_URL=jdbc:postgresql://localhost:5432/suporte
cd backend
./mvnw spring-boot:run
```

Use senhas sem metacaracteres de shell nesse modo, ou exporte as variáveis individualmente com as aspas apropriadas. O Compose lê o `.env` automaticamente; o Spring iniciado diretamente não.

Em outro terminal:

```bash
cd frontend
npm ci
npm run dev
```

Abra a URL informada pelo Vite (normalmente http://127.0.0.1:5173). Seu proxy encaminha `/api` para `localhost:8080`. Evite iniciar o backend local e o backend do Compose simultaneamente contra o mesmo banco durante o desenvolvimento.

### Verificações

```bash
cd frontend
npm run build
npm run lint
```

```bash
cd backend
./mvnw test
./mvnw package
```

Ainda não existem testes automatizados implementados. `mvnw test` com sucesso não comprova a validação funcional. O build Docker do backend usa `-DskipTests`.

## API e documentos

- [Referência da API e exemplos de autenticação](docs/06-api.md).
- [Requisitos](docs/01-requisitos.md).
- [Arquitetura e dados](docs/02-arquitetura-e-dados.md).
- [Guia do backend](docs/03-guia-backend.md).
- [Guia do frontend](docs/04-guia-frontend.md).
- [Roteiro de desenvolvimento](docs/05-roteiro-de-desenvolvimento.md).
- [Checklist manual de permissões e atendimento](docs/07-validacao-manual.md).

Os requisitos e guias descrevem o objetivo do projeto; a referência da API descreve o código atual. A configuração Compose é voltada ao ambiente local. Produção ainda requer HTTPS, gestão de segredos, backup com restauração validada e revisão de segurança.
