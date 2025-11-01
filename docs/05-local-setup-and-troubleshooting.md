# Local Setup và Troubleshooting

## Local Setup

### Prerequisites

- **Java 17** (JDK)
- **Node.js 20+** và npm
- **Docker** và Docker Compose (optional, để chạy PostgreSQL)
- **Google Cloud Console** account (cho Google OAuth2)
- **Microsoft Azure Portal** account (cho Microsoft OAuth2)

### Bước 1: Clone và Setup Backend

```bash
cd backend
```

#### 1.1. Copy và cấu hình application.yaml

```bash
cp src/main/resources/application-example.yaml src/main/resources/application.yaml
```

Chỉnh sửa `src/main/resources/application.yaml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID
            client-secret: YOUR_GOOGLE_CLIENT_SECRET
          azure:
            client-id: YOUR_AZURE_CLIENT_ID
            client-secret: YOUR_AZURE_CLIENT_SECRET
        provider:
          azure:
            issuer-uri: https://login.microsoftonline.com/YOUR_TENANT_ID/v2.0

jwt:
  secret: "your-very-strong-secret-key-at-least-256-bits-long"
```

**Lấy Google OAuth2 Credentials**:
1. Truy cập [Google Cloud Console](https://console.cloud.google.com/)
2. Tạo project mới hoặc chọn project hiện tại
3. Enable "Google+ API" hoặc "Google Identity"
4. Tạo OAuth 2.0 Client ID:
   - Application type: Web application
   - Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google`
5. Copy Client ID và Client Secret

**Lấy Microsoft Azure Credentials**:
1. Truy cập [Azure Portal](https://portal.azure.com/)
2. Azure Active Directory → App registrations → New registration
3. Name: "SSO App"
4. Supported account types: Accounts in any organizational directory
5. Redirect URI: Web → `http://localhost:8080/login/oauth2/code/azure`
6. Register → Copy Application (client) ID và Directory (tenant) ID
7. Certificates & secrets → New client secret → Copy value (chỉ hiện 1 lần!)

#### 1.2. Setup Database

**Option A: Docker Compose (Recommended)**
```bash
# Từ root directory
docker compose up postgres -d
```

**Option B: Local PostgreSQL**
```bash
# Ubuntu/Debian
sudo apt-get install postgresql
sudo -u postgres createdb appdb
sudo -u postgres createuser appuser
sudo -u postgres psql -c "ALTER USER appuser WITH PASSWORD 'apppass';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE appdb TO appuser;"
```

#### 1.3. Build và Run Backend

```bash
# Với Maven
./mvnw clean install
./mvnw spring-boot:run

# Hoặc nếu đã có Maven global
mvn clean install
mvn spring-boot:run
```

Backend sẽ chạy tại: http://localhost:8080

### Bước 2: Setup Frontend

```bash
cd frontend
```

#### 2.1. Install Dependencies

```bash
npm install
```

#### 2.2. Copy .env

```bash
cp .env.example .env
```

Kiểm tra `frontend/.env`:
```
VITE_API_BASE_URL=http://localhost:8080
```

#### 2.3. Run Frontend

```bash
npm run dev
```

Frontend sẽ chạy tại: http://localhost:5173

### Bước 3: Chạy với Docker Compose (Full Stack)

```bash
# Từ root directory
# Đảm bảo đã cấu hình application.yaml và .env
docker compose up -d --build
```

Kiểm tra logs:
```bash
docker compose logs -f backend
docker compose logs -f frontend
```

### Bước 4: Test

1. **Test Local Login**:
   - Truy cập http://localhost:5173
   - Click "Register" → Tạo account mới
   - Hoặc login với: `user@local.dev` / `User@123`

2. **Test Google OAuth2**:
   - Click "Continue with Google"
   - Đăng nhập với Google account
   - Kiểm tra redirect về frontend và token được lưu

3. **Test Microsoft OAuth2**:
   - Click "Continue with Microsoft"
   - Đăng nhập với Microsoft account
   - Kiểm tra redirect về frontend và token được lưu

4. **Test Protected Endpoint**:
   - Sau khi login, truy cập `/users` page
   - Kiểm tra danh sách users được hiển thị

## Troubleshooting

### 1. Backend không start

**Lỗi**: `Port 8080 already in use`

**Giải pháp**:
```bash
# Tìm process đang dùng port 8080
lsof -i :8080  # Linux/Mac
netstat -ano | findstr :8080  # Windows

# Kill process
kill -9 <PID>
```

Hoặc đổi port trong `application.yaml`:
```yaml
server:
  port: 8081
```

---

### 2. Database Connection Error

**Lỗi**: `Connection refused` hoặc `Connection timed out`

**Kiểm tra**:
```bash
# Kiểm tra PostgreSQL đang chạy
docker compose ps postgres

# Hoặc
sudo systemctl status postgresql

# Test connection
psql -h localhost -U appuser -d appdb
```

**Giải pháp**:
- Đảm bảo PostgreSQL đang chạy
- Kiểm tra credentials trong `application.yaml`
- Kiểm tra firewall rules

---

### 3. Google OAuth2 Redirect URI Mismatch

**Lỗi**: `redirect_uri_mismatch`

**Nguyên nhân**: Redirect URI trong Google Console không khớp với backend

**Giải pháp**:
1. Vào Google Cloud Console → OAuth 2.0 Client IDs
2. Kiểm tra "Authorized redirect URIs":
   - Phải có: `http://localhost:8080/login/oauth2/code/google`
   - Phải chính xác (không trailing slash, không port khác)

**Lưu ý**:
- Google không cho phép wildcard trong redirect URI
- URI phải match chính xác (case-sensitive)

---

### 4. Microsoft OAuth2 Error

**Lỗi**: `AADSTS50011: The reply URL specified in the request does not match`

**Giải pháp**:
1. Vào Azure Portal → App registrations → Your app → Authentication
2. Kiểm tra "Redirect URIs":
   - Phải có: `http://localhost:8080/login/oauth2/code/azure`
   - Platform: Web

**Lỗi**: `AADSTS700016: Application not found`

**Nguyên nhân**: Client ID hoặc Tenant ID sai

**Giải pháp**:
- Copy lại Application (client) ID từ Azure Portal
- Copy lại Directory (tenant) ID
- Kiểm tra `issuer-uri` trong `application.yaml`:
  ```
  https://login.microsoftonline.com/YOUR_TENANT_ID/v2.0
  ```

---

### 5. JWT Token Invalid/Expired

**Lỗi**: `401 Unauthorized` khi gọi API

**Kiểm tra**:
```javascript
// Trong browser console
console.log(localStorage.getItem('accessToken'))

// Decode token (không verify signature)
const token = localStorage.getItem('accessToken')
const payload = JSON.parse(atob(token.split('.')[1]))
console.log(payload) // Check exp, roles, etc.
```

**Giải pháp**:
- Token hết hạn → Login lại
- Token không có trong localStorage → Login lại
- Token signature invalid → Kiểm tra JWT secret trong backend

---

### 6. CORS Error

**Lỗi**: `Access to XMLHttpRequest has been blocked by CORS policy`

**Kiểm tra**:
```yaml
# backend/src/main/resources/application.yaml
cors:
  allowedOrigins: "http://localhost:5173"
```

**Giải pháp**:
- Đảm bảo `allowedOrigins` chứa frontend URL (chính xác, không trailing slash)
- Nếu frontend chạy port khác (ví dụ 3000), update `allowedOrigins`
- Kiểm tra frontend `.env`: `VITE_API_BASE_URL=http://localhost:8080`

**Lỗi thường gặp**:
- Thêm trailing slash: `http://localhost:5173/` ❌
- Thiếu http://: `localhost:5173` ❌
- Port sai: `http://localhost:3000` ❌

---

### 7. Frontend không gọi được API

**Kiểm tra**:
```bash
# Test API từ terminal
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Giải pháp**:
- Kiểm tra backend đang chạy: http://localhost:8080/swagger-ui.html
- Kiểm tra `VITE_API_BASE_URL` trong `.env`
- Kiểm tra browser console → Network tab → Xem request URL

---

### 8. Flyway Migration Failed

**Lỗi**: `FlywayException: Validate failed`

**Nguyên nhân**: Schema đã tồn tại nhưng không match với migration scripts

**Giải pháp**:
```sql
-- Option 1: Drop và recreate database
DROP DATABASE appdb;
CREATE DATABASE appdb;

-- Option 2: Clean Flyway history (không khuyến khích)
DELETE FROM flyway_schema_history;
```

Hoặc trong `application.yaml`:
```yaml
spring:
  flyway:
    enabled: false  # Tạm thời disable
```

---

### 9. Password không match

**Lỗi**: `Bad credentials` mặc dù password đúng

**Nguyên nhân**: Password trong database chưa được hash

**Giải pháp**:
- Đăng ký user mới (tự động hash)
- Hoặc seed lại database với DataSeeder

---

### 10. Clock Skew Error (JWT)

**Lỗi**: `JwtException: JWT expired` mặc dù token vừa tạo

**Nguyên nhân**: System clock không đồng bộ

**Giải pháp**:
```bash
# Linux/Mac
sudo ntpdate -s time.nist.gov

# Hoặc
sudo timedatectl set-ntp true
```

---

## Debug Tips

### Backend Logging

Thêm vào `application.yaml`:
```yaml
logging:
  level:
    com.example.app: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
```

### Frontend Debug

```javascript
// Trong browser console
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
console.log(auth.token)
console.log(auth.user)
```

### Network Debugging

**Browser DevTools**:
1. Open Network tab
2. Filter: XHR
3. Check Request/Response headers
4. Check Status codes

**Backend Logs**:
```bash
# Docker
docker compose logs -f backend

# Local
tail -f logs/application.log
```

---

## Production Checklist

Trước khi deploy production:

- [ ] Đổi JWT secret (dùng environment variable)
- [ ] Set HTTPS cho OAuth2 redirect URIs
- [ ] Update CORS `allowedOrigins` với production domain
- [ ] Set strong database password
- [ ] Enable security headers (CSP, HSTS)
- [ ] Set up rate limiting
- [ ] Configure logging và monitoring
- [ ] Backup database strategy
- [ ] Set up CI/CD pipeline
- [ ] Review và update dependencies

---

## Getting Help

Nếu vẫn gặp lỗi:

1. Kiểm tra logs (backend và frontend)
2. Kiểm tra network requests trong browser DevTools
3. Verify OAuth2 credentials trong provider console
4. Test API với Postman/curl
5. Kiểm tra database connection và schema

**Common Issues Summary**:
- OAuth2: Sai redirect URI → Fix trong provider console
- CORS: Sai origin trong config → Fix `allowedOrigins`
- JWT: Token expired → Login lại
- Database: Connection refused → Start PostgreSQL

