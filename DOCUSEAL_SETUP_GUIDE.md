# 📋 DOCUSEAL INTEGRATION - HƯỚNG DẪN TRIỂN KHAI CHI TIẾT

## 🎯 TỔNG QUAN

DocuSeal đã được tích hợp vào hệ thống để xử lý 2 loại hợp đồng điện tử:

1. **Hợp đồng đăng bán (Product Listing Contract)**: Seller ký với Hệ thống sau khi xe đạt kiểm định
2. **Hợp đồng mua bán (Sale Transaction Contract)**: Buyer và Seller cùng ký khi đặt cọc 10%

---

## 🚀 BƯỚC 1: SETUP DOCUSEAL

### 1.1. Tạo tài khoản DocuSeal

**Option 1: Cloud (Khuyến nghị cho development)**
- Đăng ký tại: https://www.docuseal.com
- Lấy API key từ: Dashboard -> Settings -> API
- Copy API key vào `application.properties`

**Option 2: Self-hosted (Optional)**
```bash
docker run -d -p 3000:3000 \
  -v docuseal_data:/data \
  --name docuseal \
  docuseal/docuseal:latest
```

### 1.2. Tạo Templates trên DocuSeal

#### Template 1: Product Listing Contract (Hợp đồng đăng bán)

**Bước tạo:**
1. Vào DocuSeal Dashboard -> Templates -> Create New
2. Tên template: "EV Product Listing Contract"
3. Upload file PDF mẫu hoặc tạo mới
4. Thêm **Roles**: 
   - Role name: `Seller` (người ký)
5. Thêm **Fields** cần điền:
   - `product_name` (Text)
   - `product_model` (Text)
   - `product_price` (Number)
   - `license_plate` (Text)
   - `brand` (Text)
   - `year` (Number)
   - `seller_name` (Text)
   - `seller_email` (Email)
   - `seller_phone` (Phone)
   - `contract_date` (Date)
   - `seller_signature` (Signature field - bắt buộc)
6. Save template và **copy Template ID**

#### Template 2: Sale Transaction Contract (Hợp đồng mua bán)

**Bước tạo:**
1. Vào DocuSeal Dashboard -> Templates -> Create New
2. Tên template: "EV Sale Transaction Contract"
3. Upload file PDF mẫu
4. Thêm **Roles**:
   - Role 1: `Buyer` (người mua)
   - Role 2: `Seller` (người bán)
5. Thêm **Fields**:
   
   **Buyer fields:**
   - `buyer_name` (Text)
   - `buyer_email` (Email)
   - `buyer_phone` (Phone)
   - `buyer_address` (Text)
   - `buyer_signature` (Signature - bắt buộc)
   
   **Seller fields:**
   - `seller_name` (Text)
   - `seller_email` (Email)
   - `seller_phone` (Phone)
   - `seller_signature` (Signature - bắt buộc)
   
   **Common fields:**
   - `order_id` (Number)
   - `total_amount` (Number)
   - `deposit_amount` (Number)
   - `remaining_amount` (Number)
   - `transaction_location` (Text)
   - `product_name` (Text)
   - `license_plate` (Text)
   - `contract_date` (Date)

6. Save template và **copy Template ID**

### 1.3. Cấu hình Webhook

1. Vào DocuSeal Dashboard -> Settings -> Webhooks
2. Add new webhook:
   - **URL**: `https://your-domain.com/api/docuseal/webhook`
   - **Events**: Check tất cả events (submission.completed, submitter.completed, etc.)
   - Save

**Lưu ý:** Nếu chạy local, dùng ngrok:
```bash
ngrok http 8080
# Copy HTTPS URL: https://abc123.ngrok.io
# Webhook URL: https://abc123.ngrok.io/api/docuseal/webhook
```

---

## ⚙️ BƯỚC 2: CẤU HÌNH APPLICATION.PROPERTIES

Mở file `application.properties` và cập nhật:

```properties
# DocuSeal Configuration
docuseal.api.key=YOUR_ACTUAL_API_KEY_HERE
docuseal.api.base-url=https://docuseal.com
docuseal.template.product-listing=TEMPLATE_ID_FROM_STEP_1.1
docuseal.template.sale-transaction=TEMPLATE_ID_FROM_STEP_1.2
docuseal.webhook.url=https://your-domain.com/api/docuseal/webhook
```

**Ví dụ thực tế:**
```properties
docuseal.api.key=pk_live_abc123xyz789
docuseal.api.base-url=https://docuseal.com
docuseal.template.product-listing=tmpl_aBc12DeF3gH
docuseal.template.sale-transaction=tmpl_xYz45OpQ6rS
docuseal.webhook.url=https://abc123.ngrok.io/api/docuseal/webhook
```

---

## 🧪 BƯỚC 3: TEST INTEGRATION

### 3.1. Test Webhook Endpoint

```bash
# Test xem webhook có hoạt động không
curl http://localhost:8080/api/docuseal/webhook/test
```

Response mong đợi:
```json
{
  "status": "success",
  "message": "DocuSeal webhook endpoint is working",
  "timestamp": "1234567890"
}
```

### 3.2. Test Flow Đăng Bán Sản Phẩm

**Bước 1: Seller đăng xe**
```bash
POST /api/seller/products/car
Authorization: Bearer <seller_jwt>
Content-Type: multipart/form-data

{
  "productname": "VinFast VF8",
  "description": "Xe điện 5 chỗ",
  "cost": 900000000,
  "licensePlate": "29A-12345",
  "model": "VF8",
  "specs": "Pin 87.7kWh",
  "brand": "VinFast",
  "year": 2023,
  "images": [file1.jpg, file2.jpg]
}
```

**Bước 2: Manager duyệt sơ bộ**
```bash
POST /api/manager/products/{productId}/approve-preliminary
Authorization: Bearer <manager_jwt>

{
  "approved": true,
  "note": "Thông tin xe hợp lệ"
}
```

**Bước 3: Manager nhập kết quả kiểm định**
```bash
POST /api/manager/products/{productId}/input-inspection
Authorization: Bearer <manager_jwt>

{
  "approved": true,
  "note": "Xe đạt kiểm định"
}
```

⚡ **Tại đây:**
- Hệ thống tự động gọi DocuSeal API
- Tạo submission (hồ sơ ký)
- Gửi email cho Seller với link ký hợp đồng
- Lưu submission ID vào DB

**Bước 4: Seller nhận email và ký hợp đồng**
- Seller mở email từ DocuSeal
- Click link ký
- Điền thông tin (nếu cần) và ký điện tử
- Submit

**Bước 5: DocuSeal gửi webhook callback**
```json
{
  "event_type": "submission.completed",
  "data": {
    "slug": "tN7jL9",
    "status": "completed",
    "documents": [
      {
        "url": "https://docuseal.com/signed/abc123.pdf"
      }
    ]
  }
}
```

⚡ **Hệ thống tự động:**
- Cập nhật contract status = true
- Lưu URL PDF đã ký
- Gửi notification cho Seller
- Có thể tự động đưa xe vào kho (tùy logic)

### 3.3. Test Flow Mua Bán

**Bước 1: Buyer mua xe và đặt cọc 10%**
```bash
POST /api/buyer/orders/{orderId}/deposit
Authorization: Bearer <buyer_jwt>

{
  "paymentMethod": "BANK_TRANSFER",
  "transactionLocation": "123 Nguyễn Huệ, Q1, TPHCM"
}
```

⚡ **Tại đây:**
- Hệ thống xử lý thanh toán cọc
- Tự động gọi DocuSeal API
- Tạo submission với 2 signers (Buyer + Seller)
- Gửi email cho cả Buyer và Seller

**Bước 2: Buyer và Seller ký hợp đồng**
- Cả hai nhận email
- Click link ký của mình
- Ký điện tử

**Bước 3: Sau khi cả hai ký xong**
- DocuSeal gửi webhook `submission.completed`
- Hệ thống cập nhật order status = "HOP_DONG_HOAN_TAT"
- Gửi notification cho Manager để duyệt giao dịch

---

## 📊 WORKFLOW HOÀN CHỈNH

### Flow 1: Đăng bán sản phẩm
```
1. Seller đăng xe → Status: CHO_DUYET
2. Manager duyệt sơ bộ → Status: CHO_KIEM_DINH
3. Bên thứ 3 kiểm định xe → Manager nhập kết quả
4. Nếu đạt → Status: DA_DUYET
   ├─→ DocuSealService.createProductListingContract()
   ├─→ Gửi email cho Seller
   └─→ Seller ký hợp đồng
5. Webhook callback → Contract.status = true
6. Xe vào kho → Status: TRONG_KHO, inWarehouse = true
7. Hiển thị trên nền tảng → Buyer có thể mua
```

### Flow 2: Mua bán xe
```
1. Buyer chọn xe → Tạo order
2. Buyer đặt cọc 10%
   ├─→ Transaction được tạo
   ├─→ DocuSealService.createSaleTransactionContract()
   └─→ Gửi email cho Buyer & Seller
3. Buyer ký hợp đồng
4. Seller ký hợp đồng
5. Webhook callback → Contract.status = true, Order.status = HOP_DONG_HOAN_TAT
6. Manager duyệt giao dịch
7. Buyer đến điểm giao dịch
8. Thanh toán 90% còn lại
9. Sang tên xe (nếu chọn)
10. Giao dịch hoàn tất → Trích 5% hoa hồng → Chuyển 95% cho Seller
```

---

## 🔧 TROUBLESHOOTING

### Lỗi 1: "DocuSeal API returned status: 401"
**Nguyên nhân:** API key sai hoặc chưa được cấu hình
**Giải pháp:**
1. Kiểm tra `application.properties`
2. Đảm bảo `docuseal.api.key` đúng
3. Kiểm tra API key còn hiệu lực trên DocuSeal Dashboard

### Lỗi 2: "Template not found"
**Nguyên nhân:** Template ID sai
**Giải pháp:**
1. Vào DocuSeal Dashboard -> Templates
2. Copy đúng Template ID
3. Cập nhật vào `application.properties`

### Lỗi 3: Webhook không nhận được callback
**Nguyên nhân:**
- URL không public (chạy localhost)
- Webhook chưa được cấu hình trên DocuSeal

**Giải pháp:**
1. Dùng ngrok để expose localhost:
   ```bash
   ngrok http 8080
   ```
2. Cập nhật webhook URL trên DocuSeal Dashboard
3. Kiểm tra logs để xem webhook có được gọi không

### Lỗi 4: Email không được gửi
**Nguyên nhân:** `send_email = false` hoặc email không hợp lệ
**Giải pháp:**
1. Kiểm tra `DocuSealSubmissionRequest.send_email = true`
2. Đảm bảo email trong User entity hợp lệ
3. Kiểm tra spam folder

---

## 📝 API ENDPOINTS SUMMARY

### Webhook
- `POST /api/docuseal/webhook` - Nhận callback từ DocuSeal (PUBLIC)
- `GET /api/docuseal/webhook/test` - Test webhook endpoint

### Manager
- `POST /api/manager/products/{id}/input-inspection` - Nhập kết quả kiểm định (tạo hợp đồng đăng bán)

### Buyer
- `POST /api/buyer/orders/{id}/deposit` - Đặt cọc & tạo hợp đồng mua bán

---

## 📂 FILES CREATED

### DTOs
- `DocuSealSubmissionRequest.java` - Request tạo submission
- `DocuSealSubmissionResponse.java` - Response từ DocuSeal
- `DocuSealWebhookPayload.java` - Webhook payload

### Config
- `DocuSealConfig.java` - Configuration class

### Service
- `DocuSealService.java` - Interface
- `DocuSealServiceImpl.java` - Implementation

### Controller
- `DocuSealWebhookController.java` - Xử lý webhook

### Updated Files
- `ManagerServiceImpl.java` - Tích hợp DocuSeal vào kiểm định
- `BuyerController.java` - Tích hợp DocuSeal vào đặt cọc
- `SecurityConfig.java` - Cho phép webhook endpoint
- `ContractsRepository.java` - Thêm query methods
- `application.properties` - Thêm DocuSeal config

---

## ✅ CHECKLIST TRIỂN KHAI

- [ ] Tạo tài khoản DocuSeal
- [ ] Lấy API key
- [ ] Tạo 2 templates (Product Listing & Sale Transaction)
- [ ] Copy Template IDs
- [ ] Cập nhật `application.properties`
- [ ] Setup ngrok (nếu chạy local)
- [ ] Cấu hình webhook trên DocuSeal Dashboard
- [ ] Test webhook endpoint
- [ ] Test flow đăng bán
- [ ] Test flow mua bán
- [ ] Kiểm tra email được gửi
- [ ] Kiểm tra webhook callback hoạt động
- [ ] Kiểm tra PDF đã ký được lưu

---

## 🎓 LƯU Ý QUAN TRỌNG

1. **Security**: Webhook endpoint là PUBLIC, cần thêm signature verification để chắc chắn request từ DocuSeal (tùy chọn)

2. **Error Handling**: Code đã có try-catch để handle lỗi, nhưng nên monitor logs để phát hiện vấn đề

3. **Notification**: Tất cả notification đã được tạo tự động trong DocuSealService

4. **PDF Storage**: URL PDF đã ký được lưu trong `Contracts.docusealDocumentUrl`, có thể download về server nếu cần

5. **Testing**: Nên test kỹ trên development trước khi deploy production

---

## 📞 HỖ TRỢ

- DocuSeal Documentation: https://www.docuseal.com/docs
- DocuSeal API Reference: https://www.docuseal.com/docs/api
- Support: support@docuseal.com

---

**TRIỂN KHAI HOÀN TẤT! 🎉**

Bây giờ bạn có thể chạy project và test tính năng hợp đồng điện tử.
