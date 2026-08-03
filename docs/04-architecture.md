# Architecture

## Objetivo

Definir a arquitetura de alto nível do MatchIQ, identificando os grandes módulos do sistema e como eles se comunicam entre si.

Este documento descreve responsabilidades, não implementação. Não há aqui detalhes de tecnologia, classes ou banco de dados — apenas uma visão clara de quais são as partes que compõem a plataforma e qual o papel de cada uma no funcionamento do todo.

## Visão Geral

O MatchIQ é composto por cinco módulos que trabalham em sequência para entregar valor ao usuário: compreender o currículo, obter vagas, comparar perfil com oportunidades, gerar recomendações e apresentar os resultados.

Cada módulo possui uma responsabilidade única e bem definida, e se conecta aos demais formando um fluxo: a saída de um módulo serve como entrada para o próximo. Isso mantém o sistema simples de entender, evoluir e testar.

## Módulos

### Resume Intelligence

Responsável por compreender o currículo enviado pelo usuário e transformá-lo em informações estruturadas que poderão ser utilizadas pelos demais módulos da plataforma.

É o ponto de entrada do fluxo: a partir do documento enviado, identifica quem é o usuário profissionalmente, quais competências possui e qual é o seu perfil para comparação.

### Vacancy Collector

Responsável por obter e organizar as vagas de emprego que servirão de referência para a comparação.

Reúne oportunidades de diferentes origens, estrutura suas informações e garante que as vagas estejam disponíveis e padronizadas para que o Match Engine possa utilizá-las.

### Match Engine

Responsável por comparar o currículo com as vagas disponíveis.

Utiliza as informações estruturadas do currículo e das vagas para calcular o nível de compatibilidade entre o perfil do candidato e cada oportunidade, produzindo o resultado da comparação.

### Recommendation Engine

Responsável por gerar sugestões de evolução profissional a partir do resultado da comparação.

Analisa os pontos fortes e as lacunas identificadas no match e transforma essas informações em recomendações práticas, como melhorias no currículo e planos de estudo, para que o usuário saiba como evoluir.

### Dashboard

Responsável por apresentar tudo ao usuário de forma clara e organizada.

Reúne os resultados das análises, as vagas comparadas, os scores de compatibilidade e as recomendações geradas, oferecendo uma visão completa para que o usuário acompanhe sua evolução profissional.

## Fluxo Geral

```
                Usuário
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
Resume Intelligence     Vacancy Collector
        │                       │
        └───────────┬───────────┘
                    ▼
              Match Engine
                    │
                    ▼
        Recommendation Engine
                    │
                    ▼
               Dashboard
```

O usuário envia seu currículo e indica as vagas de interesse. O Resume Intelligence compreende o currículo enquanto o Vacancy Collector organiza as vagas — esses dois módulos trabalham em paralelo, pois o currículo independe da vaga e a vaga independe do currículo. O Match Engine recebe as duas entradas, compara o perfil do candidato com as oportunidades e produz o resultado da compatibilidade. A partir desse resultado, o Recommendation Engine gera sugestões de evolução, e o Dashboard apresenta tudo ao usuário.

## Princípios Arquiteturais

- Cada módulo possui uma única responsabilidade.
- Os módulos comunicam-se por meio de dados estruturados.
- Um módulo nunca assume responsabilidades de outro módulo.
- A arquitetura foi projetada para permitir evolução independente de cada módulo.
