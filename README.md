# SSO Google & Microsoft - Monorepo

Full-stack monorepo application với authentication qua OAuth2 (Google & Microsoft) và local login.

## Tính năng

- ✅ Local authentication (email/password với JWT)
- ✅ OAuth2 Login với Google (OpenID Connect)
- ✅ OAuth2 Login với Microsoft Entra ID
- ✅ User management với roles (ADMIN, USER)
- ✅ RESTful API với Spring Boot
- ✅ Vue 3 SPA với TypeScript
- ✅ PostgreSQL database
- ✅ Docker Compose để chạy toàn bộ stack

## Cấu trúc

```
.
├── backend/          # Spring Boot 3.1.x + Security + OAuth2
├── frontend/         # Vue 3 + Vite + TypeScript + Pinia
├── docs/             # Tài liệu kiến trúc và hướng dẫn
└── docker-compose.yaml
```

## Quick Start

### 1. Cấu hình Backend

```bash
# Copy và chỉnh sửa file cấu hình
cp backend/src/main/resources/application-example.yaml backend/src/main/resources/application.yaml
```

Chỉnh sửa `backend/src/main/resources/application.yaml`:
- Điền `client-id` và `client-secret` cho Google OAuth2
- Điền `client-id`, `client-secret` và `tenant-id` cho Microsoft Azure
- Kiểm tra database credentials

### 2. Cấu hình Frontend

```bash
# Copy file môi trường
cp frontend/.env.example frontend/.env
```

Kiểm tra `frontend/.env` - mặc định là `VITE_API_BASE_URL=http://localhost:8080`

### 3. Chạy với Docker Compose

```bash
docker compose up -d --build
```

Sau khi build xong, truy cập:
- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html

### 4. Tài khoản mặc định

Sau khi chạy lần đầu, database sẽ được seed với:
- **Admin**: `admin@local.dev` / `Admin@123`
- **User**: `user@local.dev` / `User@123`

## OAuth2 Redirect URIs

Khi cấu hình OAuth2 providers, sử dụng các redirect URIs sau:

### Google
```
http://localhost:8080/login/oauth2/code/google
```

### Microsoft Azure
```
http://localhost:8080/login/oauth2/code/azure
```

## Phát triển Local

### Backend
```bash
cd backend
./mvnw spring-boot:run
# Hoặc nếu đã có Maven global
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

### Database
```bash
docker compose up postgres -d
```

## Tài liệu

Xem chi tiết trong thư mục `docs/`:
- `01-architecture-overview.md` - Tổng quan kiến trúc
- `02-auth-flows.md` - Flow đăng nhập chi tiết
- `03-security-design.md` - Thiết kế bảo mật
- `04-api-contracts.md` - API documentation
- `05-local-setup-and-troubleshooting.md` - Setup và troubleshooting

## Tech Stack

### Backend
- Spring Boot 3.1.x
- Spring Security 6.3
- Spring Data JPA
- PostgreSQL
- OAuth2 Client (Google & Microsoft)
- JWT (JJWT)
- Flyway
- OpenAPI/Swagger

### Frontend
- Vue 3 (Composition API)
- TypeScript
- Vite
- Pinia
- Vue Router
- Axios

## License

MIT

