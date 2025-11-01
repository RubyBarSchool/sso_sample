# Security Design

## Tổng quan Security

Hệ thống sử dụng **stateless API architecture** với JWT (JSON Web Tokens) để authentication và authorization.

## Authentication Strategy

### JWT Token-based Authentication

**Quy tắc**:
- Không sử dụng HTTP sessions (sessionless)
- Token được phát hành sau khi authentication thành công (local hoặc OAuth2)
- Token được lưu ở client-side (localStorage)
- Token được gửi trong `Authorization: Bearer <token>` header
- Token có expiration time (mặc định: 1 hour)

**Token Flow**:
```
User Login → Backend issues JWT → Client stores → Client sends in header → Backend validates → Access
```

### Token Storage

**Frontend (localStorage)**:
```typescript
// Store
localStorage.setItem('accessToken', token)

// Retrieve
const token = localStorage.getItem('accessToken')

// Send in request
headers: { Authorization: `Bearer ${token}` }
```

**Lý do localStorage thay vì cookie**:
- SPA không cần cookie cho CSRF protection (stateless)
- Dễ quản lý với JavaScript
- Có thể set expiration theo JWT exp claim

**Rủi ro**: XSS attack có thể đọc localStorage → Mitigation: Content Security Policy, sanitize input

### Refresh Token Strategy (Hiện tại: Simple)

**Current Implementation**: Token hết hạn → User phải login lại

**Mở rộng (Future)**:
```typescript
// Backend issues both accessToken (short-lived) và refreshToken (long-lived)
{
  "accessToken": "eyJ...", // 1 hour
  "refreshToken": "eyJ..."  // 7 days
}

// Frontend refresh flow
if (accessToken expired) {
  POST /api/auth/refresh { refreshToken }
  → Backend validates refreshToken
  → Issues new accessToken
}
```

## Authorization

### Role-Based Access Control (RBAC)

**Roles**:
- `ROLE_USER`: Basic user access
- `ROLE_ADMIN`: Admin access (có thể mở rộng)

**Implementation**:
```java
// Spring Security
@GetMapping("/api/users")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')") // Trong SecurityConfig
```

**Frontend Route Guards**:
```typescript
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return '/login'
  }
})
```

## Password Security

### BCrypt Hashing

**Algorithm**: BCrypt với cost factor 10 (default)

```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashed = encoder.encode("plainPassword");
// Example: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

**Properties**:
- Salt tự động generate (mỗi hash unique)
- Slow by design (resistant to brute force)
- Cost factor có thể tăng (12-14 cho production)

**Validation**:
```java
encoder.matches("plainPassword", hashed) // true/false
```

## CORS Configuration

### Current Setup

```java
cors:
  allowedOrigins: "http://localhost:5173"
```

**Production**:
```yaml
cors:
  allowedOrigins: "https://yourdomain.com,https://www.yourdomain.com"
```

**Headers Allowed**:
- `*` (all headers) - có thể restrict trong production
- `Authorization` (required cho JWT)
- `Content-Type`

**Methods Allowed**: GET, POST, PUT, DELETE, OPTIONS

**Credentials**: `allowCredentials: true` (cho cookies nếu cần)

## OAuth2 Security

### Redirect URI Whitelist

**Google/Microsoft**:
- Phải đăng ký chính xác redirect URI trong provider console
- Không thể dùng wildcard hoặc thay đổi port tùy ý
- Production URI phải là HTTPS

**Backend Redirect**:
```
http://localhost:8080/login/oauth2/code/google
http://localhost:8080/login/oauth2/code/azure
```

**Frontend Callback**:
```
http://localhost:5173/#/oauth/callback
```

### OIDC ID Token Validation

Spring Security OAuth2 Client tự động validate:
- Signature (verify với provider's public key)
- Issuer (`iss` claim)
- Audience (`aud` claim)
- Expiration (`exp` claim)

### Email Mapping

**Strategy**:
```java
String email = principal.getAttribute("email");
if (email == null) {
    email = principal.getAttribute("preferred_username"); // Microsoft fallback
}
```

**Validation**: Email phải unique trong database (unique constraint)

## JWT Security

### Token Structure

**Header**:
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload**:
```json
{
  "iss": "demo-app",
  "sub": "user@example.com",
  "roles": ["ROLE_USER"],
  "iat": 1234567890,
  "exp": 1234571490
}
```

**Signature**: HMAC-SHA256 với secret key

### Secret Key Management

**Current**: Hardcoded trong `application.yaml` (⚠️ NOT for production)

**Production Best Practices**:
1. **Environment Variable**:
   ```yaml
   jwt:
     secret: ${JWT_SECRET}
   ```
   Set `JWT_SECRET` trong environment (Docker, Kubernetes secret, etc.)

2. **Minimum Length**: 256 bits (32 bytes) cho HS256
   ```
   openssl rand -base64 32
   ```

3. **Key Rotation**: Rotate secret định kỳ, support multiple keys trong transition period

4. **Key Storage**: Không commit vào git, dùng secret management (AWS Secrets Manager, HashiCorp Vault, etc.)

### Token Validation

```java
// JwtAuthFilter
try {
    Claims claims = jwtService.parseToken(token);
    String email = claims.getSubject();
    // Verify token not expired (automatic)
    // Verify signature (automatic)
} catch (JwtException e) {
    // Token invalid → Continue without authentication
}
```

**Clock Skew**: JJWT mặc định có 60s clock skew tolerance

## HTTPS & Transport Security

### Development (localhost)

- HTTP cho local development (acceptable)
- Không expose sensitive data qua network

### Production (Required)

**Must Use HTTPS**:
1. OAuth2 redirect URIs phải là HTTPS
2. JWT tokens không bị intercept qua network
3. Prevent man-in-the-middle attacks

**Setup**:
- Reverse proxy (Nginx, Traefik) với Let's Encrypt
- Hoặc load balancer với SSL termination

## Rate Limiting (Gợi ý)

**Current**: Chưa implement

**Recommendation**: 
- Use Spring Boot Actuator + Resilience4j
- Hoặc Spring Security Rate Limiter
- Hoặc external service (Redis-based rate limiting)

**Endpoints to protect**:
- `/api/auth/login`: 5 requests/minute per IP
- `/api/auth/register`: 3 requests/hour per IP
- `/oauth2/authorization/*`: 10 requests/minute per IP

## Security Headers

### Recommended Headers (Production)

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http.headers(headers -> headers
        .contentSecurityPolicy("default-src 'self'")
        .frameOptions(FrameOptionsConfig::deny)
        .httpStrictTransportSecurity(hsts -> hsts
            .maxAgeInSeconds(31536000)
            .includeSubdomains(true)
        )
    );
    return http.build();
}
```

**Headers**:
- `Content-Security-Policy`: Prevent XSS
- `X-Frame-Options: DENY`: Prevent clickjacking
- `Strict-Transport-Security`: Force HTTPS
- `X-Content-Type-Options: nosniff`: Prevent MIME sniffing

## Vulnerability Mitigation

### 1. XSS (Cross-Site Scripting)

**Risk**: Attacker inject script → Steal localStorage token

**Mitigation**:
- Input sanitization (Vue tự động escape trong templates)
- Content Security Policy headers
- Avoid `innerHTML` với user input
- Use `v-html` cẩn thận

### 2. CSRF (Cross-Site Request Forgery)

**Current**: Disabled (vì stateless với JWT)

**Risk**: Low (vì không dùng cookie-based sessions)

**If needed**: Implement CSRF token cho state-changing operations

### 3. Token Leakage

**Risk**: Token exposed trong logs, URL, error messages

**Mitigation**:
- Không log token trong application logs
- Token trong URL fragment (không gửi lên server trong HTTP request)
- Error messages không expose token

### 4. Brute Force Attack

**Risk**: Attacker thử nhiều passwords

**Mitigation**:
- Rate limiting trên `/api/auth/login`
- Account lockout sau N failed attempts (chưa implement)
- CAPTCHA sau N attempts (chưa implement)

### 5. SQL Injection

**Risk**: Low (dùng JPA/Hibernate với prepared statements)

**Mitigation**:
- Always use parameterized queries
- Avoid native queries với string concatenation
- Validate input với `@Valid`

### 6. OAuth2 Redirect URI Manipulation

**Risk**: Attacker thay đổi redirect URI → Steal authorization code

**Mitigation**:
- Provider (Google/Microsoft) validate redirect URI phải match registered URI
- Backend không dùng redirect URI từ request parameter

## Security Checklist

- [x] Password hashing (BCrypt)
- [x] JWT token expiration
- [x] CORS configuration
- [x] Input validation (@Valid)
- [x] Role-based authorization
- [x] HTTPS-ready (production)
- [ ] Rate limiting
- [ ] Account lockout
- [ ] Security headers (CSP, HSTS)
- [ ] Secret key rotation
- [ ] Token refresh mechanism
- [ ] Audit logging

