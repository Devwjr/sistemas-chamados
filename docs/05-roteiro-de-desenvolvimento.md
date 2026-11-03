# Roteiro de desenvolvimento

## Como usar este roteiro

Implementar uma etapa por vez e concluir sua verificação antes de seguir. As etapas são uma sequência de entregas, não uma estimativa de dias. Registrar no README os comandos reais conforme o código for criado.

## Etapa 1 — Validar o funcionamento esperado

Ler os requisitos com o responsável pela TI e confirmar perfis, visibilidade, estados, prioridades e encerramento. Desenhar as telas de abertura, fila e detalhe antes de implementar.

**Entrega:** requisitos revisados e exemplos de um chamado completo, desde a abertura até o fechamento.

## Etapa 2 — Preparar a base técnica

Criar `backend/`, `frontend/` e configuração local do PostgreSQL. Versionar wrappers, locks e um arquivo de exemplo das variáveis. Configurar proxy `/api`, lint, verificação de tipos e comandos de teste/build. Não versionar senhas, arquivos de ambiente reais ou dados de produção.

**Entrega:** backend e frontend iniciam localmente; o banco está acessível com credenciais de desenvolvimento; os comandos estão documentados.

## Etapa 3 — Criar banco e cadastros

Aplicar migrações, entidades, repositórios e validações de usuários, setores e categorias. Preparar o provisionamento controlado do administrador e dados de exemplo somente no ambiente local.

**Entrega:** um banco vazio recebe o esquema completo; restrições impedem e-mail duplicado e relacionamentos inválidos.

## Etapa 4 — Implementar autenticação

Configurar Spring Security, hash de senha, sessão, CSRF, login, logout, consulta do usuário e troca de senha. Criar a tela de acesso e proteção de rotas. Implementar bloqueio de usuários inativos e revogação de sessões.

**Entrega:** usuário entra e sai; operações sem sessão ou CSRF são recusadas; senha temporária exige troca antes do uso.

## Etapa 5 — Entregar abertura e consulta de chamados

Implementar criação, protocolo, listagem paginada, filtros e detalhe. Construir formulário e lista no frontend. Aplicar restrições de visibilidade desde a primeira consulta.

**Entrega:** dois solicitantes abrem chamados e cada um vê apenas os próprios registros, inclusive nos totais e acessos por ID.

## Etapa 6 — Implementar atendimento

Adicionar atribuição, classificação, estados, histórico e controle de versão. Criar os formulários de resolução, espera, reabertura e cancelamento.

**Entrega:** o fluxo da tabela de estados funciona; transições inválidas são recusadas; dois técnicos disputando o mesmo chamado produzem um sucesso e um conflito.

## Etapa 7 — Adicionar comunicação

Implementar comentários públicos e notas internas paginadas. Registrar mensagem pública na transição para aguardando usuário. Preservar o texto do formulário em falhas e conflitos.

**Entrega:** a TI conversa com o solicitante e registra notas internas que não são expostas a ele por nenhum endpoint.

## Etapa 8 — Completar administração e painel

Criar telas e rotas de usuários, setores e categorias, inclusive desativação e redefinição de senha. Acrescentar painel com contagens autorizadas e opções de responsáveis ativos.

**Entrega:** o administrador opera os cadastros pela interface e os indicadores correspondem aos chamados visíveis.

## Etapa 9 — Validar a primeira versão

Executar testes de negócio, segurança, integração com PostgreSQL e fluxo completo do navegador. Revisar acesso por teclado, uso em celular, estados vazios e mensagens de erro. Medir a meta de desempenho em cenário documentado.

**Entrega:** critérios de aceite do MVP atendidos, falhas relevantes resolvidas e limitações registradas.

## Etapa 10 — Preparar implantação

Configurar build de produção, domínio único, HTTPS, cookies seguros, segredos externos, logs e health check. Definir execução de migrações, rotina de backup e exercício de restauração. Documentar procedimento de atualização e recuperação; rollback de aplicação deve considerar compatibilidade do esquema do banco.

**Entrega:** ambiente de homologação utilizável, restauração comprovada e validação do setor de TI antes da produção.

## Exemplo de fluxo para validação manual

1. Administrador cadastra setor, categoria, solicitante e técnico.
2. Solicitante entra, troca a senha temporária e abre um chamado.
3. Outro solicitante tenta consultar esse chamado e recebe `404`.
4. Técnico assume o chamado; o sistema registra atribuição e início do atendimento.
5. Técnico registra nota interna; o solicitante não a recebe na API.
6. Técnico pede informação e muda para aguardando usuário.
7. Solicitante responde publicamente; o técnico retoma e resolve com uma solução.
8. Solicitante reabre com justificativa; o histórico preserva a resolução anterior.
9. Técnico resolve novamente; o solicitante fecha.
10. Tentativas de comentar ou alterar o estado terminal são recusadas.
11. Administrador desativa um usuário; sessões existentes deixam de permitir acesso.

## Definição de conclusão do MVP

- Todos os requisitos funcionais têm critérios de aceite verificados.
- Regras de acesso são aplicadas no backend e testadas com usuários diferentes.
- Migrações funcionam em banco limpo e estão versionadas.
- Build do frontend, verificação de tipos e testes relevantes passam.
- API, variáveis de ambiente e comandos estão documentados.
- Nenhum segredo, hash de senha ou nota interna é exposto indevidamente.
- Há procedimento testado de backup e restauração e validação em homologação.

Depois da primeira entrega, priorizar evoluções com a equipe: anexos, notificações, SSO, recuperação de senha, SLA e relatórios. Cada evolução deve ter regras e critérios de aceite antes de entrar em desenvolvimento.
