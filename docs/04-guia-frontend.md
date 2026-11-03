# Guia do frontend com React e TypeScript

## 1. Preparação

Criar a aplicação em `frontend/` usando o template React com TypeScript do Vite. Escolher Node compatível com a versão adotada e registrar essa versão no projeto. Habilitar modo estrito do TypeScript e versionar o arquivo de lock.

Scripts esperados: `dev`, `build`, `lint` e `test`. Eles só estarão disponíveis depois da criação da aplicação.

Usar chamadas relativas a `/api` e configurar proxy no desenvolvimento. Variáveis incorporadas ao frontend são públicas: nunca colocar senha do banco, chave privada ou outro segredo nelas.

## 2. Telas e rotas

| Rota | Tela | Conteúdo |
|---|---|---|
| `/login` | Acesso | E-mail, senha e mensagens de erro |
| `/alterar-senha` | Troca de senha | Senha atual, nova e confirmação |
| `/` | Painel | Contadores e acesso à lista |
| `/chamados` | Lista | Filtros, paginação e indicadores |
| `/chamados/novo` | Abertura | Título, descrição, categoria e impacto |
| `/chamados/:id` | Detalhe | Dados, ações permitidas, comentários e histórico |
| `/admin/usuarios` | Usuários | Cadastro, edição, desativação e senha temporária |
| `/admin/setores` | Setores | Cadastro, edição e desativação |
| `/admin/categorias` | Categorias | Cadastro, edição e desativação |

Após recarregar a página, consultar `/api/auth/me` antes de liberar rotas protegidas. Se houver troca obrigatória, encaminhar à tela correspondente. As guardas de rota melhoram a navegação; a autorização efetiva continua no backend.

## 3. Estrutura e responsabilidades

```text
src/
├── app/
│   ├── router.tsx
│   └── providers.tsx
├── components/
│   ├── Button.tsx
│   ├── FormField.tsx
│   ├── Pagination.tsx
│   └── Feedback.tsx
├── features/
│   ├── auth/
│   │   ├── AuthProvider.tsx
│   │   ├── LoginPage.tsx
│   │   └── authService.ts
│   ├── chamados/
│   │   ├── pages/
│   │   ├── components/
│   │   ├── chamadoService.ts
│   │   └── types.ts
│   └── administracao/
├── services/api.ts
└── types/api.ts
```

- Página: coordena carregamento, ações e navegação.
- Componente: apresenta uma parte da tela e recebe dados e callbacks.
- Service: conhece endpoints, serialização e tipos de resposta.
- Cliente HTTP: centraliza credenciais, CSRF, status HTTP e formato de erros.
- Contexto de autenticação: mantém usuário e estado da sessão; não deve virar um depósito de todos os dados da aplicação.

## 4. Contratos TypeScript

Modelar os tipos de acordo com os DTOs da API:

```typescript
export type StatusChamado =
  | 'ABERTO'
  | 'EM_ATENDIMENTO'
  | 'AGUARDANDO_USUARIO'
  | 'RESOLVIDO'
  | 'FECHADO'
  | 'CANCELADO';

export type Prioridade = 'BAIXA' | 'MEDIA' | 'ALTA' | 'CRITICA';

export interface CriarChamadoRequest {
  titulo: string;
  descricao: string;
  categoriaId: number;
  impacto: 'INDIVIDUAL' | 'SETOR' | 'ORGANIZACAO';
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}
```

Definir separadamente os tipos de resumo, detalhe, comentário e erro. Não usar `any` para contornar divergências. TypeScript não valida JSON em runtime: manter o contrato consistente e considerar validação de resposta quando houver integração menos controlada.

## 5. Integração e sessão

Centralizar `fetch` com credenciais e tratamento de resposta. Enviar `Content-Type: application/json` somente para corpos JSON; login usa formulário. Tratar `204 No Content` sem tentar desserializar JSON.

Antes do login, buscar o token CSRF. Enviá-lo no header indicado pela API em `POST`, `PATCH`, `PUT` e `DELETE`, inclusive logout. Buscar um token atualizado após autenticação e encerramento de sessão. O cookie de sessão HttpOnly é administrado pelo navegador; não copiar seu conteúdo para `localStorage`.

Ao receber `401`, limpar o usuário da interface e encaminhar ao login, preservando uma URL interna segura para retorno. Em `403`, distinguir falta de permissão, troca obrigatória e CSRF inválido por código de erro. Não repetir automaticamente mutações depois de erro de rede ou autenticação.

## 6. Fluxos principais

### Abrir chamado

Carregar categorias ativas, validar limites, enviar o formulário e navegar para o detalhe criado. Desabilitar o botão enquanto o envio estiver em andamento, preservando o conteúdo em caso de falha. A validação do frontend ajuda o usuário; o backend valida novamente.

### Consultar fila

Manter filtros e página na URL. Ao mudar um filtro, voltar à primeira página. Usar paginação do servidor e cancelar ou ignorar respostas antigas de buscas para que uma resposta lenta não sobrescreva uma busca nova.

### Atender chamado

Mostrar ações conforme perfil, responsabilidade e estado. Exibir prioridade e status com texto, além de cor. Para resolver, solicitar solução; para aguardar usuário, solicitar mensagem pública; para reabrir ou cancelar, solicitar justificativa.

Enviar a versão exibida no detalhe em cada mutação. Se a API responder `409`, informar que o chamado mudou, recarregar os dados e preservar o texto digitado para revisão. Não sobrescrever silenciosamente a alteração de outra pessoa.

### Comentários e histórico

Separar visualmente mensagem pública de nota interna, identificando a visibilidade antes do envio. Solicitantes não recebem opção de nota interna. Renderizar conteúdo como texto; não usar HTML arbitrário vindo de comentários.

Atualizar detalhe, listas e painel afetados após uma ação bem-sucedida. Se adotar biblioteca de cache de consultas, centralizar chaves e invalidação. Para o MVP, atualizações após ação e botão de recarregar são suficientes; não é necessário WebSocket.

## 7. Experiência e acessibilidade

- Estados distintos de carregamento, erro, lista vazia e falta de permissão.
- Labels associados aos campos e erros próximos ao campo correspondente.
- Foco visível, navegação por teclado e foco controlado em diálogos.
- Mensagens de sucesso e erro anunciáveis por tecnologia assistiva.
- Listas adaptadas ao celular e ações utilizáveis sem hover.
- Datas formatadas em português e no fuso definido, preservando UTC no contrato.
- Confirmação contextual antes de cancelar chamado, desativar usuário ou redefinir senha.

## 8. Verificação

Executar checagem de tipos, lint e build. Testar formulários e mensagens com uma ferramenta como React Testing Library. Cobrir o fluxo completo com uma ferramenta como Playwright: login, abertura por solicitante, atendimento por técnico, resolução e fechamento pelo solicitante.

Incluir um teste no qual o solicitante tenta abrir uma URL de chamado alheio e outro no qual uma nota interna existe, mas não aparece para ele. Validar também a resposta da API; apenas conferir ausência visual não comprova a proteção.
