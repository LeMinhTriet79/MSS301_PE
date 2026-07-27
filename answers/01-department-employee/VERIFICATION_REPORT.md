# Báo cáo kiểm chứng — Department/Employee

Thời điểm kiểm chứng gần nhất: 20/07/2026 (Asia/Saigon).

## Bằng chứng đã đối chiếu

- `D:\BaoHuy\MSS301_SP26_FE_PE_Question.docx`
- `D:\BaoHuy\create_departments_sql.txt`
- `D:\BaoHuy\create_employee_sql.txt`

Đã rà thủ công tên dự án/lớp điều khiển, package, JDK/Boot/Cloud, các thư viện, cổng, nguồn dữ liệu, `ddl-auto`, ánh xạ
thực thể, cấu trúc DTO, ánh xạ status/HTTP, kiểm tra hợp lệ, năm API của mỗi dịch vụ, OpenFeign, định tuyến Gateway,
Security, CORS và Swagger.

## Kiểm thử Maven trên JDK 21

| Dự án                       | Số kiểm thử | Thất bại |   Lỗi | Kết quả |
|-----------------------------|------------:|---------:|------:|---------|
| `SE180211DepartmentService` |           5 |        0 |     0 | ĐẠT     |
| `SE180211EmployeeService`   |           5 |        0 |     0 | ĐẠT     |
| `SE180211EmployeeGateway`   |           1 |        0 |     0 | ĐẠT     |
| **Tổng**                    |      **11** |    **0** | **0** | **ĐẠT** |

Kiểm thử Gateway khởi động ngữ cảnh WebFlux trên cổng ngẫu nhiên và gửi yêu cầu tiền kiểm `OPTIONS` từ một nguồn khác để
xác nhận CORS hoạt động.

Lệnh đã chạy cho từng dự án:

```powershell
mvn clean test
mvn -DskipTests package
```

Cả ba dự án đều tạo được Spring Boot JAR bằng Maven trên JDK 21.

## Giới hạn của kiểm chứng tự động

Các kiểm thử đơn vị/ngữ cảnh không khởi tạo SQL Server thật. Trước khi nộp vẫn phải chạy hai SQL chính thức, khởi động
ba ứng dụng và kiểm tra nhanh các API qua cổng 8080 theo `HUONG_DAN_TU_HOC_VA_GO_CODE.md`.

Các mâu thuẫn vốn có trong đề được giải quyết và giải thích trong hướng dẫn, nổi bật là `ApiResponseDTO.timestamp` so
với JSON mẫu có `message`, khả năng nhận null của `parent_id`, và giao của hai quy tắc `effective_date`.
