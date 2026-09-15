# PayFlux

PayFlux is a prototype merchant payment platform with merchant authentication, payment
links, simulated payment processing, rules-based fraud analysis, refunds, and a dashboard.
It is a monorepo containing a Spring Boot API, a React/Vite merchant and customer frontend,
and a Docker Compose MySQL service.

## Repository layout

```text
/backend          Spring Boot 3 API, JPA persistence, security, and scheduled jobs
/frontend         React 18 + TypeScript + Vite merchant/customer application
docker-compose.yml
```

## Prerequisites

- Docker with Docker Compose
- JDK 17
- Maven 3.9+
- Node.js 20+

## Run locally

Run these commands from the repository root, in order.

### 1. Start MySQL

```bash
docker compose up -d
```

This starts MySQL 8.4 on port `3306`. The database, application user, and application
password are all `payflux` in the committed development Compose configuration.

### 2. Start the backend

```bash
cd backend
mvn spring-boot:run
```

The API listens on port `8080`. Alternatively, build and run the packaged application:

```bash
mvn -DskipTests package
java -jar target/*.jar
```

Backend environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/payflux?createDatabaseIfNotExist=true&serverTimezone=UTC` | JDBC connection URL |
| `DB_USER` | `payflux` | MySQL username |
| `DB_PASSWORD` | `payflux` | MySQL password |
| `JWT_SECRET` | Development secret in `application.yml` | JWT signing secret; use a 32+ byte secret outside local development |
| `CORS_ORIGINS` | `http://localhost:5173` | Comma-separated allowed frontend origins |
| `PROCESSOR_OUTCOME` | `SUCCESS` | Default mock processor outcome: `SUCCESS`, `FAILURE`, or `TIMEOUT` |

### 3. Start the frontend

In a second terminal, from the repository root:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

The Vite development server listens on port `5173`. The frontend environment variable is:

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_API_URL` | `http://localhost:8080` | Backend API base URL |

## Demo walkthrough

1. Open `/register` and register a merchant.
2. Create an order for `₹60,000` for a medium-risk payment, or `₹1,20,000` for a
   high-risk payment.
3. Open the generated `/pay/:id` link and submit a payment.
   - `₹60,000` reaches verification; use the fixed test code `123456`.
   - `₹1,20,000` is rejected automatically as high risk.
4. For a captured payment, open **Transactions**, select the payment, and review the
   **Flagged because:** section when present.
5. Submit **Confirm fraud** or **Mark as false positive**, then reload the detail page to
   verify that the persisted feedback is rendered.
6. From a captured transaction, issue a refund and watch its status from the transaction
   detail page or **Refunds**.

### Test-mode triggers

| Input | Result |
| --- | --- |
| Card number ending in `0000` | Processor failure / bank decline |
| Card number ending in `9999` | Processor timeout |
| UPI ID beginning `fail@` | Processor failure |
| UPI ID beginning `slow@` | Processor timeout |
| Refund reason containing `simulate-fail` | Refund fails in the scheduled processor |
| `PROCESSOR_OUTCOME=SUCCESS`, `FAILURE`, or `TIMEOUT` | Default mock processor result when no deterministic trigger applies |

The payment request also accepts the backend-only `simulateOutcome` field with a
`SUCCESS`, `FAILURE`, or `TIMEOUT` override.

## API overview

Authenticated merchant endpoints require the bearer token returned by registration or login.
Payment and order creation support the optional `X-Idempotency-Key` header.

### Authentication

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Register a merchant and return a JWT |
| `POST` | `/api/auth/login` | Authenticate a merchant and return a JWT |
| `GET` | `/api/auth/me` | Return the authenticated merchant |

### Orders

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/orders` | Create an order/payment link |
| `GET` | `/api/orders?page&size&status&from&to` | List merchant orders with status and UTC date filters |
| `GET` | `/api/orders/{id}` | Return an order and its payment attempts |

### Public payment links

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/public/orders/{id}` | Return public order/payment-link details |
| `POST` | `/api/public/orders/{id}/payments` | Submit a public payment attempt |
| `POST` | `/api/public/payments/{id}/verify` | Verify a medium-risk payment with an OTP |
| `GET` | `/api/public/payments/{id}` | Return a public payment result |

### Payments

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/payments?page&size&status&riskLevel&method` | List merchant payments with filters |
| `GET` | `/api/payments/{id}` | Return payment detail, fraud analysis, refunds, and timeline |
| `POST` | `/api/payments/{id}/refunds` | Start a refund for a captured payment |

### Refunds

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/refunds?page&size&status` | List merchant refunds with an optional status filter |
| `GET` | `/api/refunds/{id}` | Return one refund |

### Fraud alerts

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/fraud-alerts?page&size` | List medium- and high-risk merchant payments |
| `POST` | `/api/fraud-alerts/{id}/feedback` | Save merchant feedback for a fraud analysis ID |

### Dashboard

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/dashboard/summary` | Return today, last-seven-days, daily, and recent-alert statistics |

## Fraud rules

Fraud analysis is a deterministic `rules-v1` placeholder. It returns the same analysis
shape that a real model would return, including features, factors, score, level, and
prediction. Factor weights are summed and capped at `1.00`.

| Factor | Condition | Weight |
| --- | --- | --- |
| High amount | Amount `> ₹50,000` | `+0.45` |
| Very high amount | Amount `> ₹100,000` | `+0.30` |
| Above customer average | Amount is more than `4x` the customer's prior captured average | `+0.30` |
| Repeated failures | At least 3 failed/rejected attempts in the last 10 minutes | `+0.35` |
| High frequency | At least 5 attempts in the last 10 minutes | `+0.15` |
| New device | New device and amount `> ₹10,000` | `+0.15` |

Risk thresholds:

- `LOW`: score `< 0.40`
- `MEDIUM`: score `< 0.70` and `>= 0.40`
- `HIGH`: score `>= 0.70`

## Payment state machine

```text
CREATED
  -> INITIATED
  -> FRAUD_CHECK
       ├─ LOW    -> AUTHORIZED -> PROCESSING -> CAPTURED
       ├─ MEDIUM -> VERIFICATION_REQUIRED
       │              ├─ valid code   -> AUTHORIZED
       │              └─ invalid code -> REJECTED
       └─ HIGH   -> REJECTED

PROCESSING -> FAILED
CAPTURED -> PARTIALLY_REFUNDED -> REFUNDED
CAPTURED -> REFUNDED
PARTIALLY_REFUNDED -> PARTIALLY_REFUNDED or REFUNDED
```

## Judgment calls vs the SRS

- Fraud features are stored as JSON columns on `fraud_analysis`, rather than in a separate
  `fraud_feature` table. Merchant feedback is stored on that same analysis row alongside
  the frozen features and factors.
- Medium risk uses simulated verification with the fixed test code `123456`.
- High risk is rejected automatically before processor capture.
- Orders default to a 15-minute expiry. A scheduled job expires orders every 30 seconds,
  and public reads also check expiry so stale links are not payable.
- Refunds are processed asynchronously by a five-second scheduled job. Records move through
  `PENDING` and `PROCESSING` to `PROCESSED` or `FAILED`.
- Admin frontend routes are a stub available to any logged-in user; backend `/api/admin/**`
  paths are reserved for the `ADMIN` role, and no admin API is implemented yet.
- JWTs expire after 12 hours and the frontend stores the token in `localStorage`.
- Hibernate `ddl-auto=update` manages the schema for this prototype.
- IDs are prefixed `ord_`, `pay_`, `rfnd_`, and `fa_` to make entity types recognizable.
- The demo is INR-first: defaults, copy, fraud thresholds, and walkthrough amounts use INR.
  The order DTO accepts a three-letter currency code for future extension.
- Payment and order creation use idempotency keys scoped to the merchant/order.
- Payment history used by fraud analysis is merchant- and customer-scoped; customer averages
  use prior captured payments, while attempt/failure counts use a ten-minute window.

## Out of scope

Webhooks, settlement, card/tokenization vaulting, rate limiting, real machine-learning
fraud scoring, and implemented admin functionality are outside this prototype.