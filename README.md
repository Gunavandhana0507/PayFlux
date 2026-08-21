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
- [Team](#team)

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
