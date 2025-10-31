# 🚗⚡ Second-hand EV & Battery Trading Platform - Backend

> **Spring Boot REST API** cho nền tảng mua bán xe điện và pin EV cũ

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![SQL Server](https://img.shields.io/badge/SQL%20Server-2019-blue)](https://www.microsoft.com/sql-server)
[![Status](https://img.shields.io/badge/Status-Complete-success)](https://github.com)

---

## 📋 Mục lục

- [Giới thiệu](#giới-thiệu)
- [Tính năng](#tính-năng)
- [Công nghệ](#công-nghệ)
- [Cài đặt](#cài-đặt)
- [Cấu hình](#cấu-hình)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Documentation Files](#documentation-files)
- [Contributors](#contributors)

---

## 🎯 Giới thiệu

Hệ thống **mua bán xe điện và pin EV cũ** với đầy đủ tính năng:
- Thanh toán trực tuyến qua **VNPay**
- Ký hợp đồng điện tử qua **DocuSeal**
- Chat real-time với **WebSocket**
- Hệ thống escrow bảo vệ giao dịch
- Quản lý dispute và refund
- Thống kê toàn diện cho Manager

**Actors:** Guest, Buyer, Seller, Staff, Manager

---

## ✨ Tính năng

### Core Features ✅
- ✅ **Authentication & Authorization** - JWT token-based
- ✅ **User Management** - 5 roles với permissions riêng
- ✅ **Product Management** - Car EV & Battery với chi tiết đầy đủ
- ✅ **Order Management** - Flow hoàn chỉnh từ đặt cọc đến hoàn tất
- ✅ **Payment Integration** - VNPay Sandbox
- ✅ **Contract E-Signature** - DocuSeal integration
- ✅ **Real-time Chat** - WebSocket messaging
- ✅ **Notification System** - Real-time notifications
- ✅ **Feedback & Rating** - Đánh giá sản phẩm và seller

### Advanced Features ✅
- ✅ **Dispute Management** - Khiếu nại và giải quyết tranh chấp
- ✅ **Refund Processing** - Hoàn tiền tự động với escrow release
- ✅ **Manager Statistics** - Revenue, Users, Products, Orders, Transactions
- ✅ **Scheduled Tasks** - Auto-confirm orders, Auto-hide expired products
- ✅ **Advanced Search** - Multiple filters, sort, pagination
- ✅ **AI Price Suggestion** - Google Gemini AI integration

---

## 🛠️ Công nghệ

### Backend Framework
```
Spring Boot 3.x
Spring Security
Spring Data JPA
Spring WebSocket
```

### Database
```
SQL Server 2019
Hibernate ORM
```

### External Services
```
✅ VNPay Payment Gateway
✅ DocuSeal E-Signature  
✅ Cloudinary Image Storage
✅ Google Gemini AI
```

### Build Tools
```
Maven 3.9+
Java 17
```

---

## 🚀 Cài đặt

### Yêu cầu
- Java 17 hoặc cao hơn
- SQL Server 2019+
- Maven 3.9+
- IntelliJ IDEA (recommended) hoặc Eclipse

### Bước 1: Clone repository
```bash
git clone <repository-url>
cd Second-hand-EV-Battery-Trading-BE
```

### Bước 2: Cấu hình Database
```sql
-- Tạo database
CREATE DATABASE EV_Battery_Trading;

-- Import schema từ file db/schema.sql (nếu có)
-- Hoặc chạy ứng dụng với spring.jpa.hibernate.ddl-auto=update
```

### Bước 3: Cấu hình application.properties
```properties
# Sao chép từ application.properties.example
# Điền thông tin database, VNPay, DocuSeal, Cloudinary
```

### Bước 4: Build và chạy
```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

Application sẽ chạy tại: `http://localhost:8080`

---

## ⚙️ Cấu hình

### application.properties

```properties
# Database Configuration
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=EV_Battery_Trading;encrypt=false
spring.datasource.username=your_username
spring.datasource.password=your_password

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT Configuration
jwt.secret=your-secret-key-min-256-bits
jwt.expiration=86400000

# VNPay Configuration
vnpay.vnp_TmnCode=YOUR_TMN_CODE
vnpay.vnp_HashSecret=YOUR_HASH_SECRET
vnpay.vnp_PayUrl=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.vnp_ReturnUrl=http://localhost:8080/api/payment/vnpay-return

# DocuSeal Configuration
docuseal.api.key=YOUR_DOCUSEAL_API_KEY
docuseal.api.url=https://docuseal.com/api
docuseal.template.id=YOUR_TEMPLATE_ID
docuseal.webhook.url=YOUR_NGROK_URL/api/docuseal/webhook

# Cloudinary Configuration
cloudinary.cloud_name=YOUR_CLOUD_NAME
cloudinary.api_key=YOUR_API_KEY
cloudinary.api_secret=YOUR_API_SECRET

# Google Gemini AI
gemini.api.key=YOUR_GEMINI_API_KEY
```

### Test Credentials
```
Manager: manager / manager123
Staff: staff / staff123
Seller: seller / seller123
Buyer: buyer / buyer123
```

---

## 📚 API Documentation

### Swagger UI
Sau khi chạy ứng dụng, truy cập:
```
http://localhost:8080/swagger-ui.html
```

### Postman Collections
1. **Postman_Collection_Trading_EV_Battery.json** - Main collection (100+ endpoints)
2. **Postman_NEW_ENDPOINTS.json** - New Statistics & Disputes APIs (22 endpoints)
3. **Postman_Environment_EV_Trading.json** - Environment variables

**Import vào Postman để test:**
- Xem hướng dẫn chi tiết trong `POSTMAN_IMPORT_GUIDE.md`

### API Endpoints Summary

#### Authentication
```
POST /api/auth/register    - Đăng ký
POST /api/auth/login       - Đăng nhập
POST /api/auth/logout      - Đăng xuất
GET  /api/auth/me          - Get current user
```

#### Products
```
GET    /api/products                    - Tất cả sản phẩm
GET    /api/products/{id}               - Chi tiết sản phẩm
GET    /api/products/advanced-search    - Tìm kiếm nâng cao
POST   /api/seller/products             - Đăng sản phẩm (Seller)
```

#### Orders
```
POST   /api/buyer/checkout          - Tạo đơn hàng
GET    /api/buyer/orders             - Đơn hàng của buyer
POST   /api/staff/orders/{id}/approve - Duyệt đơn (Staff)
```

#### Payment
```
POST   /api/payment/create-payment-url  - Tạo URL VNPay
GET    /api/payment/vnpay-return        - VNPay callback
```

#### Disputes & Refunds
```
POST   /api/buyer/disputes                 - Tạo khiếu nại
GET    /api/manager/disputes               - Tất cả disputes
POST   /api/manager/disputes/{id}/resolve  - Giải quyết
POST   /api/manager/refunds/{id}/process   - Xử lý refund
```

#### Manager Statistics
```
GET    /api/manager/statistics/revenue        - Revenue stats
GET    /api/manager/statistics/users          - User stats
GET    /api/manager/statistics/products       - Product stats
GET    /api/manager/statistics/orders         - Order stats
GET    /api/manager/dashboard/overview        - Dashboard
```

**Xem đầy đủ:** `ENDPOINT_CHECKLIST.md`

---

## 🧪 Testing

### 1. Postman Testing
```bash
# Import collections
- Postman_Collection_Trading_EV_Battery.json
- Postman_NEW_ENDPOINTS.json
- Postman_Environment_EV_Trading.json

# Chạy tests theo thứ tự trong POSTMAN_IMPORT_GUIDE.md
```

### 2. Integration Testing
```bash
# VNPay Sandbox
Card: 9704198526191432198
Name: NGUYEN VAN A
OTP: 123456

# DocuSeal Webhook (cần ngrok)
ngrok http 8080
# Cập nhật webhook URL trong DocuSeal settings
```

### 3. Manual Testing Flows
Xem chi tiết trong:
- `POSTMAN_TESTING_GUIDE.md` - Testing workflows
- `VNPAY_COMPLETE_GUIDE.md` - VNPay integration testing
- `DOCUSEAL_COMPLETE_GUIDE.md` - DocuSeal testing
- `WEBSOCKET_CHAT_TESTING_GUIDE.md` - Chat testing

---

## 📖 Documentation Files

### 🎯 Bắt đầu từ đây
1. **PROJECT_CONTEXT.md** - Business logic và requirements
2. **BACKEND_COMPLETION_SUMMARY.md** - Tổng quan backend
3. **FINAL_HANDOVER_SUMMARY.md** - Tổng kết hoàn thành

### 🔧 Development Guides
- **POSTMAN_IMPORT_GUIDE.md** - Setup Postman
- **POSTMAN_TESTING_GUIDE.md** - Testing workflows
- **ENDPOINT_CHECKLIST.md** - Danh sách endpoints

### 🎨 Frontend Integration
- **FRONTEND_IMPLEMENTATION_GUIDE.md** - Hướng dẫn develop FE
- **FE_NEW_APIS_SUPPLEMENT.md** - APIs mới cần integrate
- **FE_DEVELOPMENT_PROMPT.md** - FE requirements

### 🔌 Integration Guides
- **VNPAY_COMPLETE_GUIDE.md** - VNPay setup & testing
- **DOCUSEAL_COMPLETE_GUIDE.md** - DocuSeal setup & testing
- **WEBSOCKET_CHAT_TESTING_GUIDE.md** - Chat feature testing

### 📊 Project Management
- **PROJECT_STATUS_AND_TODO.md** - Status tracking
- **QUICK_TODO.md** - Quick reference todos

---

## 🗂️ Project Structure

```
src/main/java/com/project/tradingev_batter/
├── Controller/              # REST API Controllers
│   ├── AuthController.java
│   ├── BuyerController.java
│   ├── SellerController.java
│   ├── StaffController.java
│   ├── ManagerController.java
│   ├── PaymentController.java
│   ├── DocuSealController.java
│   ├── ChatController.java
│   └── ...
│
├── Service/                 # Business Logic
│   ├── UserService.java
│   ├── ProductService.java
│   ├── OrderService.java
│   ├── PaymentService.java
│   ├── DisputeService.java
│   ├── RefundService.java
│   ├── StatisticsService.java
│   └── ...
│
├── Repository/              # Data Access Layer
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── OrderRepository.java
│   ├── DisputeRepository.java
│   ├── RefundRepository.java
│   └── ...
│
├── Entity/                  # Database Entities
│   ├── User.java
│   ├── Product.java
│   ├── Orders.java
│   ├── Dispute.java
│   ├── Refund.java
│   └── ...
│
├── dto/                     # Data Transfer Objects
│   ├── LoginRequest.java
│   ├── CheckoutRequest.java
│   ├── DisputeRequest.java
│   └── ...
│
├── enums/                   # Enumerations
│   ├── OrderStatus.java
│   ├── DisputeStatus.java
│   ├── RefundStatus.java
│   └── ...
│
├── security/                # Security Configuration
│   ├── JwtAuthenticationFilter.java
│   ├── JwtTokenProvider.java
│   └── CustomUserDetails.java
│
└── config/                  # Application Configuration
    ├── SecurityConfig.java
    ├── VNPayConfig.java
    ├── DocuSealConfig.java
    ├── CloudinaryConfig.java
    └── WebSocketConfig.java
```

---

## 🔒 Security

- ✅ JWT-based authentication
- ✅ Role-based access control (RBAC)
- ✅ Password encryption (BCrypt)
- ✅ CORS configuration
- ✅ SQL injection prevention (JPA)
- ✅ XSS protection
- ✅ CSRF protection (disabled for REST API)

---

## 📊 Database Schema

### Core Tables
```sql
users, roles, user_roles
products, orders, order_details
transactions, contracts
carts, cart_items
feedbacks, notifications
chatmessages
```

### New Tables (v2.0)
```sql
disputes          -- Dispute management
refunds           -- Refund processing
seller_upgrade_requests  -- Seller upgrade workflow
```

**Total: 17 tables**

Xem chi tiết trong `PROJECT_CONTEXT.md`

---

## 🚦 Order Status Flow

```
CHO_DAT_COC → CHO_THANH_TOAN_COC → DA_DAT_COC → CHO_DUYET 
→ DA_DUYET → CHO_THANH_TOAN_CUOI → DA_THANH_TOAN 
→ DANG_GIAO_DICH → DA_HOAN_TAT
    ↓ (Optional)
TRANH_CHAP → DA_HOAN_TIEN
```

---

## 🎓 Contributors

**Backend Team:**
- Developer 1 - Core Features
- Developer 2 - Payment Integration
- Developer 3 - Chat & Notifications
- Developer 4 - Statistics & Reports

**Project:** SWP391 - FPT University
**Semester:** Fall 2024
**Completion:** October 30, 2025

---

## 📞 Support

**Documentation Issues?**
- Xem `FINAL_HANDOVER_SUMMARY.md` cho troubleshooting
- Check `Known Issues` section trong các guide files

**API Questions?**
- Reference Swagger UI: http://localhost:8080/swagger-ui.html
- Import Postman collections để xem examples

**Testing Help?**
- Follow `POSTMAN_IMPORT_GUIDE.md`
- Check integration guides cho VNPay, DocuSeal

---

## 📝 License

This project is for educational purposes (SWP391 Course Project).

---

## 🎉 Status

✅ **BACKEND 100% COMPLETE**
🔄 **FRONTEND IN PROGRESS**

**Next Steps:**
1. Import Postman collections
2. Test all APIs
3. Implement Frontend
4. Integration testing
5. Deployment

---

**Cập nhật:** October 30, 2025 - 11:30 PM
**Version:** 2.0 - Production Ready
**Documentation:** Complete & Ready for Handover

🚀 **READY FOR FRONTEND DEVELOPMENT!**

