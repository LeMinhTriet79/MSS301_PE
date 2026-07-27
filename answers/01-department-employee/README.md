# Đáp án Department – Employee – Gateway

Bộ đáp án đã được rà soát thủ công theo `MSS301_SP26_FE_PE_Question.docx` và hai tệp SQL chính thức.

Đọc trước khi học/chạy:

- [HUONG_DAN_TU_HOC_VA_GO_CODE.md](HUONG_DAN_TU_HOC_VA_GO_CODE.md): hướng dẫn cực chi tiết để tự gõ lại.
- [VERIFICATION_REPORT.md](VERIFICATION_REPORT.md): phạm vi và kết quả kiểm thử gần nhất.
- [MANUAL_REVIEW.md](MANUAL_REVIEW.md): bằng chứng đầu vào và các mâu thuẫn được bộ sinh mã phát hiện.
- `database/01_create_departments_official.sql` và `02_create_employees_official.sql`: SQL dùng khi chạy bài.

## Thứ tự chạy

1. Chạy SQL trong cơ sở dữ liệu `MSS301_2026_PE`.
2. `SE180211DepartmentService` — cổng 8081.
3. `SE180211EmployeeService` — cổng 8082.
4. `SE180211EmployeeGateway` — cổng 8080.

Trong mỗi dự án:

```powershell
mvn clean test
mvn spring-boot:run
```

Swagger trực tiếp ở 8081/8082; Swagger tổng hợp tại `http://localhost:8080/swagger-ui.html`.

Trước khi nộp phải đổi `SE180211/se180211` sang MSSV thật và nén từng dự án thành tệp ZIP riêng.
