# Ticket_Hub_Backend

Spring Boot REST API for the Ticket Hub movie ticket booking system.

## Stack

- Java 21, Spring Boot 4
- MySQL, Spring Data JPA
- Clerk JWT auth, Razorpay payments, Gmail SMTP

## Local run

1. Copy `src/main/resources/application-local.properties.example` to `application-local.properties` and fill in secrets.
2. Start MySQL with database `tickethub`.
3. Run: `.\mvnw.cmd spring-boot:run`

API runs at `http://localhost:8080`.

## Deploy on Render

1. Push this repo to GitHub.
2. [render.com](https://render.com) → **New** → **Blueprint** → connect `Ticket_Hub_Backend`.
3. Render creates the web service from `render.yaml` (Docker build).
4. Add a **MySQL** database elsewhere (Render only offers PostgreSQL). Easiest: [Railway](https://railway.app) → New → **MySQL only** → copy host/user/password.
5. In Render → **tickethub-api** → **Environment**, set:

| Variable | Example |
|----------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://HOST:PORT/railway?useSSL=true&allowPublicKeyRetrieval=true` |
| `SPRING_DATASOURCE_USERNAME` | from MySQL provider |
| `SPRING_DATASOURCE_PASSWORD` | from MySQL provider |
| `CORS_ALLOWED_ORIGINS` | `https://your-frontend.vercel.app` |
| `SPRING_MAIL_USERNAME` | Gmail address |
| `SPRING_MAIL_PASSWORD` | Gmail app password |
| `RAZORPAY_API_KEY` | Razorpay key |
| `RAZORPAY_API_SECRET` | Razorpay secret |

6. **Manual Deploy** or wait for auto-deploy. API URL: `https://tickethub-api.onrender.com` (name may vary).

Health check: `GET /health`
