# ReleaseOps

Plataforma para gerenciamento de releases, solicitações de deploy, aprovações e mudanças operacionais de aplicações.

O ReleaseOps nasce para substituir processos dispersos em mensagens, planilhas e documentos por um fluxo centralizado, rastreável e orientado por regras de negócio.

## Status

O projeto está em fase inicial de estruturação. As primeiras entregas serão desenvolvidas com TDD, começando pelo ciclo de vida de uma solicitação de deploy.

## Objetivos

- Centralizar releases e solicitações de deploy.
- Controlar aprovações conforme ambiente e risco da mudança.
- Registrar o histórico e a auditoria das operações.
- Acompanhar implantações, falhas e rollbacks.
- Evoluir para integração com pipelines e ferramentas externas.

## Escopo inicial

O primeiro MVP contemplará:

- Organizações e usuários.
- Equipes e aplicações.
- Ambientes de implantação.
- Releases.
- Solicitações de deploy.
- Aprovação e rejeição de solicitações.
- Registro manual do início e do resultado de um deploy.
- Histórico de alterações.

Execução automática de pipelines, integrações externas, mensageria e gestão completa de incidentes não fazem parte da primeira versão.

## Tecnologias iniciais

- Java 21
- Spring Boot
- Maven
- JUnit 5
- AssertJ

Novas tecnologias serão introduzidas conforme necessidades concretas do produto.

## Estrutura do repositório

```text
releaseops/
├── backend/   # Aplicação Java com Spring Boot
├── docs/      # Documentação do produto e da arquitetura
└── README.md
```

O frontend será incluído posteriormente em um diretório próprio.

## Pré-requisitos

- JDK 21
- Git

Não é necessário instalar o Maven globalmente. O repositório utiliza o Maven Wrapper.

## Build

No Windows:

```powershell
cd backend
.\mvnw.cmd clean verify
```

No Linux ou macOS:

```bash
cd backend
./mvnw clean verify
```

## Execução local

No Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS:

```bash
cd backend
./mvnw spring-boot:run
```

## Documentação

- [Visão do produto](docs/product-vision.md)

As decisões arquiteturais relevantes serão registradas ao longo da evolução do projeto.

