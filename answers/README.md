# Hai bộ đáp án MSS301 PE

Thư mục này chứa hai bộ bài làm độc lập được dựng từ đúng hai đề Word và các tập lệnh SQL đi kèm. Mã sinh viên đang dùng
là **SE180211**:

| Bộ đề                 | Đề Word                           | SQL                                | Ba dự án                                                                          |
|-----------------------|-----------------------------------|------------------------------------|-----------------------------------------------------------------------------------|
| Department / Employee | `MSS301_SP26_FE_PE_Question.docx` | `departments`, `employees`         | `SE180211DepartmentService`, `SE180211EmployeeService`, `SE180211EmployeeGateway` |
| Restaurant / Food     | `MSS301_Exam_Paper.docx`          | `Category`, `restaurants`, `Foods` | `SE180211RestaurantService`, `SE180211FoodService`, `SE180211FoodyGateway`        |

## Mở đúng nội dung cần dùng

- Bộ Department/Employee: xem `01-department-employee/README.md`; học và gõ lại theo
  `01-department-employee/HUONG_DAN_TU_HOC_VA_GO_CODE.md`; file nộp nằm trong `01-department-employee/submission/`.
- Bộ Restaurant/Food: xem `02-restaurant-food/README.md`; học và gõ lại theo
  `02-restaurant-food/HUONG_DAN_TU_HOC_VA_GO_CODE.md`; yêu cầu HTTP mẫu nằm trong `API_TESTS.http`, file nộp nằm trong
  `02-restaurant-food/submission/`.
- Báo cáo kiểm chứng chi tiết của từng bộ nằm trong `VERIFICATION_REPORT.md` (bộ Restaurant/Food còn có
  `VERIFICATION.md` đầy đủ hơn).

## Thứ tự ưu tiên khi đề có mâu thuẫn

1. Các mục bắt buộc có thể khiến bài bị chấm 0: tên dự án, bộ điều khiển, gói mã nguồn, JDK, Spring Boot, cổng chạy và
   cấu hình nguồn dữ liệu.
2. Tên bảng/cột và kiểu vật lý trong SQL chính thức.
3. Bảng DTO và bảng điểm cuối/trạng thái API trong đề.
4. Quy tắc chung R01–R04.
5. JSON mẫu hoặc câu mô tả bằng văn bản khi chúng không mâu thuẫn với các mục trên.

Các quyết định không thể đồng thời thỏa hai nguồn mâu thuẫn được giải thích trong `MANUAL_REVIEW.md` và
`HUONG_DAN_TU_HOC_VA_GO_CODE.md` của từng bộ.

## Cách kiểm tra nhanh

Trong từng dự án độc lập, dùng JDK 21 và chạy:

```powershell
mvn clean test
mvn -DskipTests package
```

Hoặc chạy một lần cho cả sáu dự án từ thư mục `answers`:

```powershell
powershell -ExecutionPolicy Bypass -File .\verify-answers.ps1
```

Lượt kiểm tra cuối ngày 20/07/2026 trên JDK 21 đạt **26/26 phép kiểm thử**, không có phép kiểm thử thất bại, lỗi hoặc bị
bỏ qua. Phạm vi gồm kiểm thử đơn vị cho nghiệp vụ, JSON ngày tháng và ngữ cảnh/CORS của hai cổng API; kết quả này không
thay thế bước kiểm tra nhanh với SQL Server thật.

Để tạo lại sáu ZIP dự án sạch và hai ZIP tổng:

```powershell
powershell -ExecutionPolicy Bypass -File .\package-submissions.ps1
```

Sau khi chuẩn bị SQL Server:

1. Chạy dịch vụ chính ở cổng `8081`.
2. Chạy dịch vụ phụ thuộc ở cổng `8082`.
3. Chạy cổng API ở cổng `8080`.
4. Kiểm thử API trực tiếp và qua cổng API.

Không đưa `.pegen`, file đề Word, `target`, `.idea`, `.git` hay chính thư mục `answers` vào bài nộp. Theo đề, mỗi dự án
phải được nén thành một file ZIP riêng; sau đó ghép ba file ZIP vào một file ZIP tổng nếu EOS yêu cầu.

## Đổi MSSV

Tên dự án, lớp khởi động, bộ điều khiển và gói mã nguồn đều đang chứa `SE180211`/`se180211`. Nếu MSSV thực tế khác, cách
an toàn nhất là mở tiện ích, nhập đúng MSSV và bấm `Generate` để tạo lại. Không chỉ đổi tên thư mục vì gói và lớp bên
trong cũng là tiêu chí bắt buộc.
