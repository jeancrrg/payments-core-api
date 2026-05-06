# payments-core

Microserviço de pagamentos integrado ao gateway **Stripe**, com:

- **MVC tradicional** (Controller → Service → Repository)
- **Retry e Circuit Breaker** com **Resilience4j** em todas as chamadas externas
- **Cache distribuído** com **Redis** nas leituras
- **Processamento assíncrono** via **`@Async` + ThreadPoolTaskExecutor**
- **Migrations** com **Flyway** (incluindo seed de pagamentos de teste)
- **Documentação OpenAPI/Swagger** via Springdoc
- **Containerização** completa com Docker e docker-compose

---

## Stack

| Tecnologia | Uso |
|-----------|-----|
| Spring Boot 4.0.6 / Java 25 | Framework base |
| Spring Web MVC + Validation | API REST |
| Spring Data JPA + Hibernate | ORM |
| PostgreSQL 16 | Banco relacional |
| Spring Data Redis (Lettuce) | Cache distribuído |
| Flyway | Migrations versionadas |
| Resilience4j 2.2 | Retry / Circuit breaker |
| Stripe Java SDK 28 | Gateway de pagamento |
| Springdoc OpenAPI 2.6 | Swagger UI |
| MapStruct + Lombok | Boilerplate reduction |
| Docker / docker-compose | Containerização |

---

## Estrutura de Pastas

```
src/main/java/com/billing/payments_core_api/
├── PaymentsCoreApiApplication.java
├── config/                  # AsyncConfig, RedisConfig, StripeConfig, OpenApiConfig, WebConfig
├── controller/              # PaymentController, RefundController, HealthController
├── service/                 # PaymentService, RefundService, async/PaymentNotificationService
├── repository/              # PaymentRepository, RefundRepository
├── model/
│   ├── entity/              # Payment, Refund, PaymentStatus, RefundStatus
│   └── dto/{request,response}/
├── mapper/                  # PaymentMapper (MapStruct)
├── integration/stripe/      # StripeGatewayClient
└── exception/               # GlobalExceptionHandler, exceções, ApiError

src/main/resources/
├── application.properties           # configurações comuns
├── application-prod.properties      # perfil prod
├── application-test.properties      # perfil test
└── db/migration/
    ├── V1__create_payments_table.sql
    ├── V2__create_refunds_table.sql
    └── V3__seed_test_payments.sql
```

---

## Como executar

### 1. Subir a aplicação completa (recomendado)

Pré-requisitos: **Docker** e **docker-compose** instalados.

```bash
cp .env.example .env             # ajuste STRIPE_API_KEY com sua chave sk_test_
docker-compose up --build
```

Sobe três containers conectados na rede `payments-net`:

- `payments-postgres` (porta 5432)
- `payments-redis` (porta 6379)
- `payments-core` (porta 8080)

A aplicação aguarda os healthchecks de Postgres e Redis antes de iniciar. As migrations Flyway rodam automaticamente no startup.

### 2. Executar localmente (Maven)

```bash
# Subir apenas Postgres + Redis
docker-compose up -d postgres redis

# Variáveis para o profile test
export SPRING_PROFILES_ACTIVE=test
export STRIPE_API_KEY=sk_test_...

./mvnw spring-boot:run
```

---

## Perfis

A escolha é feita por `SPRING_PROFILES_ACTIVE` (padrão: `test`):

| Perfil | Quando usar | Características |
|--------|-------------|-----------------|
| `test` | Dev local / CI | SQL verboso, logs DEBUG, datasource local |
| `prod` | Produção | SQL silencioso, pool maior, logs INFO |

---

## Endpoints

| Método | Path | Descrição |
|--------|------|-----------|
| POST   | `/api/v1/payments` | Cria pagamento via Stripe |
| GET    | `/api/v1/payments/{id}` | Busca pagamento (cache Redis) |
| GET    | `/api/v1/payments/{id}/status` | Status atual |
| POST   | `/api/v1/payments/{id}/sync` | Re-sincroniza com Stripe |
| GET    | `/api/v1/payments/customer/{customerId}` | Lista paginada (cache Redis) |
| POST   | `/api/v1/refunds` | Solicita estorno |
| GET    | `/api/v1/refunds/{id}` | Busca estorno (cache Redis) |
| GET    | `/api/v1/refunds/payment/{paymentId}` | Estornos de um pagamento |
| GET    | `/api/v1/health` | Healthcheck |
| GET    | `/swagger-ui.html` | UI Swagger |
| GET    | `/v3/api-docs` | Contrato OpenAPI |
| GET    | `/actuator/health` | Health Spring Actuator |

### Exemplo — criar pagamento

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust_alice",
    "amount": 199.90,
    "currency": "USD",
    "paymentMethodId": "pm_card_visa",
    "description": "Subscription monthly fee"
  }'
```

### Exemplo — solicitar estorno parcial

```bash
curl -X POST http://localhost:8080/api/v1/refunds \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId": "11111111-1111-1111-1111-111111111103",
    "amount": 50.00,
    "reason": "requested_by_customer"
  }'
```

---

## Resiliência, Cache e Async

**Retry / Circuit Breaker (Stripe):** instância `stripeApi` com 3 tentativas, backoff exponencial 500ms × 2, retry em `ApiConnectionException`, `RateLimitException` e `StripeIntegrationException`. Circuit breaker abre quando 50% das chamadas falham em janela de 10, com 30s em estado OPEN. Configurável em `application.properties`.

**Cache Redis:** três caches com TTL distintos — `customerPayments` (5 min), `paymentById` (10 min), `refundById` (10 min). Operações de escrita (`createPayment`, `requestRefund`, `syncWithStripe`) usam `@CacheEvict` para invalidação coerente.

**Async:** pool dedicado `paymentExecutor` (5 / 20 / queue 100) usado pelo `PaymentNotificationService` (`@Async`) para notificações e auditoria após cada pagamento e estorno. Falhas não derrubam a operação principal — são logadas pelo `LoggingAsyncExceptionHandler`.

---

## Migrations e Seed

Três migrations versionadas em `src/main/resources/db/migration/`. A V3 popula 10 pagamentos fictícios para 4 clientes de teste (`cust_alice`, `cust_bob`, `cust_carol`, `cust_dave`) cobrindo todos os status (PENDING, PROCESSING, SUCCEEDED, FAILED, CANCELLED, REFUNDED, PARTIALLY_REFUNDED), além de 2 estornos de exemplo.

---

## Variáveis de Ambiente

Veja `.env.example`. As principais:

| Variável | Descrição |
|----------|-----------|
| `SPRING_PROFILES_ACTIVE` | `prod` ou `test` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Credenciais Postgres |
| `REDIS_HOST`, `REDIS_PORT` | Endereço Redis |
| `STRIPE_API_KEY` | Chave Stripe `sk_test_...` |

---

## Documentação Swagger

Após subir o serviço, acesse:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

Todos os endpoints e DTOs estão anotados com `@Tag`, `@Operation`, `@ApiResponse` e `@Schema`.
