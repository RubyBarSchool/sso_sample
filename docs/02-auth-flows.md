# Authentication Flows

Tài liệu này mô tả chi tiết các flow đăng nhập trong hệ thống.

## 1. Local Login Flow (Email/Password)

### Sequence Diagram

```mermaid
sequenceDiagram
    participant U as User (Browser)
    participant FE as Frontend (Vue)
    participant BE as Spring Boot API
    participant DB as PostgreSQL
    participant JWT as JWT Service

    U->>FE: Enter email/password
    FE->>FE: Validate form
    FE->>BE: POST /api/auth/login<br/>{email, password}
    BE->>BE: AuthenticationManager.authenticate()
    BE->>DB: SELECT user by email
    DB-->>BE: User entity
    BE->>BE: BCryptPasswordEncoder.matches()
    alt Valid credentials
        BE->>JWT: issueToken(email, roles)
        JWT-->>BE: JWT token string
        BE-->>FE: 200 OK {accessToken: "..."}
        FE->>FE: Store token in localStorage
        FE->>FE: Store token in Pinia store
        FE->>BE: GET /api/auth/me<br/>(Authorization: Bearer token)
        BE->>JWT: parseToken(token)
        BE->>DB: SELECT user by email from token
        DB-->>BE: User data
        BE-->>FE: 200 OK {user profile}
        FE->>FE: Store user in Pinia
        FE->>U: Redirect to / (Home)
    else Invalid credentials
        BE-->>FE: 401 Unauthorized
        FE->>U: Show error message
    end
```

### API Request/Response

**Request**:
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@local.dev",
  "password": "User@123"
}
```

**Response (Success)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (Error)**:
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Bad credentials"
}
```

## 2. Google OIDC Flow

### Sequence Diagram

```mermaid
sequenceDiagram
    participant U as User
    participant FE as Frontend (Vue)
    participant BE as Spring Boot
    participant G as Google IdP
    participant DB as PostgreSQL
    participant JWT as JWT Service

    U->>FE: Click "Continue with Google"
    FE->>FE: window.location.href =<br/>BE_URL + /oauth2/authorization/google
    FE->>BE: GET /oauth2/authorization/google
    BE->>BE: Spring Security OAuth2<br/>initiates PKCE flow
    BE->>G: Redirect to Google OAuth2<br/>with client_id, redirect_uri,<br/>scope=openid profile email
    G->>U: Show Google login page
    U->>G: Enter Google credentials
    G->>G: Authenticate user
    G->>BE: Redirect to<br/>/login/oauth2/code/google<br/>with authorization code
    BE->>G: Exchange code for ID token<br/>(POST /token with client_secret)
    G-->>BE: ID Token (JWT) + access_token
    BE->>BE: OAuth2SuccessHandler.onAuthenticationSuccess()
    BE->>BE: Extract email from ID token:<br/>principal.getAttribute("email")
    BE->>BE: Extract name:<br/>principal.getAttribute("name")
    BE->>DB: SELECT user WHERE email=?
    alt User exists
        DB-->>BE: Existing user
    else User not found
        BE->>DB: INSERT new user<br/>(provider=GOOGLE)
        BE->>DB: INSERT user_role (ROLE_USER)
        DB-->>BE: New user
    end
    BE->>JWT: issueToken(user.email, user.roles)
    JWT-->>BE: JWT token
    BE->>FE: 302 Redirect to<br/>http://localhost:5173/#/oauth/callback#token=...
    FE->>FE: OidcCallback.vue mounted
    FE->>FE: Extract token from URL fragment
    FE->>FE: authStore.setTokenFromCallback(token)
    FE->>BE: GET /api/auth/me (Bearer token)
    BE-->>FE: User profile
    FE->>FE: Store user in Pinia
    FE->>U: Redirect to / (Home)
```

### Google OAuth2 Configuration

**Required Setup**:
1. Tạo OAuth 2.0 Client ID tại [Google Cloud Console](https://console.cloud.google.com/)
2. Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
3. Scopes: `openid`, `profile`, `email`

**ID Token Claims**:
```json
{
  "sub": "123456789",
  "email": "user@gmail.com",
  "name": "John Doe",
  "picture": "https://...",
  "email_verified": true,
  "iat": 1234567890,
  "exp": 1234571490
}
```

## 3. Microsoft Entra ID OIDC Flow

### Sequence Diagram

```mermaid
sequenceDiagram
    participant U as User
    participant FE as Frontend (Vue)
    participant BE as Spring Boot
    participant MS as Microsoft Entra ID
    participant DB as PostgreSQL
    participant JWT as JWT Service

    U->>FE: Click "Continue with Microsoft"
    FE->>FE: window.location.href =<br/>BE_URL + /oauth2/authorization/azure
    FE->>BE: GET /oauth2/authorization/azure
    BE->>BE: Spring Security OAuth2<br/>initiates PKCE flow
    BE->>MS: Redirect to Microsoft login<br/>with tenant_id, client_id,<br/>redirect_uri, scope
    MS->>U: Show Microsoft login page
    U->>MS: Enter Microsoft credentials<br/>(or use existing session)
    MS->>MS: Authenticate user
    MS->>BE: Redirect to<br/>/login/oauth2/code/azure<br/>with authorization code
    BE->>MS: Exchange code for ID token<br/>(POST /token endpoint)
    MS-->>BE: ID Token (JWT) + access_token
    BE->>BE: OAuth2SuccessHandler.onAuthenticationSuccess()
    BE->>BE: Extract email from ID token:<br/>email OR preferred_username
    BE->>BE: Extract name:<br/>name OR email
    BE->>BE: Detect provider: "MICROSOFT"<br/>(from authorities.contains("azure"))
    BE->>DB: SELECT user WHERE email=?
    alt User exists
        DB-->>BE: Existing user
    else User not found
        BE->>DB: INSERT new user<br/>(provider=MICROSOFT)
        BE->>DB: INSERT user_role (ROLE_USER)
        DB-->>BE: New user
    end
    BE->>JWT: issueToken(user.email, user.roles)
    JWT-->>BE: JWT token
    BE->>FE: 302 Redirect to<br/>http://localhost:5173/#/oauth/callback#token=...
    FE->>FE: OidcCallback.vue mounted
    FE->>FE: Extract token from URL fragment
    FE->>FE: authStore.setTokenFromCallback(token)
    FE->>BE: GET /api/auth/me (Bearer token)
    BE-->>FE: User profile
    FE->>FE: Store user in Pinia
    FE->>U: Redirect to / (Home)
```

### Microsoft Azure Configuration

**Required Setup**:
1. Tạo App Registration tại [Azure Portal](https://portal.azure.com/)
2. Configure redirect URI: `http://localhost:8080/login/oauth2/code/azure`
3. Enable ID tokens in Authentication settings
4. Scopes: `openid`, `profile`, `email`, `User.Read`

**Application (client) ID**: Copy từ Azure Portal
**Directory (tenant) ID**: Copy từ Azure Portal
**Client secret**: Tạo trong "Certificates & secrets"

**ID Token Claims**:
```json
{
  "sub": "abc123...",
  "email": "user@company.com",
  "preferred_username": "user@company.com",
  "name": "John Doe",
  "oid": "object-id",
  "tid": "tenant-id",
  "iat": 1234567890,
  "exp": 1234571490
}
```

**Note**: Microsoft có thể dùng `preferred_username` thay vì `email` claim.

## 4. Registration Flow (Local)

### Sequence Diagram

```mermaid
sequenceDiagram
    participant U as User
    participant FE as Frontend
    participant BE as Spring Boot
    participant DB as PostgreSQL

    U->>FE: Fill registration form
    FE->>FE: Validate form (email, username, password)
    FE->>BE: POST /api/auth/register<br/>{email, username, password}
    BE->>BE: @Valid validation
    BE->>DB: SELECT EXISTS(email)
    alt Email exists
        DB-->>BE: true
        BE-->>FE: 409 Conflict / 400 Bad Request
        FE->>U: Show "Email already exists"
    else Email available
        DB-->>BE: false
        BE->>BE: BCryptPasswordEncoder.encode(password)
        BE->>DB: SELECT role WHERE name='ROLE_USER'
        DB-->>BE: ROLE_USER
        BE->>DB: INSERT user<br/>(provider=LOCAL, enabled=true)
        BE->>DB: INSERT user_role
        DB-->>BE: User saved
        BE-->>FE: 200 OK {message: "User registered successfully"}
        FE->>U: Show success, redirect to /login
    end
```

## Error Handling

### OAuth2 Errors

Nếu OAuth2 flow thất bại, `OAuth2AuthenticationFailureHandler` (chưa implement trong code hiện tại) có thể redirect về:
```
/#/login?error=oidc_failed
```

Frontend có thể hiển thị error message tương ứng.

### Common Error Scenarios

1. **Invalid token**: JWT expired hoặc signature invalid → 401 → Redirect to login
2. **Missing token**: Request không có Authorization header → 401
3. **User not found**: Token hợp lệ nhưng user bị xóa → 401
4. **Email conflict**: Đăng ký với email đã tồn tại → 409

