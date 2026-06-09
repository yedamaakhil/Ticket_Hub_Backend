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

## Deploy

Uses Docker. See `Dockerfile`, `docker-compose.yml`, and `render.yaml`. Set env vars from `.env.example`.
