# API Contracts

Tài liệu này mô tả các API endpoints, request/response formats, và error codes.

## Base URL

**Development**: `http://localhost:8080`  
**Production**: `https://api.yourdomain.com`

## Authentication

Hầu hết endpoints yêu cầu JWT token trong header:
```http
Authorization: Bearer <token>
```

## Endpoints

### 1. POST /api/auth/register

Đăng ký user mới (local account).

**Request**:
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "newuser@example.com",
  "username": "newuser",
  "password": "SecurePass123"
}
```

**Validation Rules**:
- `email`: Required, valid email format, unique
- `username`: Required, min 3 chars, max 50 chars
- `password`: Required, min 6 chars

**Response (Success - 200 OK)**:
```json
{
  "message": "User registered successfully"
}
```

**Response (Error - 400 Bad Request)**:
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ]
}
```

**Response (Error - 409 Conflict)**:
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Email already exists"
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "username": "newuser",
    "password": "SecurePass123"
  }'
```

---

### 2. POST /api/auth/login

Đăng nhập với email/password (local account).

**Request**:
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@local.dev",
  "password": "User@123"
}
```

**Validation Rules**:
- `email`: Required, valid email format
- `password`: Required

**Response (Success - 200 OK)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkZW1vLWFwcCIsInN1YiI6InVzZXJAbG9jYWwuZGV2Iiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTcwNDEwODAwMCwiZXhwIjoxNzA0MTExNjAwfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
}
```

**Response (Error - 401 Unauthorized)**:
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Bad credentials"
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@local.dev",
    "password": "User@123"
  }'
```

**Response Headers**:
- `Content-Type: application/json`

---

### 3. GET /api/auth/me

Lấy thông tin user hiện tại (từ JWT token).

**Request**:
```http
GET /api/auth/me
Authorization: Bearer <token>
```

**Response (Success - 200 OK)**:
```json
{
  "id": 1,
  "email": "user@local.dev",
  "username": "Regular User",
  "provider": "LOCAL",
  "enabled": true,
  "createdAt": "2024-01-01T10:00:00",
  "roles": ["ROLE_USER"]
}
```

**Response Fields**:
- `id`: User ID (Long)
- `email`: Email address (String)
- `username`: Display name (String)
- `provider`: `LOCAL`, `GOOGLE`, or `MICROSOFT` (String)
- `enabled`: Account enabled status (Boolean)
- `createdAt`: ISO 8601 timestamp (String)
- `roles`: Array of role names (String[])

**Response (Error - 401 Unauthorized)**:
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

**cURL Example**:
```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

---

### 4. GET /api/users

Lấy danh sách tất cả users (yêu cầu authentication với role USER hoặc ADMIN).

**Request**:
```http
GET /api/users
Authorization: Bearer <token>
```

**Response (Success - 200 OK)**:
```json
[
  {
    "id": 1,
    "email": "admin@local.dev",
    "username": "Admin User",
    "provider": "LOCAL",
    "enabled": true,
    "createdAt": "2024-01-01T10:00:00",
    "roles": ["ROLE_ADMIN", "ROLE_USER"]
  },
  {
    "id": 2,
    "email": "user@local.dev",
    "username": "Regular User",
    "provider": "LOCAL",
    "enabled": true,
    "createdAt": "2024-01-01T10:05:00",
    "roles": ["ROLE_USER"]
  },
  {
    "id": 3,
    "email": "user@gmail.com",
    "username": "Google User",
    "provider": "GOOGLE",
    "enabled": true,
    "createdAt": "2024-01-01T11:00:00",
    "roles": ["ROLE_USER"]
  }
]
```

**Response (Error - 401 Unauthorized)**:
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 401,
  "error": "Unauthorized"
}
```

**Response (Error - 403 Forbidden)**:
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied"
}
```

**cURL Example**:
```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN"
```

---

### 5. GET /oauth2/authorization/google

Initiates Google OAuth2 flow. Frontend redirects user đến endpoint này.

**Request**:
```http
GET /oauth2/authorization/google
```

**Response**: 302 Redirect to Google OAuth2 authorization page

**Browser Flow**:
```
User clicks "Continue with Google"
→ Browser redirects to: http://localhost:8080/oauth2/authorization/google
→ Spring Security redirects to Google
→ User authenticates at Google
→ Google redirects back to: http://localhost:8080/login/oauth2/code/google
→ Backend processes → Redirects to frontend: http://localhost:5173/#/oauth/callback#token=...
```

**cURL Example** (for testing redirect):
```bash
curl -L http://localhost:8080/oauth2/authorization/google
```

---

### 6. GET /oauth2/authorization/azure

Initiates Microsoft Azure OAuth2 flow. Tương tự Google flow.

**Request**:
```http
GET /oauth2/authorization/azure
```

**Response**: 302 Redirect to Microsoft Entra ID login page

**Browser Flow**: Tương tự Google flow, redirect về `/login/oauth2/code/azure`

---

## Common Error Codes

| Status Code | Meaning | Example Scenarios |
|-------------|---------|-------------------|
| 200 | OK | Request successful |
| 400 | Bad Request | Validation failed, malformed request |
| 401 | Unauthorized | Missing/invalid JWT token, bad credentials |
| 403 | Forbidden | Valid token but insufficient permissions |
| 409 | Conflict | Email already exists (register) |
| 500 | Internal Server Error | Server-side error |

## Error Response Format

Standard error response:
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register"
}
```

## Swagger/OpenAPI

API documentation available tại:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## Rate Limiting (Future)

Khi implement rate limiting, response headers sẽ có:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 99
X-RateLimit-Reset: 1704110400
```

Rate limit exceeded (429 Too Many Requests):
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded"
}
```

