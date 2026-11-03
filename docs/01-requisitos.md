# Documento de requisitos

## 1. Objetivo

Centralizar as solicitações de suporte de TI de uma organização, permitindo registrar problemas, acompanhar atendimentos, distribuir trabalho entre técnicos e consultar o histórico.

O sistema será interno, para uma única organização, com funcionários vinculados a setores. Uma pessoa sem autenticação não poderá consultar chamados.

## 2. Premissas para validar

- O administrador cadastra os usuários; não haverá cadastro público no MVP.
- Cada usuário pertence a um setor e tem um perfil principal.
- Técnicos podem consultar a fila de todos os setores; solicitantes consultam apenas seus próprios chamados.
- O administrador também pode executar as ações de um técnico.
- A prioridade é definida pela TI. O solicitante informa o impacto no trabalho.
- O fuso de apresentação inicial será America/Sao_Paulo; os instantes são persistidos em UTC.
- Não haverá exclusão física de chamados pela interface.

Antes de usar em produção, validar com os responsáveis: quem pode ver chamados sensíveis, política de retenção, prazo para reabertura, necessidade de SSO e critérios de prioridade. O modelo inicial de visibilidade ampla entre técnicos precisa dessa validação.

## 3. Perfis e permissões

| Ação | Solicitante | Técnico | Administrador |
|---|---|---|---|
| Abrir chamado | Sim | Sim | Sim |
| Consultar chamado próprio | Sim | Sim | Sim |
| Consultar fila geral | Não | Sim | Sim |
| Comentar publicamente | Em chamado próprio | Sim | Sim |
| Criar e ler nota interna | Não | Sim | Sim |
| Assumir chamado sem responsável | Não | Sim | Sim |
| Atribuir ou trocar responsável | Não | Não | Sim |
| Definir categoria e prioridade | Não | Se responsável | Sim |
| Alterar status de atendimento | Não | Se responsável | Sim |
| Cancelar chamado | Próprio, nas condições previstas | Não | Nas condições previstas |
| Fechar ou reabrir após resolução | Próprio | Não | Sim |
| Gerenciar usuários, setores e categorias | Não | Não | Sim |

As permissões devem ser verificadas no backend para cada recurso e operação, inclusive quando alguém fornece um ID diretamente na URL.

## 4. Escopo do MVP

### RF01 — Autenticação

O usuário entra com e-mail e senha e pode sair da sessão. Credenciais inválidas retornam mensagem genérica. Usuários inativos não podem iniciar sessão nem continuar usando sessões existentes.

**Aceite:** login válido permite consultar o usuário autenticado; login inválido não revela se o e-mail existe; logout invalida a sessão.

### RF02 — Administração de cadastros

O administrador cria, consulta, atualiza e desativa usuários, setores e categorias. E-mail deve ser único, sem diferenciar maiúsculas e minúsculas. Usuários devem trocar a senha temporária no primeiro acesso.

**Aceite:** um solicitante não acessa operações administrativas; desativar um cadastro preserva seu histórico e impede novas utilizações. Usuário com chamados ativos atribuídos precisa ter esses chamados redistribuídos antes de ser desativado.

### RF03 — Abertura de chamado

Campos: título, descrição, categoria e impacto. O backend define solicitante autenticado, setor de origem, prioridade inicial `MEDIA`, status `ABERTO`, protocolo e datas.

**Aceite:** título de 5 a 150 caracteres; descrição de 10 a 5.000; categoria ativa obrigatória; impacto entre `INDIVIDUAL`, `SETOR` e `ORGANIZACAO`. O cliente não escolhe o autor ou o status inicial.

### RF04 — Listagem e busca

Listagem paginada com filtros por protocolo/título, status, prioridade, categoria e período de abertura. Técnicos e administradores também filtram por responsável e setor.

**Aceite:** o solicitante recebe apenas seus registros, inclusive em buscas e totais; a ordenação padrão é abertura mais recente; o tamanho de página tem limite de 100 registros.

### RF05 — Detalhe e histórico

Exibir dados do chamado, responsável, comentários e histórico de alterações de status, atribuição, categoria e prioridade.

**Aceite:** cada evento informa autor, data e alteração. Solicitantes não recebem notas internas nem dados derivados delas em respostas da API.

### RF06 — Distribuição e triagem

Um técnico ativo pode assumir um chamado aberto sem responsável. O administrador pode atribuir ou reatribuir a um técnico ou administrador ativo. A TI classifica a prioridade em `BAIXA`, `MEDIA`, `ALTA` ou `CRITICA`.

**Aceite:** assumir atribui o técnico e inicia o atendimento na mesma transação; duas tentativas simultâneas não geram dois responsáveis. Apenas um responsável pode estar atribuído por vez.

### RF07 — Atendimento e encerramento

O responsável registra o andamento e a solução. O solicitante confirma o encerramento ou solicita a reabertura após a resolução.

**Aceite:** resolver exige resumo da solução de 10 a 5.000 caracteres; reabrir exige justificativa de 10 a 5.000 caracteres. Todas as transições seguem a tabela da seção 5.

### RF08 — Comentários

Permitir mensagens públicas e notas internas. O solicitante só cria mensagens públicas. Não haverá edição nem exclusão de comentários no MVP.

**Aceite:** mensagens de 1 a 5.000 caracteres; notas internas aparecem apenas para a TI; chamados fechados ou cancelados não aceitam novas mensagens.

### RF09 — Painel operacional

Exibir contagem por status, por prioridade e de chamados sem responsável. Para o solicitante, considerar apenas chamados próprios; para a TI, considerar a fila geral.

**Aceite:** os números respeitam as mesmas regras de visibilidade da listagem. Não incluir métricas de SLA no MVP.

### RF10 — Troca de senha

Permitir trocar a própria senha mediante senha atual, invalidando as demais sessões. No primeiro acesso, permitir apenas consultar a sessão, trocar a senha e sair até concluir a troca obrigatória.

**Aceite:** senhas não aparecem em respostas ou logs; a nova senha segue a política configurada; após a troca, a senha antiga deixa de funcionar. Recuperação autônoma por e-mail fica para uma etapa posterior; inicialmente o administrador pode emitir uma senha temporária, invalidando as sessões existentes.

## 5. Ciclo de vida do chamado

| Origem | Destino | Quem pode executar | Condição |
|---|---|---|---|
| ABERTO | EM_ATENDIMENTO | Técnico que assume ou administrador | Atribuir responsável ativo |
| EM_ATENDIMENTO | AGUARDANDO_USUARIO | Responsável ou administrador | Informar o que falta em comentário público |
| AGUARDANDO_USUARIO | EM_ATENDIMENTO | Responsável ou administrador | Retomar explicitamente o atendimento |
| EM_ATENDIMENTO | RESOLVIDO | Responsável ou administrador | Registrar solução |
| RESOLVIDO | FECHADO | Solicitante do chamado ou administrador | Confirmar conclusão |
| RESOLVIDO | EM_ATENDIMENTO | Solicitante do chamado ou administrador | Justificar reabertura; manter responsável ativo |
| ABERTO | CANCELADO | Solicitante do chamado ou administrador | Informar motivo |
| EM_ATENDIMENTO ou AGUARDANDO_USUARIO | CANCELADO | Administrador | Informar motivo |

Uma resposta do solicitante não altera o status automaticamente. Não são permitidas outras transições. `FECHADO` e `CANCELADO` são terminais no MVP. Se o responsável tiver sido desativado após a resolução, o administrador deve atribuir alguém ativo antes da reabertura.

## 6. Regras de negócio complementares

- Protocolo único e imutável no formato `CH-000001`, gerado a partir de uma sequência do banco. Lacunas na numeração são aceitáveis.
- O setor de origem é uma fotografia do setor do solicitante na abertura; mudar o setor do usuário não altera chamados existentes.
- Trocar responsável não altera o status, exceto na primeira atribuição de um chamado aberto, que inicia o atendimento.
- Cadastros inativos permanecem visíveis no histórico, mas não podem ser escolhidos em novos registros.
- Título, descrição e impacto ficam imutáveis após a abertura no MVP; complementos entram como comentários.
- Alterações relevantes e seus eventos de histórico devem ser gravados na mesma transação.
- Prioridade representa a classificação da TI; impacto informado não determina prioridade automaticamente.

## 7. Requisitos não funcionais

| ID | Requisito | Verificação |
|---|---|---|
| RNF01 | Proteger rotas por autenticação, perfil e propriedade | Testes com usuários e chamados distintos |
| RNF02 | Armazenar somente hash de senha usando encoder apropriado | Inspeção do cadastro e testes de autenticação |
| RNF03 | Usar HTTPS em produção, cookie HttpOnly e Secure, proteção CSRF | Verificação da configuração do ambiente |
| RNF04 | Validar entradas e retornar erros padronizados | Testes de campos inválidos e conflitos |
| RNF05 | Garantir integridade por transações, chaves e controle de concorrência | Teste de atribuições simultâneas |
| RNF06 | Interface responsiva e navegável por teclado | Revisão em celular, desktop e teclado |
| RNF07 | Consultas paginadas com índices adequados | Plano de consultas e teste com massa de dados |
| RNF08 | Registrar erros sem senhas, cookies ou descrições sensíveis | Revisão dos logs |
| RNF09 | Versionar o esquema e permitir instalação em banco vazio | Execução das migrações em ambiente limpo |
| RNF10 | Definir backup e comprovar restauração antes da produção | Exercício documentado de restauração |

Meta inicial proposta: 95% das consultas simples abaixo de 1 segundo em teste com 50 usuários concorrentes e 10 mil chamados. Medir o tempo da API, sem incluir rede externa, e registrar recursos da máquina e cenário. Validar a meta com a infraestrutura disponível.

## 8. Fora do MVP

Anexos, e-mails automáticos, chat em tempo real, integração com diretório corporativo, inventário de equipamentos, múltiplas organizações, SLA com calendário útil, avaliação do atendimento e recuperação de senha por e-mail.

Essas funcionalidades exigem requisitos próprios. Anexos, por exemplo, precisam de limites, autorização de download, armazenamento e tratamento de arquivos perigosos.
