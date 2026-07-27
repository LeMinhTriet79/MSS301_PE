# Đáp án đề Restaurant / Food / Foody Gateway

Thư mục này chứa ba dự án Spring Boot độc lập dùng JDK 21 và Spring Boot 3.5.11:

- `SE180211RestaurantService` — cổng 8081;
- `SE180211FoodService` — cổng 8082;
- `SE180211FoodyGateway` — cổng 8080.

## Bắt đầu ở đây

1. Đọc [HUONG_DAN_TU_HOC_VA_GO_CODE.md](HUONG_DAN_TU_HOC_VA_GO_CODE.md) để tự học và gõ lại từng bước.
2. Chạy [database/MSS301_2026_PE.sql](database/MSS301_2026_PE.sql). Ba SQL đề bài được giữ riêng dưới tên
   `01_create_category_official.sql`, `02_create_restaurants_official.sql`, `03_create_foods_official.sql` để đối chiếu;
   tập lệnh gộp đã sửa lỗi thiếu dấu phẩy trong SQL Restaurant gốc.
3. Nếu cần dữ liệu kiểm thử, chạy [database/SEED_CATEGORIES_OPTIONAL.sql](database/SEED_CATEGORIES_OPTIONAL.sql).
4. Chạy lần lượt dịch vụ Restaurant, dịch vụ Food và Foody Gateway.
5. Mở [API_TESTS.http](API_TESTS.http) trong IntelliJ để thử toàn bộ điểm cuối API.
6. Xem kết quả xây dựng/kiểm thử tại [VERIFICATION.md](VERIFICATION.md).

## Lệnh cho mỗi dự án

```powershell
mvn clean test
mvn spring-boot:run
```

Swagger:

- Restaurant: `http://localhost:8081/swagger-ui.html`
- Food: `http://localhost:8082/swagger-ui.html`
- Gateway tổng hợp: `http://localhost:8080/swagger-ui.html`

Trước khi nộp, đổi `SE180211/se180211` sang MSSV thật, chạy lại kiểm thử, dùng `mvn clean` và nén riêng từng dự án.
Không đưa `target`, `.pegen`, tài liệu học hoặc dữ liệu mẫu tùy chọn vào tệp ZIP nộp nếu EOS chỉ yêu cầu mã nguồn dự án.
