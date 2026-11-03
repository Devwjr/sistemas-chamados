# Guia de desenvolvimento do backend com Spring

## 1. Preparar o projeto

Gerar um projeto Maven no Spring Initializr com Java e dependências Spring Web, Spring Security, Spring Data JPA, Validation, PostgreSQL Driver e Flyway. Acrescentar os módulos de suporte ao PostgreSQL exigidos pela versão escolhida do Flyway. Usar Spring Boot Test e Spring Security Test nos testes.

Configurar a conexão por variáveis de ambiente, como `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. Documentar os nomes em um arquivo de exemplo sem valores reais. Separar configuração local, de testes e de produção.

Comandos previstos após criar o projeto e seu wrapper:

```bash
cd backend
./mvnw spring-boot:run
./mvnw verify
```

Esses comandos ainda não podem ser executados neste repositório, pois o código será criado durante o desenvolvimento.

## 2. O que cada parte faz

| Parte | Responsabilidade | Exemplo |
|---|---|---|
| Entity | Representar dados persistidos e seus relacionamentos | `Chamado` com `@Entity` e `@Version` |
| DTO de entrada | Definir os campos aceitos e validar sua forma | `CriarChamadoRequest` |
| DTO de saída | Expor apenas dados autorizados e úteis | `ChamadoDetalheResponse` |
| Controller | Mapear HTTP, validar entrada e delegar | `POST /api/chamados` |
| Service | Executar regras, permissões e transações | Assumir chamado e registrar histórico |
| Repository | Buscar e persistir dados | Buscar chamados visíveis com paginação |
| Mapper | Converter entidades e DTOs explicitamente | Omitir senha e notas internas |
| Security | Identificar usuário e proteger operações | Validar sessão e perfil |
| Exception handler | Converter falhas em respostas estáveis | Conflito de versão vira HTTP 409 |
| Migration | Evoluir o esquema do banco | Criar tabela de comentários |
| Test | Comprovar comportamento e impedir regressões | Solicitante não lê chamado alheio |

Fluxo de uma requisição: filtros de segurança → controller → service → repository → banco. A resposta volta por um DTO. Não retornar entidades JPA diretamente e não receber uma entidade inteira como corpo de edição.

## 3. Implementar cadastros e persistência

Começar por setor, usuário e categoria; depois criar chamado, comentário e histórico.

- Usar relacionamentos com carregamento controlado, geralmente `LAZY`.
- Evitar percorrer coleções inteiras de comentários ao listar chamados.
- Usar projeções, consultas específicas ou carregamento planejado para evitar uma consulta adicional por linha.
- Fazer mapeamento de saída dentro da transação quando depender de relações lazy.
- Consultar comentários e histórico por endpoints paginados.
- Persistir enums com `EnumType.STRING`.
- Validar unicidade no banco mesmo que já exista validação no serviço.

## 4. Segurança e autenticação

Para o MVP, usar a sessão do Spring Security. O navegador guarda o identificador em cookie HttpOnly. Não implementar autenticação por JWT apenas para substituir uma sessão simples.

Fluxo proposto:

1. `GET /api/auth/csrf` entrega o token CSRF e o nome do header esperado.
2. O frontend envia o token em `POST /api/auth/login`, com campos `username` e `password` em formato de formulário. `username` contém o e-mail.
3. O filtro de autenticação do Spring Security verifica senha e estado do usuário, aplica proteção contra fixação de sessão e retorna sucesso JSON ou erro JSON.
4. `GET /api/auth/me` retorna os dados públicos da sessão.
5. Após login e logout, o frontend busca um novo token CSRF, pois o token anterior pode ser invalidado.
6. `POST /api/auth/logout` invalida a sessão e remove seu cookie.

Configurar explicitamente os handlers e URLs para evitar redirecionamentos HTML nas chamadas da API. Login e logout também exigem proteção CSRF. Desabilitar métodos alternativos de autenticação que não serão usados.

Usar `PasswordEncoder` com algoritmo adaptativo, como BCrypt. Configurar limite de tentativas de login e expiração de sessão. Em produção, cookie com `HttpOnly`, `Secure` e `SameSite` adequado à implantação; em localhost HTTP, o perfil local precisa permitir o cookie sem `Secure`.

Verificar o usuário ativo em cada requisição autenticada e revogar sessões ao desativá-lo ou trocar seu perfil. Na troca ou redefinição de senha, revogar as demais sessões; a redefinição administrativa revoga todas. Para uma instância, um registro de sessões controlado pode atender; para múltiplas instâncias, usar armazenamento de sessões compartilhado.

Usuários com troca obrigatória de senha só acessam `/auth/me`, `/auth/csrf`, `/auth/senha` e logout. As demais operações devem ser bloqueadas até concluir a troca.

Permitir acesso anônimo somente aos endpoints necessários ao login e token CSRF. Aplicar autorização por perfil nas operações e autorização por recurso no serviço. Carregar o usuário pelo contexto autenticado, nunca por `solicitanteId` enviado pelo cliente.

## 5. Contratos da API

Prefixo: `/api`. IDs numéricos internos e protocolo público como texto. Todas as datas de saída seguem ISO 8601 com fuso UTC. Documentar contratos em OpenAPI e restringir sua exposição em produção conforme a política interna.

### Autenticação

| Método | Rota | Uso |
|---|---|---|
| GET | `/auth/csrf` | Obter token para requisições mutáveis |
| POST | `/auth/login` | Entrar usando formulário |
| GET | `/auth/me` | Consultar usuário autenticado |
| POST | `/auth/logout` | Encerrar sessão |
| PATCH | `/auth/senha` | Trocar senha atual |

### Chamados

| Método | Rota | Uso |
|---|---|---|
| POST | `/chamados` | Criar chamado |
| GET | `/chamados` | Listar registros visíveis com filtros |
| GET | `/chamados/{id}` | Consultar detalhe autorizado |
| POST | `/chamados/{id}/assumir` | Assumir e iniciar atendimento |
| PATCH | `/chamados/{id}/responsavel` | Administrador atribui ou reatribui |
| PATCH | `/chamados/{id}/classificacao` | Alterar prioridade e/ou categoria |
| POST | `/chamados/{id}/transicoes` | Executar mudança válida de status |
| GET | `/chamados/{id}/comentarios` | Listar comentários visíveis |
| POST | `/chamados/{id}/comentarios` | Criar comentário |
| GET | `/chamados/{id}/historico` | Listar histórico |
| GET | `/painel` | Consultar totais conforme o perfil |

A primeira atribuição administrativa também leva `ABERTO` para `EM_ATENDIMENTO`. Na rota de transições, aceitar apenas as mudanças da tabela de requisitos; a mudança inicial depende das rotas de atribuição, para que responsável e status sejam definidos juntos.

Exemplo de abertura:

```json
{
  "titulo": "Computador não conecta à rede",
  "descricao": "Desde o início do expediente, o computador não acessa os sistemas internos.",
  "categoriaId": 1,
  "impacto": "INDIVIDUAL"
}
```

Retornar `201 Created`, header `Location` e o detalhe criado, incluindo `id`, `protocolo`, `status`, `prioridade` e `versao`.

Exemplo de resolução:

```json
{
  "versao": 3,
  "destino": "RESOLVIDO",
  "solucao": "Configuração de rede corrigida e acesso validado com o solicitante."
}
```

Para aguardar usuário, enviar `mensagemPublica`; para cancelamento ou reabertura, enviar `justificativa`. O serviço valida o campo obrigatório para cada destino e rejeita combinações sem sentido. Não aceitar autor ou data enviados pelo cliente.

Todas as mutações de chamados existentes recebem `versao`, inclusive inclusão de comentário, e retornam a versão atualizada. Ao adicionar comentário, atualizar também o chamado para que seu controle de versão participe da transação. Isso impede aceitar comentário concorrente com um fechamento já efetivado.

Exemplo de listagem: `GET /api/chamados?status=ABERTO&page=0&size=20&sort=criadoEm,desc`. Aceitar apenas campos de ordenação explicitamente permitidos e aplicar filtros de visibilidade antes da paginação e contagem.

Contrato estável de página, sem expor diretamente a serialização interna do Spring:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

### Administração e opções de formulário

| Método | Rota | Acesso e uso |
|---|---|---|
| GET | `/categorias?ativo=true` | Autenticados consultam opções; administração pode incluir inativas |
| POST/PATCH | `/categorias` e `/categorias/{id}` | Administrador cria, edita ou desativa |
| GET/POST/PATCH | `/setores` e `/setores/{id}` | Administrador lista, cria, edita ou desativa |
| GET/POST/PATCH | `/usuarios` e `/usuarios/{id}` | Administrador lista, cria, edita ou desativa |
| POST | `/usuarios/{id}/redefinir-senha` | Administrador define senha temporária e exige troca |
| GET | `/usuarios/elegiveis-atendimento` | TI consulta IDs e nomes de responsáveis ativos |

Listagens de administração e opções de usuários também devem ser paginadas. `POST` opera sobre a coleção e `PATCH` sobre um ID. Não disponibilizar exclusão física no MVP. DTOs de consulta de usuários nunca incluem senha ou hash.

## 6. Serviços, transações e concorrência

Cada comando deve executar em método transacional: carregar chamado, verificar visibilidade e permissão, conferir versão e estado, aplicar alteração e gravar histórico.

Usar `@Version` para detectar alterações concorrentes. Além da verificação da versão informada pelo cliente, o update do JPA deve conferir a versão no banco. Converter falha de bloqueio otimista em `409 Conflict` e orientar a interface a recarregar.

Na operação de assumir, verificar que o chamado está aberto e sem responsável dentro da mesma transação. Em duas tentativas simultâneas, apenas uma pode confirmar; a outra recebe conflito. Não fazer uma consulta isolada seguida de atualização sem proteção.

Para registrar auditoria, usar o autor autenticado e um relógio do servidor, preferencialmente `Clock` injetável para facilitar os testes.

## 7. Erros e validação

Criar um `@RestControllerAdvice` e tratar também erros nos handlers de segurança, pois eles podem acontecer antes do controller. Usar um formato consistente inspirado em Problem Details:

```json
{
  "type": "about:blank",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Revise os campos informados.",
  "instance": "/api/chamados",
  "code": "VALIDATION_ERROR",
  "fieldErrors": [
    { "field": "titulo", "message": "Informe entre 5 e 150 caracteres." }
  ]
}
```

- `400`: JSON inválido, campos inválidos ou parâmetros malformados.
- `401`: autenticação ausente ou inválida.
- `403`: operação proibida para o perfil ou token CSRF inválido.
- `404`: recurso inexistente ou fora da visibilidade do usuário.
- `409`: versão desatualizada, transição incompatível com o estado ou unicidade violada.
- `500`: falha inesperada, sem stack trace ou SQL na resposta.

Usar códigos de erro estáveis para a interface distinguir, por exemplo, sessão expirada, troca obrigatória de senha e conflito de versão.

## 8. Testes necessários

Priorizar comportamentos com impacto real:

1. Testes de serviço para cada transição permitida e recusada.
2. Testes de segurança para acesso anônimo, perfis e chamado de outro solicitante.
3. Testes que comprovem ausência de notas internas e hashes nas respostas.
4. Integração com PostgreSQL para migrações, filtros, paginação e restrições; usar Testcontainers se houver runtime de containers disponível.
5. Teste concorrente de dois técnicos assumindo o mesmo chamado.
6. Fluxo de sessão: CSRF, login, troca obrigatória, logout e revogação de usuário inativo.
7. Rollback: se o histórico falhar, a mudança do chamado não pode ficar salva sozinha.

Não depender apenas de H2 para verificar comportamento específico do PostgreSQL. Se containers não estiverem disponíveis, preparar um banco PostgreSQL isolado para testes.

## 9. Operação

Adicionar health check com exposição mínima, logs com identificador de requisição, migrações automatizadas e segredos externos ao código. Definir timeout de sessão, limite de tentativas, backup e restauração antes da produção. Para múltiplas instâncias, compartilhar sessões e coordenar a execução de migrações.
