# Validação manual de permissões e atendimento

Use contas distintas de solicitante, técnico e administrador, com sessões separadas. Registre o resultado e o código HTTP de cada cenário. Os testes devem ser feitos em ambiente local ou de homologação com dados descartáveis.

## Visibilidade

- [ ] Sem sessão, `GET /api/chamados` e `GET /api/painel` retornam `401`.
- [ ] O solicitante A abre um chamado; o solicitante B não o encontra na lista e recebe `404` ao consultar detalhe, comentários ou histórico pelo ID.
- [ ] A paginação e os totais do solicitante B não incluem chamados do solicitante A.
- [ ] Um solicitante vê comentários públicos, mas não notas internas.
- [ ] Um técnico autorizado vê notas internas; o histórico não inclui conteúdo de comentários.
- [ ] Uma conta inativa perde acesso mesmo com sessão previamente autenticada.
- [ ] Com troca de senha obrigatória, operações de chamados e administração são recusadas até a troca.

## Atendimento

- [ ] Um técnico ativo assume um chamado aberto sem responsável; responsável e status mudam para técnico e `EM_ATENDIMENTO`.
- [ ] Um segundo técnico não consegue assumir o mesmo chamado após a primeira atribuição.
- [ ] Técnico não responsável não consegue classificar nem alterar o status; o responsável consegue.
- [ ] Administrador atribui a um técnico ativo; atribuir pela primeira vez a um chamado aberto inicia atendimento.
- [ ] Técnico responsável pode pedir informação com comentário público e status `AGUARDANDO_USUARIO`.
- [ ] Solicitante responde publicamente, mas a resposta não muda o status automaticamente.
- [ ] Responsável retoma o atendimento e resolve informando uma solução válida.
- [ ] Solicitante fecha o chamado resolvido ou reabre com justificativa válida.
- [ ] Chamado aberto pode ser cancelado pelo solicitante com justificativa; chamado em atendimento só pode ser cancelado pelo administrador.
- [ ] Chamados fechados ou cancelados recusam comentários e novas transições.
- [ ] Repetir uma ação com `versao` antiga resulta em conflito e não sobrescreve a atualização atual.

## Administração

- [ ] Solicitante e técnico recebem `403` ao usar endpoints administrativos.
- [ ] E-mail duplicado é recusado sem diferenciar maiúsculas de minúsculas.
- [ ] Categoria ou setor inativo não pode ser escolhido em novos registros.
- [ ] Usuário técnico com chamados em atendimento/aguardando usuário não pode ser desativado antes da redistribuição.
- [ ] Redefinir senha exige troca no próximo acesso e invalida as outras sessões.

## Resultado

Para cada falha, anote perfil, endpoint, corpo enviado, resposta esperada e resposta obtida. Não use somente a interface para validar autorização: faça também requisições diretas à API, pois a proteção precisa estar no backend.
