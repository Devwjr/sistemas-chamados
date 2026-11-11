# Referência da API

Referência dos controllers e serviços atuais. Não há Swagger/OpenAPI configurado. As regras abaixo representam o comportamento previsto no código; ainda faltam testes automatizados. Os relacionamentos usados nos DTOs dos chamados são carregados nas consultas de detalhe e listagem.

## Endereço e formatos

- Backend iniciado localmente: `http://localhost:8080/api`.
- Corpos JSON, exceto login (`application/x-www-form-urlencoded`). Campos desconhecidos em JSON são recusados.
- IDs numéricos; enums em maiúsculas; datas e instantes serializados em ISO 8601.
- Autenticação por sessão (`JSESSIONID`), sem bearer token.
- Toda escrita exige CSRF, inclusive login e logout.

## Sessão e CSRF

| Método | Rota | Corpo / resposta |
|---|---|---|
| GET | `/auth/csrf` | Público. Retorna `token` e `headerName`. |
| POST | `/auth/login` | Público com CSRF. Formulário com `username` (e-mail) e `password`. Sucesso: `200`, `{"authenticated":true}`. |
| GET | `/auth/me` | Retorna o usuário autenticado. |
| PATCH | `/auth/senha` | `senhaAtual`, `novaSenha`; sucesso `204`. |
| POST | `/auth/logout` | Sem corpo; sucesso `204`, invalida sessão. |

Obtenha o token e preserve os cookies no mesmo cliente. Envie o token no cabeçalho indicado por `headerName`. Consulte `/auth/csrf` novamente após login e antes das próximas escritas; não reutilize cegamente o token anterior ao login.

Usuários inativos são recusados. Enquanto `trocaSenhaObrigatoria` for `true`, apenas `/auth/me`, `/auth/csrf`, `/auth/senha` e `/auth/logout` ficam disponíveis ao usuário autenticado. A senha nova deve ser diferente da atual, ter ao menos 12 caracteres e no máximo 72 bytes UTF-8. A troca mantém a sessão atual e revoga as demais por versão de autenticação. Login tem limite de 10 requisições por IP por minuto na instância; o proxy precisa ser considerado na implantação.

### Exemplo com curl

Execute em Bash ou Zsh. Requer `curl` e `jq`. O exemplo usa arquivo temporário de cookies e lê senhas sem mostrá-las no terminal. Use o e-mail da sua instalação.

```bash
API_BASE=http://localhost:8088/api
COOKIE_JAR=$(mktemp)
chmod 600 "$COOKIE_JAR"

csrf() {
  CSRF_JSON=$(curl --fail-with-body -sS -b "$COOKIE_JAR" -c "$COOKIE_JAR" "$API_BASE/auth/csrf") || return
  CSRF_HEADER=$(printf '%s' "$CSRF_JSON" | jq -r '.headerName')
  CSRF_TOKEN=$(printf '%s' "$CSRF_JSON" | jq -r '.token')
}

printf 'Senha de login: '
read -r -s LOGIN_PASSWORD
printf '\n'
csrf
printf '%s' "$LOGIN_PASSWORD" | curl --fail-with-body -sS \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  -H "$CSRF_HEADER: $CSRF_TOKEN" \
  --data-urlencode 'username=admin@empresa.local' \
  --data-urlencode 'password@-' "$API_BASE/auth/login"
unset LOGIN_PASSWORD

curl --fail-with-body -sS -b "$COOKIE_JAR" "$API_BASE/auth/me" | jq
```

Se houver troca obrigatória, execute antes de consultar chamados:

```bash
printf 'Senha atual: '
read -r -s CURRENT_PASSWORD
printf '\nNova senha: '
read -r -s NEW_PASSWORD
printf '\n'
csrf
printf '%s\n%s\n' "$CURRENT_PASSWORD" "$NEW_PASSWORD" \
  | jq -Rn '[inputs] | {senhaAtual: .[0], novaSenha: .[1]}' \
  | curl --fail-with-body -sS -X PATCH \
    -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
    -H "$CSRF_HEADER: $CSRF_TOKEN" -H 'Content-Type: application/json' \
    --data-binary @- "$API_BASE/auth/senha"
unset CURRENT_PASSWORD NEW_PASSWORD
```

Consulte categorias e use um ID ativo real na criação:

```bash
curl --fail-with-body -sS -b "$COOKIE_JAR" "$API_BASE/categorias?ativo=true" | jq

csrf
curl --fail-with-body -sS -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  -H "$CSRF_HEADER: $CSRF_TOKEN" -H 'Content-Type: application/json' \
  --data '{"titulo":"Não consigo acessar o e-mail","descricao":"O acesso ao e-mail apresenta erro desde esta manhã.","categoriaId":1,"impacto":"INDIVIDUAL"}' \
  "$API_BASE/chamados" | jq

curl --fail-with-body -sS -b "$COOKIE_JAR" \
  "$API_BASE/chamados?page=0&size=20&status=ABERTO" | jq

csrf
curl --fail-with-body -sS -X POST -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  -H "$CSRF_HEADER: $CSRF_TOKEN" "$API_BASE/auth/logout"
rm -f "$COOKIE_JAR"
unset CSRF_JSON CSRF_TOKEN CSRF_HEADER COOKIE_JAR
```

## Perfis

- `SOLICITANTE`: consulta e comenta apenas chamados próprios; comentários públicos.
- `TECNICO`: consulta fila geral, assume chamados e atende os que estão atribuídos a ele; acessa notas internas.
- `ADMIN`: consulta fila geral, administra cadastros, atribui responsáveis e executa ações de atendimento.

O backend deve verificar perfil e vínculo com o chamado; ocultar botões na interface não substitui essa verificação. Um chamado de outro solicitante é tratado como `404`.

## Paginação

Listagens retornam:

```json
{"items":[],"page":0,"size":20,"totalItems":0,"totalPages":0}
```

Parâmetros comuns: `page` começa em zero (máximo 100000); `size` vai de 1 a 100, padrão 20. Cadastros e responsáveis elegíveis são ordenados por `nome,asc`. Comentários e histórico são ordenados por `criadoEm,asc`. Esses endpoints não oferecem parâmetro para mudar a ordenação.

## Chamados

| Método | Rota | Entrada / retorno |
|---|---|---|
| GET | `/chamados` | Página de chamados visíveis, com filtros abaixo. |
| POST | `/chamados` | Criação; retorna chamado com `201` e cabeçalho `Location`. |
| GET | `/chamados/{id}` | Detalhe do chamado visível. |
| POST | `/chamados/{id}/assumir` | `{"versao":0}`; técnico/admin assume aberto sem responsável. |
| PATCH | `/chamados/{id}/responsavel` | `{"versao":0,"responsavelId":2}`; somente admin. |
| PATCH | `/chamados/{id}/classificacao` | `{"versao":0,"prioridade":"ALTA","categoriaId":1}`; admin ou técnico responsável. Ao menos um dos dois campos de classificação. |
| POST | `/chamados/{id}/transicoes` | Corpo descrito na seção de transições. |
| GET | `/chamados/{id}/comentarios` | Página de comentários; notas internas somente para TI. |
| POST | `/chamados/{id}/comentarios` | `{"versao":0,"conteudo":"Verifiquei o acesso.","visibilidade":"PUBLICO"}`; retorna o chamado atualizado. |
| GET | `/chamados/{id}/historico` | Página do histórico de eventos. |
| GET | `/painel` | Contagens limitadas aos chamados visíveis. |

Mutações de chamados retornam `200`, salvo criação (`201`). A versão dos exemplos é ilustrativa: envie sempre `versao` da resposta mais recente. Operações com versão desatualizada retornam `409`; releia o chamado e revise a ação antes de tentar novamente. Comentários também atualizam a versão. Chamados fechados/cancelados não aceitam alterações ou comentários. Não há exclusão nem edição de título, descrição ou impacto.

### Filtros da listagem

| Parâmetro | Valores / regra |
|---|---|
| `busca` | Até 150 caracteres. Busca parcial no título; também reconhece ID numérico ou protocolo como `CH-000001`. Não busca na descrição. |
| `status` | `ABERTO`, `EM_ATENDIMENTO`, `AGUARDANDO_USUARIO`, `RESOLVIDO`, `FECHADO`, `CANCELADO`. |
| `prioridade` | `BAIXA`, `MEDIA`, `ALTA`, `CRITICA`. |
| `categoriaId` | ID da categoria. |
| `responsavelId`, `setorId` | Somente técnico/admin; solicitante recebe `403` ao enviá-los. |
| `de`, `ate` | Datas `YYYY-MM-DD`, inclusivas no fuso `America/Sao_Paulo`; início não pode ser posterior ao fim. |
| `sort` | `campo,asc` ou `campo,desc`; campos `criadoEm`, `atualizadoEm`, `id`, `titulo`. Padrão `criadoEm,desc`. Desempate por ID ascendente. |

### Criação e resposta

Campos obrigatórios: `titulo` (5–150 caracteres), `descricao` (10–5000), `categoriaId` ativa e `impacto` (`INDIVIDUAL`, `SETOR`, `ORGANIZACAO`). O backend define autor, setor de origem, status `ABERTO`, prioridade `MEDIA` e datas.

`ChamadoView` contém: `id`, `protocolo`, `titulo`, `descricao`, `impacto`, `status`, `prioridade`, `solicitante`, `setorOrigem`, `responsavel`, `categoria`, `solucao`, `criadoEm`, `atualizadoEm`, `resolvidoEm`, `fechadoEm`, `canceladoEm`, `versao`. Referências seguem `{ "id": 1, "nome": "…" }`; responsável e datas de encerramento podem ser nulos.

### Transições

Envie `versao`, `destino` e apenas o complemento aplicável:

| Origem → destino | Quem / complemento |
|---|---|
| `ABERTO` → `EM_ATENDIMENTO` | Via assumir ou atribuir responsável; não pelo endpoint de transições. |
| `EM_ATENDIMENTO` → `AGUARDANDO_USUARIO` | Responsável/admin; `mensagemPublica` de 10–5000 caracteres. |
| `AGUARDANDO_USUARIO` → `EM_ATENDIMENTO` | Responsável/admin; sem complemento. |
| `EM_ATENDIMENTO` → `RESOLVIDO` | Responsável/admin; `solucao` de 10–5000 caracteres. |
| `RESOLVIDO` → `EM_ATENDIMENTO` | Solicitante dono/admin; `justificativa` de 10–5000; responsável precisa continuar ativo e com perfil de TI. |
| `RESOLVIDO` → `FECHADO` | Solicitante dono/admin; sem complemento. |
| `ABERTO` → `CANCELADO` | Solicitante dono/admin; `justificativa` de 10–5000. |
| `EM_ATENDIMENTO` ou `AGUARDANDO_USUARIO` → `CANCELADO` | Admin; `justificativa` de 10–5000. |

Exemplo de resolução: `{"versao":3,"destino":"RESOLVIDO","solucao":"A permissão de acesso foi corrigida e validada."}`. Outras transições são recusadas. Uma resposta pública não retoma o atendimento automaticamente.

Comentários usam `conteudo` de 1–5000 caracteres e `visibilidade` `PUBLICO` ou `INTERNO`; solicitante não cria notas internas. A resposta de comentário listado contém `id`, `autor`, `conteudo`, `visibilidade`, `criadoEm`.

Histórico contém `id`, `autor`, `tipoEvento`, `valorAnterior`, `valorNovo`, `justificativa`, `criadoEm`. Eventos: `ABERTURA`, `STATUS`, `RESPONSAVEL`, `PRIORIDADE`, `CATEGORIA`.

O painel retorna `porStatus`, `porPrioridade`, `semResponsavel` e `total`. `semResponsavel` conta apenas chamados abertos sem atribuição.

## Administração

| Método | Rota | Permissão / corpo |
|---|---|---|
| GET | `/categorias` | Autenticado; não-admin deve enviar `ativo=true`; admin pode omitir ou usar true/false. |
| POST | `/categorias` | Admin; cadastro completo. Retorna `201`. |
| PATCH | `/categorias/{id}` | Admin; cadastro completo. Retorna `200`. |
| GET | `/setores` | Admin; página de setores. |
| POST | `/setores` | Admin; cadastro completo. Retorna `201`. |
| PATCH | `/setores/{id}` | Admin; cadastro completo. Retorna `200`. |
| GET | `/usuarios` | Admin; página de usuários. |
| POST | `/usuarios` | Admin; usuário completo com senha temporária. Retorna `201`. |
| PATCH | `/usuarios/{id}` | Admin; usuário completo, sem senha temporária. Retorna `200`. |
| POST | `/usuarios/{id}/redefinir-senha` | Admin; `{"senhaTemporaria":"<senha temporária>"}`; retorna `204`. |
| GET | `/usuarios/elegiveis-atendimento` | Técnico/admin; página de referências de usuários ativos com perfil de TI. |

Apesar de usarem PATCH, os endpoints de cadastro exigem todos os campos obrigatórios; não implementam JSON Merge Patch.

Categoria/setor: `nome` (2–100), `ativo` obrigatório, `descricao` opcional até 500 caracteres. O serviço de setores ignora `descricao` e retorna esse campo nulo. Desativação usa PATCH com `ativo:false`; não há exclusão física.

Usuário: `nome` (2–120), `email` válido até 200, `perfil`, `setorId`, `ativo`. Na criação, inclua `senhaTemporaria` com a mesma política de comprimento da troca de senha. E-mail é normalizado para minúsculas e deve ser único. Um novo usuário precisa de setor ativo. O retorno contém `id`, `nome`, `email`, `perfil`, `setor`, `ativo`, `trocaSenhaObrigatoria`, sem senha ou hash.

O administrador não pode desativar a própria conta nem remover seu próprio perfil administrativo. Para desativar/remover perfil de TI de um usuário, redistribua seus chamados em atendimento ou aguardando usuário. Redefinir senha exige uma conta diferente da própria; força troca no próximo acesso e revoga sessões existentes. Para a própria conta use `/auth/senha`.

## Erros

O formato padrão usa `detail`, não `message`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Escolha uma senha diferente da atual.",
  "instance": "/api/auth/senha",
  "code": "INVALID_INPUT",
  "fieldErrors": []
}
```

`code` no exemplo deve ser interpretado conforme a resposta efetiva. Erros de validação usam `VALIDATION_ERROR`, `detail` genérico e `fieldErrors` com `field`/`message`.

| HTTP | Situação |
|---|---|
| 400 | Campos, enum, JSON, paginação ou senha inválidos. |
| 401 | Sem autenticação, credenciais incorretas ou sessão revogada. |
| 403 | Perfil sem permissão, CSRF inválido ou troca de senha obrigatória. |
| 404 | Registro inexistente ou chamado invisível ao solicitante. |
| 409 | Versão desatualizada, transição inválida ou conflito de cadastro. |
| 429 | Limite de login; aguarde o período indicado por `Retry-After` quando presente. |
| 500 | Falha inesperada; `detail` contém referência para diagnóstico nos logs. |

## Health check

`GET /actuator/health` é público no backend, fora do prefixo `/api`. Com o backend iniciado localmente:

```bash
curl http://localhost:8080/actuator/health
```
