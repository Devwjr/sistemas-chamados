# Sistema de chamados de suporte de TI

Documentação para desenvolver uma aplicação interna na qual funcionários abrem chamados e a equipe de TI acompanha o atendimento até a conclusão.

Este repositório contém o planejamento; a aplicação ainda não foi implementada. As regras descritas são uma proposta inicial para validar com o setor de TI.

## Documentos

1. [Requisitos do sistema](docs/01-requisitos.md): escopo, perfis, regras de negócio e critérios de aceite.
2. [Arquitetura e banco de dados](docs/02-arquitetura-e-dados.md): tecnologias, organização, entidades e relacionamentos.
3. [Guia do backend Spring](docs/03-guia-backend.md): responsabilidades de cada camada, segurança, API, validações e testes.
4. [Guia do frontend TypeScript](docs/04-guia-frontend.md): telas, componentes, integração com a API e controle de acesso.
5. [Roteiro de desenvolvimento](docs/05-roteiro-de-desenvolvimento.md): sequência prática de implementação e definição de conclusão.

## Base do projeto

- Backend: Java com Spring Boot, Spring Web, Spring Security, Spring Data JPA e Bean Validation.
- Banco: PostgreSQL, com migrações Flyway.
- Frontend: React com TypeScript, Vite e React Router.
- Comunicação: API REST com JSON.
- Autenticação inicial: sessão do Spring Security em cookie seguro, com proteção CSRF.
- Organização: um backend modular e um frontend separado, no mesmo repositório.

React é a escolha adotada neste guia para utilizar TypeScript no frontend. TypeScript é a linguagem; React fornece a estrutura de componentes da interface.

## Estrutura planejada

```text
sistemas-chamados-spring/
├── README.md
├── docs/
├── backend/        # Criar durante a implementação
├── frontend/       # Criar durante a implementação
└── compose.yaml    # Criar para o PostgreSQL local
```

Comece pelos requisitos e siga o roteiro. Não é necessário implementar todas as funcionalidades de uma vez: a primeira entrega deve permitir abrir, atender e resolver um chamado com segurança.
