# Documentacao — Order Book de Vibranium

## Plano de Construcao

- [PLANO_CONSTRUCAO.md](PLANO_CONSTRUCAO.md) — 10 etapas incrementais de implementacao

## Diagramas UML (PlantUML)

| # | Diagrama | Descricao |
|---|----------|-----------|
| 01 | [Modelo de Dados](diagramas/01-modelo-dados.puml) | Diagrama ER com entidades, relacionamentos e indices |
| 02 | [Arquitetura de Componentes](diagramas/02-arquitetura-componentes.puml) | Visao geral da arquitetura com camadas e dependencias |
| 03 | [Fluxo: Criar Ordem](diagramas/03-fluxo-criar-ordem.puml) | Sequencia completa de criacao de ordem (reserva + matching) |
| 04 | [Fluxo: Matching Engine](diagramas/04-fluxo-matching.puml) | Algoritmo Price-Time Priority com loop de matching |
| 05 | [Fluxo: Cancelamento](diagramas/05-fluxo-cancelamento.puml) | Cancelamento de ordem com liberacao de saldo |
| 06 | [Recovery no Startup](diagramas/06-recovery-startup.puml) | Reconstrucao do book + re-matching pos-recovery |
| 07 | [Diagrama de Classes](diagramas/07-diagrama-classes.puml) | Classes de dominio, servicos, engine e REST |
| 08 | [Estados da Ordem](diagramas/08-estados-ordem.puml) | Maquina de estados do ciclo de vida da ordem |
| 09 | [Fluxo: Settlement](diagramas/09-fluxo-settlement.puml) | Transferencia atomica de saldos no trade |
| 10 | [Graceful Shutdown](diagramas/10-graceful-shutdown.puml) | Desligamento ordenado com drenagem de operacoes |

## Como visualizar os diagramas

Os diagramas estao no formato [PlantUML](https://plantuml.com/). Para visualizar:

- **VS Code:** Extensao "PlantUML" (jebbs.plantuml)
- **IntelliJ:** Plugin "PlantUML Integration"
- **Online:** [PlantUML Server](https://www.plantuml.com/plantuml/uml/)
- **CLI:** `java -jar plantuml.jar docs/diagramas/*.puml`
