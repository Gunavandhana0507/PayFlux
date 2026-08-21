# PayFlux

Intelligent payment gateway prototype: React + Spring Boot + MySQL, with a rule-based fraud
engine that returns the contributing factors behind every risk decision (placeholder for the
ML service described in the SRS).

```
/backend    Spring Boot 3 API (Java 17, Maven, Spring Security + JWT, Spring Data JPA)
/frontend   React 18 + Vite + TypeScript app (Tailwind, React Router, Recharts, Axios)
/scripts    smoke.sh - end-to-end API walkthrough
```

## Run it locally

### 1. MySQL

```bash
docker compose up -d
```

Starts MySQL 8 on `localhost:3306` with database/user/password `payflux`.

### 2. Backend (http://localhost:8080)

```bash
cd backend
mvn spring-boot:run
```

Schema is created automatically (`ddl-auto: update`). Overridable settings:
`PAYFLUX_DB_URL`, `PAYFLUX_DB_USER`, `PAYFLUX_DB_PASSWORD`, `PAYFLUX_JWT_SECRET`,
`PAYFLUX_CHECKOUT_BASE_URL`.

Run without Docker/MySQL using the in-memory profile:

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### 3. Frontend (http://localhost:5173)

```bash
cd frontend
npm install
npm run dev
```

`VITE_API_BASE_URL` (see `.env.example`) points the app at the API; it defaults to
`http://localhost:8080`.

Production build / lint:

```bash
cd frontend && npm run build && npm run lint
```

### 4. API-only smoke test

```bash
bash scripts/smoke.sh
```

## Manual end-to-end flow

1. Sign up at `/signup` with the merchant KYC fields, which logs you into `/dashboard`.
2. Create an order (try `60000` INR to trip the risk rules) and open its checkout link.
3. Pay on `/pay/:orderId` with any method. Medium risk asks for an OTP - use `123456`.
4. Open the transaction from `/dashboard/transactions` to see the score, the contributing
   factors and the evaluated features.
5. Submit **Confirm Fraud** or **Mark as False Positive**; the verdict is stored against the
   original feature set and shown on reload.
6. Refund the captured payment in full or in part; refunds settle asynchronously
   (`PENDING -> PROCESSING -> PROCESSED`).

## Risk rules (placeholder for the ML service)

| Rule | Weight |
| --- | --- |
| Amount above the high-value threshold (50,000) | 35 |
| Amount above the very-high threshold (200,000) | 55 |
| 3+ failed attempts in the last 10 minutes | 30 |
| At least one recent failed attempt | 10 |
| Device not seen before | 15 |
| First order from this customer | 10 |
| Payment attempted between 01:00-05:00 UTC | 5 |

Score `>= 70` is HIGH (rejected and flagged), `>= 30` is MEDIUM (OTP step-up), otherwise LOW.
Thresholds and weights live under `payflux.fraud.*` in `backend/src/main/resources/application.yml`.

## Not in this phase

Webhooks, settlement, card vault/tokenization, rate limiting, cross-merchant admin tooling and
the real Python/scikit-learn scoring service are intentionally out of scope; `/admin` is a
layout stub.
