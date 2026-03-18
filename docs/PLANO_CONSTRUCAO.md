# Plano de Construcao — Order Book de Vibranium

## Visao Geral das Etapas

O projeto sera construido em **10 etapas incrementais**, cada uma entregando valor testavel.
A ordem respeita as dependencias entre componentes: modelo de dados primeiro, depois servicos
de dominio, matching engine, API REST e, por fim, resiliencia e observabilidade.

---

## Etapa 1 — Scaffolding do Projeto Quarkus

**Objetivo:** Criar a estrutura base do projeto com todas as dependencias configuradas.

**Entregaveis:**
- Projeto Quarkus gerado (Maven)
- `pom.xml` com dependencias:
  - `quarkus-resteasy-reactive-jackson` (API REST)
  - `quarkus-hibernate-orm-panache` (ORM)
  - `quarkus-jdbc-postgresql` (driver PostgreSQL)
  - `quarkus-smallrye-health` (health checks)
  - `quarkus-junit5` + `rest-assured` (testes)
- `application.properties` com configuracao de datasource (dev + test via H2/Testcontainers)
- `docker-compose.yml` com PostgreSQL
- `.gitignore` adequado para projeto Java/Quarkus
- Estrutura de pacotes:
  ```
  src/main/java/com/orderbook/
  ├── entity/
  ├── repository/
  ├── service/
  ├── engine/
  ├── rest/
  ├── dto/
  └── exception/
  ```

**Criterio de aceite:** `./mvnw compile` passa sem erros.

---

## Etapa 2 — Modelo de Dados e Entidades JPA

**Objetivo:** Criar as entidades JPA e o schema do banco conforme SOLUCAO.md secao 3.

**Entregaveis:**
- Entidades JPA:
  - `User` (id, name, email, createdAt)
  - `Wallet` (id, userId, balanceBrl, balanceVibranium, reservedBrl, reservedVibranium, version)
  - `Order` (id, userId, side, price, quantity, remaining, status, createdAt, updatedAt)
  - `Trade` (id, buyOrderId, sellOrderId, price, quantity, executedAt)
  - `TransactionHistory` (id, userId, tradeId, type, price, quantity, totalValue, createdAt)
- Enums: `OrderSide` (BUY, SELL), `OrderStatus` (NEW, PARTIALLY_FILLED, FILLED, CANCELLED)
- `@Version` na entidade Wallet para optimistic locking
- Migration SQL (Flyway ou `import.sql`) com indices recomendados
- Constraint UNIQUE em `wallets(user_id)` e `trades(buy_order_id, sell_order_id)`

**Dependencias:** Etapa 1
**Criterio de aceite:** Testes de mapeamento JPA passam; schema gerado corretamente.

---

## Etapa 3 — Camada de Repositorio (Panache)

**Objetivo:** Criar repositorios com queries customizadas necessarias.

**Entregaveis:**
- `UserRepository` (findByEmail)
- `WalletRepository` (findByUserId)
- `OrderRepository` (findByUserId, findOpenOrders para recovery)
- `TradeRepository` (findByOrderId, listagem paginada)
- `TransactionHistoryRepository` (findByUserId paginado)

**Dependencias:** Etapa 2
**Criterio de aceite:** Testes de repositorio com banco de teste passam.

---

## Etapa 4 — Wallet Service

**Objetivo:** Implementar logica de reserva, liberacao e transferencia de saldo.

**Entregaveis:**
- `WalletService` com metodos:
  - `reserveBalance(userId, side, price, quantity)` — reserva saldo ao criar ordem
  - `releaseBalance(userId, order)` — libera saldo ao cancelar ordem
  - `settleTrade(buyOrder, sellOrder, price, quantity)` — transfere saldos no trade
- Tratamento de `OptimisticLockException` com retry
- `InsufficientBalanceException` customizada

**Dependencias:** Etapa 3
**Criterio de aceite:**
- Reserva de saldo decrementa balance e incrementa reserved
- Saldo insuficiente lanca excecao
- Optimistic lock retry funciona sob concorrencia

---

## Etapa 5 — Order Service

**Objetivo:** Implementar criacao e cancelamento de ordens.

**Entregaveis:**
- `OrderService` com metodos:
  - `createOrder(userId, side, price, quantity)` — valida, reserva saldo, persiste
  - `cancelOrder(userId, orderId)` — valida status, libera saldo, atualiza status
  - `getOrder(orderId)` — consulta
  - `getOrdersByUser(userId, page, size)` — listagem paginada
- DTOs de request/response
- Validacao de campos (price > 0, quantity > 0)

**Dependencias:** Etapa 4
**Criterio de aceite:**
- Ordem criada com status NEW e saldo reservado
- Cancelamento libera saldo e atualiza status
- Ordem FILLED/CANCELLED nao pode ser cancelada

---

## Etapa 6 — Matching Engine (Core)

**Objetivo:** Implementar o motor de matching in-memory — o coracao do sistema.

**Entregaveis:**
- `PriceTimeKey` record com Comparator para bids (DESC) e asks (ASC)
- `MatchingEngine` com:
  - `ConcurrentSkipListMap<PriceTimeKey, Order>` para bids e asks
  - `ReentrantLock` para serializar matching
  - `submitOrder(order)` — executa matching e retorna lista de trades
  - `insertWithoutMatching(order)` — para recovery
  - `removeOrder(orderId)` — para cancelamento
  - `getOrderBook()` — snapshot do book para API
- Algoritmo Price-Time Priority:
  - Ordem BUY: casa com asks onde ask.price <= buy.price
  - Ordem SELL: casa com bids onde bid.price >= sell.price
  - Preco de execucao = preco do maker (resting order)
  - Partial fills (multiplos trades por ordem)

**Dependencias:** Etapa 2 (usa entidade Order)
**Criterio de aceite:**
- Match exato: 2 ordens compativeis geram 1 trade
- Match parcial: remaining atualizado corretamente
- Multiplos matches: ordem agressora casa com varias resting orders
- Sem match: ordem adicionada ao book
- Price-Time Priority respeitada

---

## Etapa 7 — Trade Service e Integracao do Matching

**Objetivo:** Integrar matching engine com persistencia e settlement de trades.

**Entregaveis:**
- `TradeService` com:
  - `executeTrades(order, matchResults)` — persiste trades e faz settlement
  - `settleTrade(buyOrder, sellOrder, price, quantity)` — @Transactional atomico
  - `createTransactionHistory(userId, trade, type)` — registra historico
  - `listTrades(page, size)` — listagem paginada
- Integracao no fluxo: OrderService -> MatchingEngine -> TradeService -> WalletService
- Rollback in-memory em caso de falha na persistencia

**Dependencias:** Etapas 4, 5, 6
**Criterio de aceite:**
- Trade persistido com dados corretos
- Wallets atualizadas atomicamente
- TransactionHistory criado para ambos os lados
- Excesso de BRL devolvido ao comprador
- Rollback funciona em caso de falha

---

## Etapa 8 — REST API (JAX-RS)

**Objetivo:** Expor todos os endpoints da API conforme SOLUCAO.md secao 7.

**Entregaveis:**
- `UserResource`:
  - `POST /api/users` — criar usuario com wallet
  - `GET /api/users/{id}/wallet` — consultar saldo
  - `GET /api/users/{id}/transactions` — historico paginado
- `OrderResource`:
  - `POST /api/orders` — criar ordem (header X-User-Id)
  - `GET /api/orders/{id}` — consultar ordem
  - `DELETE /api/orders/{id}` — cancelar ordem
- `OrderBookResource`:
  - `GET /api/orderbook` — snapshot do livro de ofertas
- `TradeResource`:
  - `GET /api/trades` — listar trades paginado
- DTOs de request/response com validacao (Bean Validation)
- `PageResponse<T>` generico para paginacao
- Exception handlers (JAX-RS ExceptionMapper)

**Dependencias:** Etapas 5, 7
**Criterio de aceite:**
- Todos os endpoints respondem conforme especificacao
- Validacao retorna 400 com mensagem clara
- Paginacao funciona corretamente
- Testes de integracao com RestAssured passam

---

## Etapa 9 — Resiliencia (Recovery, Shutdown, Idempotencia)

**Objetivo:** Implementar mecanismos de resiliencia conforme SOLUCAO.md secao 8.

**Entregaveis:**
- `OrderBookRecovery`:
  - Reconstroi book a partir do banco no startup (@Observes StartupEvent)
  - Executa ciclo de re-matching pos-recovery
- `OrderBookShutdown`:
  - Para de aceitar novas ordens (@Observes ShutdownEvent)
  - Drena operacoes in-flight com timeout
- Idempotencia:
  - UUID gerado pelo cliente (aceito no request)
  - Constraint UNIQUE em trades(buy_order_id, sell_order_id)

**Dependencias:** Etapas 6, 7, 8
**Criterio de aceite:**
- Restart reconstroi book corretamente
- Re-matching casa ordens pendentes
- Shutdown graceful nao perde operacoes
- Retry de criacao de ordem com mesmo UUID nao duplica

---

## Etapa 10 — Health Checks e Observabilidade

**Objetivo:** Implementar health checks e preparar para producao.

**Entregaveis:**
- `OrderBookReadinessCheck` — banco acessivel + book reconstruido
- `OrderBookLivenessCheck` — engine respondendo
- Logging estruturado nos pontos criticos (matching, trades, recovery)
- Metricas basicas (opcional): ordens no book, trades/s

**Dependencias:** Etapas 6, 9
**Criterio de aceite:**
- `/q/health/ready` retorna UP apos recovery completa
- `/q/health/live` retorna UP enquanto engine funciona
- Logs permitem rastrear fluxo de uma ordem do recebimento ao trade

---

## Diagrama de Dependencias entre Etapas

```
Etapa 1 (Scaffolding)
   |
Etapa 2 (Modelo de Dados)
   |
   +---> Etapa 3 (Repositorios)
   |        |
   |     Etapa 4 (Wallet Service)
   |        |
   |     Etapa 5 (Order Service)
   |        |         \
   +---> Etapa 6 (Matching Engine)
            |         /
         Etapa 7 (Trade Service + Integracao)
            |
         Etapa 8 (REST API)
            |
         Etapa 9 (Resiliencia)
            |
         Etapa 10 (Health Checks)
```

## Estimativa de Complexidade

| Etapa | Complexidade | Componentes novos |
|-------|-------------|-------------------|
| 1     | Baixa       | Config, POM       |
| 2     | Baixa       | 5 entidades, enums|
| 3     | Baixa       | 5 repositorios    |
| 4     | Media       | WalletService, retry, excecoes |
| 5     | Media       | OrderService, validacoes |
| 6     | Alta        | MatchingEngine, PriceTimeKey, algoritmo |
| 7     | Alta        | TradeService, integracao, atomicidade |
| 8     | Media       | 4 resources, DTOs, mappers |
| 9     | Media       | Recovery, shutdown |
| 10    | Baixa       | Health checks     |
