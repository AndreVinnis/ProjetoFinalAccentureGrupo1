# Projeto Final — Academia Java | Accenture

> Projeto de conclusão da **Academia Java**, programa de formação técnica promovido pela **Accenture BR**. A entrega final consiste em **dois sistemas integrados** — um **backend** em Java/Spring Boot e um **frontend** em React — desenvolvidos em sprints curtas ao longo de 11 dias.

---

## 🧩 Contexto

A aplicação simula um ecossistema único, com dois domínios que se conversam:

- **E-commerce** — catálogo de produtos, carrinho e fluxo de pedidos.
- **Banking (simplificado)** — contas, saldos fictícios, faturas e cartão de crédito.

A integração entre os dois garante que o **pagamento de um pedido** seja feito por **débito direto** no saldo do usuário dentro do módulo bancário, sem dependência de gateways externos. Notificações por e-mail (confirmações, faturas, etc.) são despachadas de forma **assíncrona** via RabbitMQ.

## 🏗 Arquitetura em dois sistemas

```
.
├── backend/          → API REST em Java 21 / Spring Boot 4
└── frontend/         → SPA em React 19 + TypeScript + Vite
```

### Backend — modular por contexto

O backend é organizado em módulos que se comunicam apenas por *facades* públicas, preservando o isolamento entre contextos:

- `auth` — identidade, login, JWT e roles (`CUSTOMER`, `ECOMMERCE_ADMIN`, `BANKING_ADMIN`).
- `banking` — contas, transações, cartões, faturas e pagamentos.
- `ecommerce` — produtos, carrinho, pedidos e eventos de ciclo de vida.
- `notification` — consumidor RabbitMQ que envia e-mails e registra logs.
- `shared` — configurações transversais (OpenAPI, etc.).

### Frontend

SPA em React consumindo a API REST do backend, com autenticação por token JWT armazenado no cliente e telas separadas para cliente final e administradores de cada contexto.

## 🛠 Stack

**Backend**
- Java 21 · Spring Boot 4.x
- Spring Security + JWT
- Spring Data JPA · H2 (dev)
- RabbitMQ (e-mails assíncronos)
- Maven · JUnit 5 · Mockito

**Frontend**
- React 19 · TypeScript
- Vite
- React Router

## 🚀 Como rodar

**Backend**
```bash
cd backend
./mvnw spring-boot:run
```

**Frontend**
```bash
cd frontend
npm install
npm run dev
```

## 👥 Equipe

- [André Vinícius Barros Macambira](https://github.com/AndreVinnis)
- [Andrey](https://github.com/Aster240)
- [Antônio](https://github.com/antoniohortencio)
- [Cainã](https://github.com/CMouraAraujo)

## 📖 Documentação completa

A wiki técnica detalhada (decisões arquiteturais, diagramas de classe, fluxo de autenticação, contratos das facades) está em [`Documentação/`](./Documentação/Home.md).

---
_⚠️ Projeto em constante atualização — esta capa será expandida conforme o refinamento do sistema._
