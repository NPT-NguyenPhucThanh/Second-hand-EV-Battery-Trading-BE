# Hướng dẫn sử dụng Swagger UI

## Swagger đã được cấu hình thành công! 🎉

### Cách truy cập Swagger UI:

1. **Khởi động ứng dụng Spring Boot**
   ```
   mvnw.cmd spring-boot:run
   ```

2. **Truy cập Swagger UI qua trình duyệt:**
   - URL Swagger UI: `http://localhost:8080/swagger-ui.html`
   - URL API Docs (JSON): `http://localhost:8080/v3/api-docs`

### Tính năng đã được cấu hình:

✅ **Springdoc OpenAPI 2.3.0** - Thư viện Swagger hiện đại cho Spring Boot 3.x
✅ **JWT Authentication** - Tích hợp sẵn Bearer Token authentication
✅ **Public Access** - Swagger UI không cần đăng nhập
✅ **Auto Documentation** - Tự động tạo docs từ các Controller

### Cách sử dụng JWT trong Swagger:

1. **Đăng nhập để lấy token:**
   - Mở endpoint `/api/auth/login` trong Swagger UI
   - Nhập thông tin đăng nhập
   - Copy JWT token từ response

2. **Xác thực với Bearer Token:**
   - Click nút **"Authorize"** (biểu tượng ổ khóa) ở góc trên phải
   - Nhập token vào ô "Value" (không cần thêm "Bearer ")
   - Click **"Authorize"**
   - Click **"Close"**

3. **Sử dụng các API được bảo vệ:**
   - Sau khi authorize, tất cả request sẽ tự động có header Authorization
   - Bạn có thể test các endpoint yêu cầu authentication

### Cấu trúc API theo phân quyền:

📂 **Guest APIs** (`/api/guest/**`)
- Xem danh sách sản phẩm
- Tìm kiếm, lọc sản phẩm
- Xem chi tiết sản phẩm
- Xem thông tin seller

📂 **Auth APIs** (`/api/auth/**`)
- Đăng ký
- Đăng nhập
- Đổi mật khẩu

📂 **Client/Buyer APIs** (`/api/client/**`, `/api/buyer/**`)
- Quản lý giỏ hàng
- Mua sản phẩm
- Xem lịch sử giao dịch
- Đánh giá sản phẩm

📂 **Seller APIs** (`/api/seller/**`)
- Đăng bán xe và pin
- Quản lý sản phẩm
- Theo dõi doanh thu
- Quản lý đơn hàng

📂 **Manager APIs** (`/api/manager/**`)
- Duyệt sản phẩm
- Quản lý giao dịch
- Xử lý tranh chấp
- Quản lý người dùng

📂 **Chat APIs** (`/api/chat/**`)
- Chat real-time giữa buyer và seller

📂 **DocuSeal Webhook** (`/api/docuseal/**`)
- Webhook callback từ DocuSeal (public)

### Tùy chỉnh Swagger:

Các cấu hình trong `application.properties`:

```properties
# Swagger UI settings
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.tryItOutEnabled=true
springdoc.packages-to-scan=com.project.tradingev_batter.Controller
```

### Thêm mô tả cho Controller (Optional):

Bạn có thể thêm annotation vào Controller để có mô tả đẹp hơn:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/client")
@Tag(name = "Client", description = "APIs dành cho người dùng đã đăng ký")
public class ClientController {
    
    @Operation(
        summary = "Thêm sản phẩm vào giỏ hàng",
        description = "Client có thể thêm xe hoặc pin vào giỏ hàng",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/cart/add")
    public ResponseEntity<?> addToCart(@RequestBody CartRequest request) {
        // ...
    }
}
```

### Troubleshooting:

❌ **Swagger UI không hiển thị:**
- Kiểm tra ứng dụng đã chạy chưa
- Xóa cache trình duyệt
- Thử truy cập: `http://localhost:8080/v3/api-docs`

❌ **Lỗi 401 Unauthorized:**
- Đảm bảo đã authorize với JWT token
- Token phải còn hiệu lực (chưa hết hạn)

❌ **API không hiển thị trong Swagger:**
- Kiểm tra Controller có trong package `com.project.tradingev_batter.Controller`
- Kiểm tra có annotation `@RestController` và `@RequestMapping`

### Lưu ý bảo mật:

⚠️ **Trong môi trường Production:**
- Cân nhắc tắt Swagger UI: `springdoc.swagger-ui.enabled=false`
- Hoặc bảo vệ Swagger bằng authentication riêng
- Chỉ cho phép truy cập từ IP nội bộ

---

## Tóm tắt những gì đã thêm:

1. ✅ **Dependency** - `springdoc-openapi-starter-webmvc-ui` version 2.3.0
2. ✅ **SwaggerConfig.java** - Cấu hình OpenAPI với JWT authentication
3. ✅ **SecurityConfig.java** - Cho phép public access tới Swagger endpoints
4. ✅ **application.properties** - Cấu hình Swagger UI settings

**Giờ bạn có thể test tất cả API qua Swagger UI!** 🚀

