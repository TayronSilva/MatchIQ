# Database

## Objetivo

Definir quais informações o MatchIQ precisa armazenar para funcionar.

Este documento descreve o modelo conceitual de dados: quais entidades serão persistidas, que tipo de informação cada uma guarda e como elas se relacionam. Não há aqui SQL, tabelas ou detalhes de implementação — apenas o que precisa existir permanentemente no sistema.

## Entidades

### Usuário

**Descrição**

Representa a pessoa que utiliza o MatchIQ para analisar sua carreira. É o dono dos dados dentro da plataforma: envia currículos, acompanha análises e recebe recomendações.

**Informações armazenadas**

- Dados de identificação (nome, e-mail)
- Dados de acesso à plataforma
- Configurações da conta (idioma, tema, notificações)
- Data de criação da conta

**Relacionamentos**

- Possui um ou vários Currículos.
- Possui uma ou várias Análises.
- Possui uma ou várias Recomendações.

---

### Currículo

**Descrição**

Representa um currículo enviado pelo usuário para análise. É a principal fonte de informações sobre o perfil profissional do candidato.

**Informações armazenadas**

- Nome do arquivo
- Conteúdo extraído
- Data de envio
- Idioma

**Relacionamentos**

- Pertence a um Usuário.
- Possui diversas Skills identificadas.
- Pode participar de vários Matches.

---

### Vaga

**Descrição**

Representa uma oportunidade de emprego coletada pelo sistema e usada como referência para a comparação com o perfil do candidato.

**Informações armazenadas**

- Título e descrição da vaga
- Empresa contratante
- Localização e modalidade
- Data de publicação
- Origem da vaga

**Relacionamentos**

- Possui diversas Skills exigidas.
- Pode participar de vários Matches.

---

### Skill

**Descrição**

Representa uma competência identificada no currículo ou exigida por uma vaga. É a unidade básica de comparação entre o perfil do candidato e a oportunidade.

**Informações armazenadas**

- Nome da competência
- Categoria (técnica, comportamental, etc.)

**Relacionamentos**

- Pode estar associada a vários Currículos.
- Pode estar associada a várias Vagas.
- É utilizada nos cálculos dos Matches.

**Observação**

A skill em si não possui nível — quem possui nível é a relação entre a skill e o currículo (ex.: o usuário possui Java em nível avançado) ou entre a skill e a vaga (ex.: a vaga exige Java). Por isso, neste momento a Skill armazena apenas nome e categoria; a decisão de onde guardar nível e anos de experiência será tomada na modelagem detalhada, possivelmente por meio de relações como CurrículoSkill e VagaSkill.

---

### Match

**Descrição**

Representa o resultado da comparação entre um currículo e uma vaga. É o ponto central do sistema: expressa o quanto o perfil do candidato atende aos requisitos da oportunidade.

**Informações armazenadas**

- Score de compatibilidade
- Versão do algoritmo utilizada na comparação
- Data da comparação
- Status da comparação

**Relacionamentos**

- Compara um Currículo com uma Vaga.
- Gera uma Análise.
- Utiliza as Skills do Currículo e da Vaga como base do cálculo.

---

### Análise

**Descrição**

Representa o diagnóstico técnico gerado a partir de um match. Detalha o resultado da comparação, apontando o que está alinhado e o que falta para o candidato aumentar sua aderência à vaga.

**Informações armazenadas**

- Pontos fortes identificados
- Competências ausentes ou pouco exploradas
- Lacunas técnicas detectadas
- Observações gerais sobre o perfil

**Relacionamentos**

- Pertence a um Match.
- Pertence a um Usuário.
- Serve de base para a geração de Recomendações.

---

### Recomendação

**Descrição**

Representa as orientações de evolução profissional geradas a partir da análise. Transforma as lacunas identificadas em ações práticas para o usuário evoluir.

**Informações armazenadas**

- Sugestões de melhoria no currículo
- Plano de estudos sugerido
- Prioridade da recomendação
- Data de geração

**Relacionamentos**

- Pertence a uma Análise.
- Pertence a um Usuário.
- Está associada às Skills que motivaram a recomendação.

---

## Resumo dos Relacionamentos

```
Usuário (1)
├── (N) Currículos
├── (N) Análises
└── (N) Recomendações

Currículo (N)
├── (N) Skills
└── (N) Matches

Vaga (N)
├── (N) Skills
└── (N) Matches

Match (1)
└── (1) Análise

Análise (1)
└── (N) Recomendações
```
