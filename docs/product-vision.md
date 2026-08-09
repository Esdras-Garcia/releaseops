# Visão do produto — ReleaseOps

## Contexto

Equipes de tecnologia frequentemente coordenam releases e deploys por meio de mensagens, planilhas, documentos e diferentes ferramentas sem uma visão centralizada. Esse processo dificulta a aprovação das mudanças, a identificação dos responsáveis e a consulta ao histórico de uma implantação.

## Problema

Quando o processo de liberação não possui um fluxo claro e rastreável, a equipe enfrenta problemas como:

- Aprovações informais ou sem registro.
- Falta de informações sobre risco e impacto.
- Dificuldade para saber qual versão está em cada ambiente.
- Ausência de um histórico confiável de deploys.
- Planos de rollback incompletos ou dispersos.
- Baixa rastreabilidade entre mudanças, falhas e incidentes.

## Proposta

O ReleaseOps será uma plataforma para centralizar o ciclo operacional de uma mudança:

1. Preparação de uma release.
2. Solicitação de deploy para um ambiente.
3. Avaliação de risco e impacto.
4. Aprovação ou rejeição.
5. Registro da execução.
6. Confirmação do resultado.
7. Rollback ou abertura de incidente quando necessário.
8. Preservação do histórico para consulta e auditoria.

Na primeira versão, o ReleaseOps gerenciará o processo, mas não executará deploys automaticamente.

## Visão

Para equipes de tecnologia que precisam organizar e controlar mudanças em suas aplicações, o ReleaseOps é uma plataforma de gestão de releases e deploys que centraliza solicitações, aprovações e registros operacionais. Diferentemente de processos baseados em mensagens e planilhas, o produto oferece regras explícitas, rastreabilidade e uma fonte única de informações.

## Usuários

### Desenvolvedor

- Prepara releases.
- Solicita deploys.
- Informa riscos, impactos e plano de rollback.
- Acompanha aprovações e resultados.

### QA

- Registra evidências de validação.
- Aprova ou rejeita mudanças quando sua participação for exigida.
- Confirma o comportamento da aplicação após a implantação.

### Líder técnico

- Avalia risco e impacto.
- Aprova ou rejeita solicitações.
- Acompanha mudanças em andamento.
- Participa da decisão de rollback.

### Administrador

- Gerencia usuários e equipes.
- Cadastra aplicações e ambientes.
- Define permissões e políticas operacionais.
- Consulta o histórico e a auditoria.

## Conceitos principais

### Organização

Representa uma empresa ou grupo isolado dentro da plataforma.

### Equipe

Agrupa usuários responsáveis por uma ou mais aplicações.

### Aplicação

Representa um sistema que possui releases e pode ser implantado em diferentes ambientes.

### Ambiente

Representa um destino de implantação, como desenvolvimento, QA, homologação ou produção.

### Release

Representa uma versão preparada de uma aplicação. Pode conter versão, descrição, commit, tag, mudanças e evidências de teste.

### Solicitação de deploy

Representa a intenção de implantar determinada release em um ambiente. Contém justificativa, risco, impacto, data desejada e plano de rollback.

### Aprovação

Registra a decisão de um usuário autorizado sobre uma solicitação de deploy.

### Execução

Representa uma tentativa real de implantação, com início, término, responsável e resultado.

## Primeira capacidade do produto

A primeira entrega permitirá representar o ciclo inicial de uma solicitação de deploy:

1. A solicitação é criada como rascunho.
2. O rascunho é submetido para aprovação.
3. Uma solicitação já submetida não pode ser submetida novamente.

Esses comportamentos formarão o primeiro ciclo de desenvolvimento orientado por testes.

## MVP

O MVP deverá permitir:

- Cadastrar organizações e usuários.
- Organizar usuários em equipes.
- Cadastrar aplicações e ambientes.
- Registrar releases.
- Criar e submeter solicitações de deploy.
- Aprovar ou rejeitar solicitações.
- Registrar manualmente o início e o resultado de um deploy.
- Consultar o histórico das mudanças.

## Fora do MVP

Os itens abaixo poderão ser desenvolvidos posteriormente, mas não fazem parte da primeira versão:

- Execução automática de pipelines.
- Integração com GitHub ou outros provedores Git.
- Notificações por e-mail ou mensageria externa.
- Atualizações em tempo real.
- Gestão completa de incidentes.
- Políticas de aprovação configuráveis.
- Cobrança e assinaturas.
- Arquitetura de microserviços.
- Kubernetes.

## Regras iniciais de negócio

- Toda solicitação de deploy começa como rascunho.
- Apenas um rascunho pode ser submetido para aprovação.
- O solicitante não pode aprovar a própria solicitação.
- Uma pessoa não pode aprovar a mesma solicitação mais de uma vez.
- Um deploy não pode começar sem as aprovações exigidas.
- Mudanças relevantes realizadas após uma aprovação invalidam as aprovações anteriores.
- O histórico de uma solicitação não pode ser apagado depois do início do deploy.

As regras serão refinadas conforme o domínio for explorado e os casos de uso forem implementados.

## Princípios de desenvolvimento

- Desenvolver comportamentos em ciclos curtos de TDD.
- Começar com um monólito modular.
- Manter regras de negócio independentes de frameworks sempre que possível.
- Adicionar dependências somente quando existir uma necessidade concreta.
- Registrar decisões arquiteturais relevantes.
- Priorizar clareza, testabilidade e evolução segura.

## Critérios iniciais de sucesso

- O fluxo de uma solicitação pode ser compreendido sem consultar mensagens externas.
- Transições inválidas são impedidas pelas regras do domínio.
- Ações relevantes possuem autoria e histórico.
- Usuários de uma organização não acessam dados de outra.
- O projeto possui testes automatizados para as regras essenciais.

