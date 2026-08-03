# Domain Model

## Introdução

O MatchIQ é um assistente de carreira que ajuda profissionais de tecnologia a entenderem o quanto seus currículos estão alinhados com as vagas de emprego que desejam.

Para que o sistema funcione, ele precisa conhecer e relacionar alguns conceitos centrais do negócio: quem usa a plataforma, o que essa pessoa entrega para análise, o que ela está buscando, e o que o sistema produz a partir dessa comparação.

Este documento apresenta essas entidades de forma simples, como uma explicação para um desenvolvedor que acabou de entrar na equipe. Aqui não há atributos, tabelas ou detalhes técnicos — apenas a descrição de negócio de cada entidade e o papel que ela desempenha no funcionamento do MatchIQ.

## Entidades do Domínio

### Usuário

Representa a pessoa que utiliza o MatchIQ para analisar sua carreira.

É o dono dos dados dentro da plataforma: ele envia seu currículo, informa as vagas de interesse e recebe os resultados das análises e recomendações geradas pelo sistema.

### Currículo

Representa o currículo enviado pelo usuário para análise.

É a principal fonte de informações utilizada pelo sistema para identificar competências, experiências profissionais, formação acadêmica e demais informações relevantes para o processo de comparação com vagas.

### Vaga

Representa uma oportunidade de emprego que serve de referência para a comparação.

A vaga contém o perfil esperado pelo empregador, incluindo as competências, experiências e conhecimentos exigidos para a posição. É contra esse perfil que o currículo do usuário é comparado.

### Skill

Representa uma competência identificada no currículo ou exigida por uma vaga.

A skill é a unidade básica de comparação do MatchIQ: o sistema extrai as competências presentes no currículo, identifica as competências exigidas pela vaga e as relaciona para calcular o nível de aderência entre o candidato e a oportunidade.

### Match

Representa o resultado da comparação entre um currículo e uma vaga.

O match expressa, por meio de um score de compatibilidade, o quanto o perfil do candidato atende aos requisitos da oportunidade, servindo como base para a análise e para as recomendações geradas pelo sistema.

### Análise

Representa o diagnóstico técnico gerado a partir de um match.

A análise detalha o resultado da comparação, apontando os principais pontos fortes do currículo, as competências ausentes ou pouco exploradas e as lacunas que precisam ser desenvolvidas para aumentar a aderência do candidato à vaga.

### Recomendação

Representa as orientações de evolução profissional geradas para o usuário.

A recomendação transforma as lacunas identificadas na análise em ações práticas, como sugestões de melhorias no currículo e planos de estudo personalizados, para que o usuário saiba exatamente onde e como evoluir.

## Relação entre as entidades

```
Usuário
   │
   ▼
Currículo
   │
   ▼
Skill
   │
   ├──────────────┐
   ▼              ▼
Vaga         Match
                  │
                  ▼
             Análise
                  │
                  ▼
            Recomendação
```

Este é um mapa mental simples do fluxo principal do MatchIQ: o usuário envia seu currículo, o sistema identifica as skills presentes nele, compara essas skills com as exigidas pela vaga e gera um match. A partir do match, o sistema produz a análise e, em seguida, as recomendações de evolução profissional. Cada entidade se conecta à próxima nesse fluxo.
