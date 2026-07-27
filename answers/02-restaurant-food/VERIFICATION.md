# Báo cáo rà soát và xác minh — Restaurant / Food

Ngày xác minh: 2026-07-20 (Asia/Saigon).

## Tài liệu đầu vào đã đối chiếu

| Tệp                               | SHA-256                                                            |
|-----------------------------------|--------------------------------------------------------------------|
| `MSS301_Exam_Paper.docx`          | `5DBC1C7E0DF1AF65C3530551EB25B8B9C03665147E5D130CA5DBE14D4ACD3284` |
| `create_category_table_sql.txt`   | `337081C082BD39B9258E8F5EA35A8F33B87FA663734EFE760F745ACAF811B2A5` |
| `create_restaurant_table_sql.txt` | `9DF1D522CF3873C063B462E2F76F6D174F46F76991B9D6D2F137368553F5DA6C` |
| `create_foods_table_sql.txt`      | `19C0C1A412A26B8027503ABAA87CB41D0A29BEDCF505971B3A19BEF818984BEA` |

Quá trình rà soát đã đối chiếu tên dự án, tên gói Java, JDK/Spring Boot, cổng, nguồn dữ liệu, DTO, điểm cuối API, ánh xạ
HTTP/trạng thái, phân trang, CORS, tuyến gateway và từng cột SQL.

## Môi trường xác minh

- Eclipse Temurin JDK `21.0.11+10`;
- Apache Maven `3.9.11`;
- Spring Boot `3.5.11`;
- Spring Cloud `2025.0.2`.

## Kết quả tự động

Đã chạy `mvn clean test`, sau đó `mvn -DskipTests package` độc lập trong cả ba dự án.

| Dự án                       | Số kiểm thử | Thất bại |   Lỗi | Đóng gói    |
|-----------------------------|------------:|---------:|------:|-------------|
| `SE180211RestaurantService` |           8 |        0 |     0 | ĐẠT         |
| `SE180211FoodService`       |           5 |        0 |     0 | ĐẠT         |
| `SE180211FoodyGateway`      |           2 |        0 |     0 | ĐẠT         |
| **Tổng**                    |      **15** |    **0** | **0** | **3/3 ĐẠT** |

Phạm vi kiểm thử:

- kiểm tra hợp lệ khi tạo mới và phân trang;
- tên Restaurant trùng → trạng thái 3;
- Category/Restaurant tham chiếu không tồn tại → trạng thái 4;
- cập nhật một phần giữ nguyên các trường bị bỏ qua;
- chi tiết Food ánh xạ đúng `FoodResponseDTO` có Restaurant lồng bên trong;
- `openDate` nhận ISO-8601 và ví dụ `dd/MM/yyyy`, trả ra ISO UTC;
- gateway có tuyến Restaurant, Category, Food và hai tuyến OpenAPI;
- yêu cầu CORS tiền kiểm chấp nhận nguồn bên ngoài.

## Các sửa đổi quan trọng sau khi sinh mã

- Đổi đường dẫn mã nguồn/OpenAPI thành đúng `{restaurantId}` và `{foodId}`.
- Bổ sung tuyến `/api/categories/**` qua gateway.
- Bổ sung Swagger tổng hợp cho hai dịch vụ qua cổng 8080.
- Chỉ giữ một bộ lọc CORS để tránh tiêu đề HTTP lặp.
- Giữ thông báo nghiệp vụ thật trong `ApiResponseDTO`.
- Dùng phản hồi Feign kiểu `ApiResponseDTO<RestaurantDTO>` thay cho `Object`.
- Hòa giải hai định dạng `openDate` mà vẫn tuần tự hóa theo R04.
- Bổ sung cấu trúc gói của gateway bằng `package-info.java`, không tạo lớp miền nghiệp vụ không cần thiết.
- Bổ sung dữ liệu Category mẫu tùy chọn và bộ yêu cầu IntelliJ HTTP Client.

## Phần cần xác minh trên máy thi

Không có phiên bản SQL Server đang chạy trong phạm vi môi trường xây dựng hiện tại, nên chưa kiểm thử đầu-cuối với cơ sở
dữ liệu thật. Trước khi nộp phải:

1. chạy lược đồ trên SQL Server 2016+;
2. khởi động ba ứng dụng đúng thứ tự;
3. chạy toàn bộ `API_TESTS.http`;
4. kiểm tra phản hồi và dữ liệu DB sau thao tác DELETE mềm;
5. mở Swagger của gateway và kiểm tra cả hai nhóm API;
6. đổi MSSV mẫu thành MSSV thật;
7. chạy `mvn clean test`, sau đó `mvn clean` trước khi nén.

Những mâu thuẫn không thể đồng thời đúng theo nghĩa đen đã được giải thích và ra quyết định
tại [HUONG_DAN_TU_HOC_VA_GO_CODE.md](HUONG_DAN_TU_HOC_VA_GO_CODE.md#2-các-mâu-thuẫn-wordsql-và-quyết-định-đã-dùng).
