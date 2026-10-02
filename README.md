# Autopay Recovery Agent

**PayFlow Automated Payment Recovery** is a local-development demo of a voice-assisted workflow for helping customers resolve failed recurring payments. It combines a Spring Boot API, PostgreSQL persistence, a Vapi integration boundary, and a React dashboard for inspecting fictional demo activity.

> This repository is an assignment/demo project. Customer records, payment attempts, and payment links are fictional. Payment processing is mocked; no real payment is collected.

## Problem statement

Failed recurring payments can leave customers uncertain about what happened and what to do next. Support teams need a consistent way to check a payment, offer a retry or payment link, arrange a callback, and record an escalation or outcome. This project demonstrates that workflow through a voice-agent tool layer and a simple dashboard, with business operations kept in the backend.

## Key features

- Spring Boot REST API backed by PostgreSQL and JPA.
- Seeded fictional customer records for local demonstrations.
- Mock payment status, retry, and payment-link operations.
- Vapi client integration for outbound calls and call webhooks.
- Seven Java agent tools exposed through validated HTTP adapter routes.
- Read APIs for calls, recovery activity, and database-derived dashboard metrics.
- React + TypeScript + Vite + Tailwind dashboard with backend health, customer records, call history, recovery activity, and a start-call action.
- Structured, bounded errors for Vapi tool requests; no authentication layer is enabled in this development demo.

## Architecture and request flow

```text
PayFlow React dashboard ──HTTP/JSON──> Spring Boot controllers
                                          │
Vapi API Request tools ─HTTP/JSON─────────┤
                                          ▼
                           Agent tools / application services
                                          │
                                          ▼
                              Spring Data JPA repositories
                                          │
                                          ▼
                                     PostgreSQL

Outbound-call path: Dashboard → outbound-call controller → customer/call services
                    → VapiService → Vapi API
Vapi call events:   Vapi → /api/webhooks/vapi → VapiWebhookService → CallService → PostgreSQL
```

Controllers validate/route HTTP requests. Services contain application behavior and delegate persistence to repositories. Agent tool components adapt existing services into small callable operations; the HTTP adapter validates tool arguments and delegates to those components rather than duplicating business logic. Dashboard response DTOs expose only the fields the UI needs.

## Voice-agent flow with Vapi

The Vapi assistant configuration, system prompt, API Request tool schemas, and demo conversation scenarios are documented in [`docs/vapi-agent.md`](docs/vapi-agent.md).

1. The assistant identifies itself as automated and confirms that it is a suitable time to speak.
2. It obtains the customer ID from trusted call context and loads demo customer details when needed.
3. It checks payment status before any retry and asks permission before attempting one.
4. It can offer the mock payment link, schedule a callback, or record a support escalation.
5. It records the final recovery outcome and confirms only what the backend reports.
6. Vapi call lifecycle events are sent to `POST /api/webhooks/vapi` to update the call record where supported.

The assistant must not request payment credentials, imply that it is human, pressure a customer who refuses, or claim payment success unless the backend confirms it. Use Vapi **API Request** tools for the direct JSON routes in this project; Vapi Function tools use a different webhook envelope.

## Backend APIs

All routes are relative to `http://localhost:8080` by default.

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/health` | Simple application health response. |
| `GET` | `/api/customers` | List fictional customers. |
| `GET` | `/api/customers/{id}` | Get a customer by ID. |
| `GET` | `/api/customers/phone/{phone}` | Find a customer by phone. |
| `GET` | `/api/payments/{customerId}` | Read mock payment status. |
| `POST` | `/api/payments/{customerId}/retry` | Perform a mock payment retry. |
| `POST` | `/api/payments/{customerId}/link` | Generate a fictional payment link. |
| `POST` | `/api/calls/outbound/{customerId}` | Create a call record and request an outbound Vapi call. |
| `GET` | `/api/calls` | Return recent call summaries for the dashboard. |
| `GET` | `/api/recovery-actions` | Return recent recovery actions. |
| `GET` | `/api/metrics` | Calculate dashboard metrics from persisted calls/actions. |
| `POST` | `/api/tools/vapi/get_customer_details` | Invoke `CustomerTool.getCustomerDetails`. |
| `POST` | `/api/tools/vapi/check_payment_status` | Invoke `PaymentTool.checkPaymentStatus`. |
| `POST` | `/api/tools/vapi/retry_payment` | Invoke `PaymentTool.retryPayment`. |
| `POST` | `/api/tools/vapi/send_payment_link` | Invoke `PaymentTool.sendPaymentLink`. |
| `POST` | `/api/tools/vapi/schedule_callback` | Invoke `RecoveryTool.scheduleCallback`. |
| `POST` | `/api/tools/vapi/record_recovery_outcome` | Invoke `RecoveryTool.recordRecoveryOutcome`. |
| `POST` | `/api/tools/vapi/escalate_to_support` | Invoke `SupportTool.escalateToSupport`. |
| `POST` | `/api/webhooks/vapi` | Accept supported Vapi call lifecycle events. |

Agent tool routes accept direct JSON arguments, for example `{"customerId":1}`. Callback scheduling also requires an ISO local `callbackTime`; outcome recording requires a supported `outcome`; escalation requires a non-empty `reason`. Successful calls return JSON DTOs. Invalid arguments and unknown tools return HTTP 400 JSON; missing customers return HTTP 404; unexpected tool failures return a generic HTTP 500 JSON response.

## Agent tools

| Tool | Responsibility |
|---|---|
| `CustomerTool.getCustomerDetails(customerId)` | Return the customer’s demo account and payment context. |
| `PaymentTool.checkPaymentStatus(customerId)` | Read current mock status and failure reason. |
| `PaymentTool.retryPayment(customerId)` | Attempt a mock retry using the existing payment service. |
| `PaymentTool.sendPaymentLink(customerId)` | Generate a fictional checkout link. |
| `RecoveryTool.scheduleCallback(customerId, callbackTime)` | Record a requested callback. |
| `RecoveryTool.recordRecoveryOutcome(customerId, outcome)` | Persist a recovery outcome and associate it with the latest eligible call. |
| `SupportTool.escalateToSupport(customerId, reason)` | Record a support escalation; this does not transfer the call to a live agent. |

## Technology stack

- **Backend:** Java 21, Spring Boot, Spring MVC, Spring Data JPA, Hibernate, Maven.
- **Database:** PostgreSQL 16, Docker Compose for local development.
- **Voice integration:** Vapi REST API through Spring `RestClient`, outbound-call and webhook integration.
- **Frontend:** React, TypeScript, Vite, Tailwind CSS.
- **Tests:** JUnit 5, Spring Boot test support, MockMvc, Mockito.

## Database overview

The application uses three JPA entities/tables with foreign keys to `customers`:

- **`customers`** stores a fictional customer name/phone, amount due, payment status, failure reason, and creation time.
- **`calls`** stores the customer relationship, Vapi call ID, start/end timestamps, duration, call status, and optional recovery outcome.
- **`recovery_actions`** stores customer-linked callback, support escalation, and recorded outcome activity, including status, optional callback time/details, and creation time.

The dashboard endpoints return summary DTOs rather than exposing full JPA entities. Hibernate is configured with `spring.jpa.hibernate.ddl-auto=update` for this local demo; no migration framework is currently included. Database credentials are provided through environment variables and are not part of the schema or source code.

## Dashboard

The dashboard is served by Vite at `http://localhost:5173` in development. It shows:

- Backend/system health from `GET /api/health`.
- Total calls, completed calls, recovery rate, average call duration, payment links sent, and escalations from `GET /api/metrics`.
- Customer name, amount due, payment status, and failure reason from `GET /api/customers`.
- Recent calls with status, outcome, duration, and start time from `GET /api/calls`.
- Recent recorded recovery actions from `GET /api/recovery-actions`.
- A **Start recovery call** action backed by `POST /api/calls/outbound/{customerId}`, with loading/success/error feedback.

Metric definitions: recovery rate is calls with outcome `RECOVERED` among calls with status `COMPLETED` (zero when no calls completed); average duration is the rounded mean of calls with a recorded duration; payment links sent count recovery actions whose status is `PAYMENT_LINK_SENT`; escalations count actions of type `SUPPORT_ESCALATION`.

The backend allows browser CORS requests from `http://localhost:5173` and `http://127.0.0.1:5173` by default. Set `DASHBOARD_CORS_ALLOWED_ORIGINS` to change the permitted local dashboard origins.

## Safety and privacy assumptions

- All seeded names, phone numbers, payment statuses, and activity are fictional demo data.
- Payment attempts are simulated locally. Generated checkout URLs use a reserved demo domain and do not process payment.
- The voice agent must never request card numbers, CVV, PINs, passwords, bank credentials, or one-time passcodes.
- Vapi API keys and database credentials belong in an ignored local `.env` file or a deployment secret store. Never commit them or paste them into prompts, tool arguments, or logs.
- No Spring Security, authentication, authorization, rate limiting, or production privacy controls are configured. Keep the service bound to a trusted development environment.
- Do not use real customer information or enable outbound calling to real phone numbers for this assignment demo.

## Local setup and environment variables

Prerequisites: JDK 21, Docker Desktop with Docker Compose, and Node.js/npm compatible with the locked Vite toolchain.

Create a local environment file from the safe template:

```powershell
Copy-Item .env.example .env
```

The root `.env.example` lists the supported variables without real credentials:

| Variable | Purpose |
|---|---|
| `DB_NAME` | Local PostgreSQL database name. |
| `DB_PORT` | Host port published by Docker Compose and used by Spring Boot. |
| `DB_USERNAME` | Local PostgreSQL username. |
| `DB_PASSWORD` | Local PostgreSQL password; replace the development placeholder locally. |
| `VAPI_API_KEY` | Optional Vapi server API key for outbound call creation. Keep the real value secret. |
| `VAPI_BASE_URL` | Vapi API base URL. |
| `VAPI_ASSISTANT_ID` | Assistant identifier used for outbound calls. |
| `VAPI_PHONE_NUMBER_ID` | Vapi phone-number identifier used for outbound calls. |
| `DASHBOARD_CORS_ALLOWED_ORIGINS` | Optional comma-separated browser origins; defaults to the two local Vite origins. |

The frontend may use `frontend/.env` copied from `frontend/.env.example` to set `VITE_API_BASE_URL`; its local default is `http://localhost:8080`. Vite variables are public browser configuration, not a place for secrets.

## Run the backend and frontend

From the repository root, start PostgreSQL and the backend:

```powershell
docker compose up -d postgres
docker compose ps
.\mvnw.cmd clean package
java -jar target/autopay-recovery-0.0.1-SNAPSHOT.jar
```

In a second PowerShell window:

```powershell
cd frontend
Copy-Item .env.example .env
npm ci
npm run dev
```

Open the Vite URL shown in the terminal (normally `http://localhost:5173`). The API is available at `http://localhost:8080`. Verify the backend with:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/api/customers
Invoke-RestMethod http://localhost:8080/api/metrics
```

Outbound Vapi calling requires valid Vapi configuration and a reachable Vapi assistant/phone number. A Vapi-hosted service cannot reach a developer’s `localhost` URL; configure an HTTPS-reachable backend endpoint before attempting a real Vapi call. No tunnel or cloud deployment is included here.

## Testing

Backend tests and package build:

```powershell
.\mvnw.cmd clean package
```

Frontend production build:

```powershell
cd frontend
npm ci
npm run build
```

Backend tests cover application startup, recovery services, Vapi webhook handling, health response, dashboard metric calculation, and Vapi tool input/error handling. No live Vapi call is part of the automated test suite.

## Known limitations

- Payment retries return a randomized mock result (`SUCCESS`, `FAILED`, or `PENDING`); no bank or payment provider is contacted.
- A generated payment link is fictional and is not payable.
- The outbound-call UI requires working Vapi credentials and phone/assistant configuration; there is no local voice simulation.
- The Vapi webhook handles the call event shapes currently modeled by the backend; it is not a general Vapi event processor.
- Dashboard calls/actions are unpaginated demo lists; metrics are calculated over the available database records.
- Customer verification, identity checks, authentication, authorization, and live human handoff are not implemented.
- The local PostgreSQL compose volume persists demo data across application restarts.

## Production improvements

- Add authentication, authorization, rate limiting, audit controls, and secure service-to-service credentials.
- Replace the mock payment component with a compliant payment-provider integration and a verified payment status source.
- Add consent management, customer verification, opt-out handling, retention rules, and protections for sensitive data.
- Use database migrations, validated configuration, production secrets management, and separate development/test/production environments.
- Add pagination, filtering, indexes, query-level metric aggregation, observability, and operational alerts.
- Complete Vapi webhook signature validation, idempotency, delivery retry handling, and robust event mapping.
- Add integration tests for PostgreSQL, contract tests for Vapi/API payloads, and frontend component/end-to-end tests.
- Configure trusted HTTPS hosting and a reviewed Vapi-accessible backend endpoint before enabling real calls.
