# RÀ SOÁT THỦ CÔNG — MSS301 PE

> Tệp này được tạo lại sau mỗi lần sinh đáp án. Không nộp bài trước khi hoàn thành toàn bộ ô kiểm tra.

## Kết quả nhận diện

- Hồ sơ đã chọn: `DEPARTMENT_EMPLOYEE`
- Kết quả tự động: `DEPARTMENT_EMPLOYEE` (độ tin cậy 97%)
- MSSV/phần tên package: `se180211`
- Cơ sở dữ liệu: `MSS301_2026_PE`

Bằng chứng đã sử dụng:

- `D:\BaoHuy\MSS301_SP26_FE_PE_Question.docx`
- `D:\BaoHuy\create_departments_sql.txt`
- `D:\BaoHuy\create_employee_sql.txt`

Bằng chứng phân loại:

- Đã phát hiện đồng thời bảng Department và Employee trong lược đồ SQL.
- Đặc tả cũng yêu cầu cả Department Service và Employee Service.

## Lược đồ đã phân tích

### `dbo.departments`

| Cột              | Kiểu SQL      | Cho phép null |      ID | Duy nhất | Giá trị hợp lệ           | Quan hệ                              |
|------------------|---------------|--------------:|--------:|---------:|--------------------------|--------------------------------------|
| `department_id`  | INT           |         không | tự tăng |          |                          |                                      |
| `name`           | NVARCHAR(50)  |         không |         |          |                          |                                      |
| `code`           | NVARCHAR(10)  |         không |         |       có |                          |                                      |
| `effective_date` | DATE          |            có |         |          |                          |                                      |
| `status`         | NVARCHAR(10)  |            có |         |          | ACTIVE, INACTIVE, CLOSED |                                      |
| `location`       | NVARCHAR(100) |            có |         |          |                          |                                      |
| `parent_id`      | INT           |            có |         |          |                          | departments.department_id (suy luận) |

Các ràng buộc `CHECK` đã phân tích (cần đối chiếu với phần kiểm tra Java và DDL đã sinh):

- `[status] IN ('ACTIVE', 'INACTIVE', 'CLOSED')`
- `[effective_date] >= CAST(GETDATE() AS DATE)`

### `dbo.employees`

| Cột             | Kiểu SQL      | Cho phép null |      ID | Duy nhất | Giá trị hợp lệ                  | Quan hệ                              |
|-----------------|---------------|--------------:|--------:|---------:|---------------------------------|--------------------------------------|
| `employee_id`   | INT           |         không | tự tăng |          |                                 |                                      |
| `full_name`     | NVARCHAR(100) |         không |         |          |                                 |                                      |
| `email`         | NVARCHAR(100) |         không |         |          |                                 |                                      |
| `position`      | NVARCHAR(30)  |         không |         |          |                                 |                                      |
| `status`        | NVARCHAR(10)  |         không |         |          | LEFT, RETIRED, ACTIVE, INACTIVE |                                      |
| `start_date`    | DATE          |         không |         |          |                                 |                                      |
| `end_date`      | DATE          |            có |         |          |                                 |                                      |
| `department_id` | INT           |         không |         |          |                                 | departments.department_id (suy luận) |

Các ràng buộc `CHECK` đã phân tích (cần đối chiếu với phần kiểm tra Java và DDL đã sinh):

- `([end_date] IS NULL OR [end_date]>=[start_date])`
- `([status]='LEFT' OR [status]='RETIRED' OR [status]='ACTIVE' OR [status]='INACTIVE')`

## Các mục vẫn cần quyết định thủ công

- [ ] Quan hệ `dbo.departments.parent_id -> dbo.departments.department_id` được suy luận từ quy ước tên `*_id`; hãy kiểm
  tra thủ công.
- [ ] Quan hệ `dbo.employees.department_id -> dbo.departments.department_id` được suy luận từ quy ước tên `*_id`; hãy
  kiểm tra thủ công.
- [ ] Đề mâu thuẫn giữa `ApiResponseDTO.message` và `timestamp`; mã nguồn tuân theo bảng DTO và dùng `timestamp`.
- [ ] Dữ liệu vào của Employee tuân theo DTO Department lồng nhau; dùng `{"department":{"departmentId":...}}`.
- [ ] Đề mâu thuẫn về khả năng nhận null của `parent_id`; hãy đối chiếu với SQL chính thức của giám thị.
- [x] Mâu thuẫn `effective_date` đã được xử lý khi rà soát: Java áp dụng `today <= date < today + 360 days`, đồng thời
  thỏa SQL chính thức và đề Word.
- [ ] Các trường SQL `DATE` dùng `dd/MM/yyyy` theo JSON Department cụ thể trong đề. R04 nhắc timestamp ISO-8601; cần xác
  nhận giám thị có áp dụng yêu cầu này cho trường ngày không có thời gian của Employee hay không.
- [ ] Các biểu thức `CHECK` tổng quát được liệt kê ở trên nhưng cấu hình DSL không biểu diễn được toàn bộ. Hãy ưu tiên
  SQL chính thức trong `.pegen/evidence` và đối chiếu phần kiểm tra Java thủ công.
- [ ] Quan hệ suy luận, không phải `FOREIGN KEY` tường minh: `departments.parent_id -> departments.department_id`.
- [ ] Quan hệ suy luận, không phải `FOREIGN KEY` tường minh: `employees.department_id -> departments.department_id`.
- [ ] PUT một phần hiện coi JSON null giống trường bị bỏ qua, nên chưa thể xóa tường minh giá trị của cột cho phép null.
  Nếu bộ kiểm thử yêu cầu xóa bằng null, cần dùng DTO cập nhật có khả năng ghi nhận sự hiện diện của trường, ví dụ
  `JsonNullable`.
- [x] Cả ba dự án đã vượt qua `mvn clean test` và `mvn -DskipTests package` trên JDK 21; xem `VERIFICATION_REPORT.md`.

## Các dự án đã sinh

- `SE180211DepartmentService`
- `SE180211EmployeeService`
- `SE180211EmployeeGateway`

Hai dịch vụ dữ liệu gồm Spring Web, Spring Data JPA, Validation, MS SQL Server Driver, Lombok, DevTools và Springdoc.
Employee Service còn sử dụng OpenFeign. Gateway chỉ dùng các thư viện cần thiết cho Gateway, Security và OpenAPI.

## Kiểm tra cuối bắt buộc

- [ ] Mở từng thư mục đã sinh thành một dự án Maven độc lập trong IntelliJ.
- [ ] Đặt SDK dự án (`Project SDK`) và trình chạy Maven thành JDK 21.
- [ ] Bật xử lý annotation nếu IntelliJ báo lỗi Lombok.
- [x] Chạy `mvn -DskipTests compile` trong từng dự án; đã hoàn thành khi kiểm chứng đáp án.
- [ ] Ưu tiên SQL chính thức đã sao chép vào `.pegen/evidence`; không ghi đè cơ sở dữ liệu thi đang tồn tại.
- [ ] Chạy tập lệnh tiện ích không phá hủy `database/MSS301_2026_PE.sql` trên SQL Server và đối chiếu mọi bảng/cột với
  đề chính thức.
- [ ] Khởi động Department Service (8081), Employee Service (8082), rồi Gateway (8080).
- [ ] Kiểm thử tạo mới/cập nhật/chi tiết/vô hiệu hóa/danh sách, mọi lỗi dữ liệu không hợp lệ, mã trùng và ánh xạ status
  không tìm thấy.
- [ ] Kiểm thử toàn bộ định tuyến công khai qua cổng 8080 và xác nhận CORS.
- [ ] Nén ZIP riêng từng dự án, không gồm `target`, `.idea` hoặc `.git`; tuân thủ đúng hướng dẫn đóng gói EOS trong đề.

Được sinh lúc 2026-07-20T14:23:42.576896400Z.
