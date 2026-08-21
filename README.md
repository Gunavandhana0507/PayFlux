```markdown
# PayFlux

**A full-stack payment gateway prototype with explainable, ML-based fraud-risk detection.**

PayFlux is an academic Project-Based Learning (PBL) project that demonstrates how a modern payment gateway works end to end — from merchant order creation through simulated payment processing, fraud-risk scoring, refunds, and merchant analytics. It is a prototype built for learning purposes and does not process real financial transactions.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [What Makes PayFlux Different](#what-makes-payflux-different)
- [Tech Stack](#tech-stack)
- [Project Status](#project-status)
- [Repository Structure](#repository-structure)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Documentation](#documentation)
- [Scope & Disclaimer](#scope--disclaimer)
- [Roadmap](#roadmap)

---

## Overview

PayFlux supports three user roles:

- **Merchant** — registers with KYC details, creates payment orders, monitors transactions and refunds, and reviews fraud alerts.
- **Customer** — opens a payment page for an order, selects a payment method, and completes a simulated payment.
- **Admin** — platform-level oversight of merchants, transactions, and fraud alerts across the system (in progress).

Every payment attempt is scored for fraud risk (Low / Medium / High) before it is authorized. Unlike a plain risk-score display, PayFlux shows *why* a transaction was flagged and lets merchants feed their own judgment back into the system — this is the project's primary differentiator.

## Key Features

- Merchant onboarding with KYC fields and JWT authentication
- Order creation, listing, retrieval, auto-expiry, and idempotent creation
- Simulated payments across Card, UPI, Net Banking, and Wallet methods
- Fraud-risk scoring with Low / Medium / High classification
- A defined payment state machine governing all status transitions
- Full and partial refunds
- Merchant, Customer, and Admin-facing UI surfaces

## What Makes PayFlux Different

- **Explainable risk output** — flagged transactions show the specific contributing factors behind the score (e.g. unusually high amount for this customer, multiple recent failed attempts, new device) instead of a bare number or color badge.
- **Merchant feedback loop** — merchants can mark a flagged transaction as "Confirmed Fraud" or "False Positive." This feedback is logged against the transaction's original feature set, laying the groundwork for the fraud model to be retrained on each merchant's own corrections rather than staying static.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, TypeScript, Tailwind CSS, React Router, Recharts, Axios |
| Backend | Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA |
| Database | MySQL 8.0 |
| Fraud Detection | Rule-based scoring engine (Phase 1 placeholder); Python/Scikit-learn model planned |
| Tooling | Maven, Docker Compose, Git/GitHub, Postman |

## Project Status

This project is under active development.

- [ ] **Frontend + Backend Phase 1** — scaffolding, design system, auth, orders, payments, refunds, rule-based fraud stub with explainability, merchant feedback endpoint
- [ ] **Backend Phase 2** — webhooks, settlement
- [ ] **Backend Phase 3** — card vault/tokenization, full admin dashboard, rate limiting
- [ ] **ML fraud model** — real Scikit-learn model trained on a transaction dataset, retrained using merchant feedback

## Repository Structure

```
PayFlux/
├── frontend/           # React + Vite frontend (customer, merchant, and admin UIs)
│   ├── src/
│   │   ├── components/ui/   # Shared design-system components
│   │   ├── lib/api.ts       # Axios client
│   │   └── ...
│   └── package.json
├── backend/             # Spring Boot backend
│   ├── src/
│   └── pom.xml
├── docs/                 # SRS, ER diagram, DFDs, and other project documentation
├── docker-compose.yml    # Local MySQL for backend development
└── README.md
```

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Java 17+ and Maven
- Docker (for local MySQL)
- Git

### Run the backend

```bash
# Start MySQL
docker-compose up -d

# Run the backend
cd backend
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

### Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The app runs at `http://localhost:5173` and calls the backend directly.

## Environment Variables

| Variable | Location | Description |
|---|---|---|
| `VITE_API_URL` | `frontend/.env` | Base URL of the backend API (e.g. `http://localhost:8080`) |
| `SPRING_DATASOURCE_URL` | `backend/src/main/resources/application.yml` | MySQL connection string (defaults to the local Docker Compose instance) |
| `JWT_SECRET` | `backend/src/main/resources/application.yml` | Secret used to sign JWTs (set your own value for local development) |

## Documentation

- **Software Requirements Specification (SRS)** — full functional and non-functional requirements, IEEE-format
- **Literature Review** — survey of fraud-detection features across payment gateways in use in India, and the research gap PayFlux addresses
- **Entity-Relationship Diagram** — database schema
- **Data Flow Diagrams (Level 0 & 1)** — system-level and process-level data flow
- **System Architecture Diagram** — layer-by-layer component breakdown
- **Payment State Machine, Activity, and Sequence Diagrams** — payment and refund lifecycle detail

All of the above live in `/docs`.

## Scope & Disclaimer

PayFlux is built strictly for academic demonstration:

- All payments are **simulated** — no real card numbers, CVVs, UPI PINs, or bank credentials are ever collected or stored.
- The system does **not** connect to real banking or card-network infrastructure.
- The current fraud-detection engine is a rule-based placeholder; it produces a risk score and explanatory factors but does not use a trained ML model yet, and does not claim to determine, with certainty, whether any transaction is fraudulent.
- Security- and compliance-oriented design choices (JWT auth, planned tokenization, planned HMAC-signed webhooks) are implemented as design-discipline exercises appropriate to an academic prototype, not as certified production controls.

## Roadmap

- [ ] Complete backend Phases 2–3
- [ ] Replace the rule-based fraud stub with a trained Scikit-learn model
- [ ] Use logged merchant feedback (Confirmed Fraud / False Positive) to retrain the fraud model
- [ ] Database Design and System Design documents
- [ ] End-to-end testing and demo preparation
