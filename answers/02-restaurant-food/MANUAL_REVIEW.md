# RÀ SOÁT THỦ CÔNG — MSS301 PE

> Tệp này được tạo khi sinh đáp án. Không nộp bài trước khi hoàn thành mọi ô kiểm tra.

## Kết quả nhận diện

- Hồ sơ đã chọn: `RESTAURANT_FOOD`
- Kết quả tự động: `RESTAURANT_FOOD` (độ tin cậy 97%)
- MSSV/đoạn tên gói: `se180211`
- Cơ sở dữ liệu: `MSS301_2026_PE`

Tài liệu đầu vào đã dùng:

- `D:\BaoHuy\MSS301_Exam_Paper.docx`
- `D:\BaoHuy\create_category_table_sql.txt`
- `D:\BaoHuy\create_restaurant_table_sql.txt`
- `D:\BaoHuy\create_foods_table_sql.txt`

Căn cứ phân loại:

- Phát hiện các bảng Category, Restaurant và Food trong lược đồ SQL.
- Đặc tả cũng mô tả miền nghiệp vụ Restaurant/Food.

## Lược đồ đã phân tích

### `dbo.Category`

| Cột           | Kiểu SQL      | Cho phép null |      ID | Duy nhất | Giá trị cho phép | Quan hệ |
|---------------|---------------|--------------:|--------:|---------:|------------------|---------|
| `category_id` | INT           |         không | tự tăng |          |                  |         |
| `name`        | NVARCHAR(100) |         không |         |       có |                  |         |

### `dbo.restaurants`

| Cột             | Kiểu SQL     | Cho phép null |      ID | Duy nhất | Giá trị cho phép | Quan hệ                           |
|-----------------|--------------|--------------:|--------:|---------:|------------------|-----------------------------------|
| `restaurant_id` | INT          |         không | tự tăng |          |                  |                                   |
| `address`       | VARCHAR(100) |         không |         |          |                  |                                   |
| `open_date`     | DATETIME2    |         không |         |          |                  |                                   |
| `name`          | VARCHAR(100) |         không |         |       có |                  |                                   |
| `owner_name`    | VARCHAR(100) |         không |         |          |                  |                                   |
| `phone`         | VARCHAR(11)  |         không |         |          |                  |                                   |
| `price_from`    | INT          |            có |         |          |                  |                                   |
| `price_to`      | INT          |            có |         |          |                  |                                   |
| `status`        | VARCHAR(10)  |         không |         |          |                  |                                   |
| `category_id`   | INT          |         không |         |          |                  | `Category.category_id` (suy luận) |

### `dbo.Foods`

| Cột             | Kiểu SQL      | Cho phép null |      ID | Duy nhất | Giá trị cho phép     | Quan hệ                                |
|-----------------|---------------|--------------:|--------:|---------:|----------------------|----------------------------------------|
| `food_id`       | INT           |         không | tự tăng |          |                      |                                        |
| `name`          | NVARCHAR(100) |         không |         |          |                      |                                        |
| `price`         | INT           |         không |         |          |                      |                                        |
| `ingredient`    | NVARCHAR(500) |         không |         |          |                      |                                        |
| `restaurant_id` | INT           |         không |         |          |                      | `restaurants.restaurant_id` (suy luận) |
| `status`        | NVARCHAR(20)  |         không |         |          | `ACTIVE`, `INACTIVE` |                                        |

Ràng buộc `CHECK` đã phân tích (cần đối chiếu với kiểm tra hợp lệ trong Java và DDL đã tạo):

- `[status] IN ('ACTIVE', 'INACTIVE')`

## Các mục vẫn cần người làm bài quyết định hoặc xác nhận

- [ ] Đã khôi phục dấu phẩy bị thiếu trước `PRIMARY KEY` trong bảng `dbo.restaurants`.
- [ ] Quan hệ `dbo.restaurants.category_id -> dbo.Category.category_id` được suy luận theo quy ước tên `*_id`; cần xác
  nhận thủ công.
- [ ] Quan hệ `dbo.Foods.restaurant_id -> dbo.restaurants.restaurant_id` được suy luận theo quy ước tên `*_id`; cần xác
  nhận thủ công.
- [ ] Câu chữ và ví dụ Restaurant/Food có mâu thuẫn về DTO và phân trang; mã nguồn dùng đủ `FoodDTO`, `FoodResponseDTO`
  và `FoodListDTO`.
- [ ] SQL dùng `ingredient`, trong khi đề dùng `ingredients`; trường Java là `ingredients` và `@Column` ánh xạ tới
  `ingredient`.
- [ ] `openDate` bị mâu thuẫn: ví dụ dùng `20/05/2025`, SQL dùng `DATETIME2`, còn R04 yêu cầu ISO-8601. JSON trả ra tuân
  theo R04, ví dụ `2026-03-14T10:00:00Z`; bộ giải tuần tự hóa đầu vào chấp nhận cả hai dạng.
- [ ] Đề bỏ sót `categoryId` trong `RestaurantDTO`, mặc dù SQL bắt buộc `category_id`; DTO có thêm `categoryId` để thao
  tác tạo/cập nhật thỏa lược đồ.
- [ ] Độ dài tên Restaurant bị mâu thuẫn: bảng trong đề ghi 50, SQL được phát ghi 100; kiểu vật lý và kiểm tra hợp lệ
  bám SQL, tức 100 ký tự.
- [ ] Phần mô tả danh sách Restaurant nhắc đến `status`, nhưng bảng tham số truy vấn ghi `ownerName`; điểm cuối bám bảng
  tham số, tức `name + ownerName`.
- [ ] `status` của Restaurant/Food không có `DEFAULT` trong SQL và đề yêu cầu đủ trường bắt buộc; API tạo mới vì vậy yêu
  cầu và giữ giá trị `ACTIVE`/`INACTIVE` được gửi lên.
- [ ] Các biểu thức `CHECK` SQL tổng quát không phải lúc nào cũng biểu diễn được bằng cấu hình sinh mã; ưu tiên ba SQL
  chính thức trong thư mục `database` và đối chiếu kiểm tra hợp lệ trong Java.
- [ ] Quan hệ được suy luận, không phải `FOREIGN KEY` tường minh: `restaurants.category_id -> Category.category_id`.
- [ ] Quan hệ được suy luận, không phải `FOREIGN KEY` tường minh: `Foods.restaurant_id -> restaurants.restaurant_id`.
- [ ] PUT một phần hiện xem JSON `null` giống trường bị bỏ qua, nên không thể dùng `null` để xóa tường minh giá trị cột
  nullable. Nếu bộ kiểm thử yêu cầu xóa bằng `null`, cần DTO cập nhật nhận biết sự hiện diện của trường, chẳng hạn
  `JsonNullable`.
- [ ] Biên dịch Maven là cổng kiểm tra cuối bắt buộc. Bộ đáp án đã vượt qua kiểm thử/đóng gói, nhưng vẫn phải thử lại
  trên máy thi và cơ sở dữ liệu thật.

## Các dự án đáp án

- `SE180211RestaurantService`
- `SE180211FoodService`
- `SE180211FoodyGateway`

Hai dịch vụ dữ liệu dùng Spring Web, Spring Data JPA, Validation, MS SQL Server Driver, Lombok, DevTools và Springdoc.
Dịch vụ phụ thuộc còn dùng OpenFeign. Gateway chỉ dùng các thư viện phụ thuộc cần cho định tuyến, CORS và OpenAPI.

## Kiểm tra cuối bắt buộc

- [ ] Mở từng thư mục đáp án thành một dự án Maven độc lập trong IntelliJ.
- [ ] Đặt Project SDK và trình chạy Maven là JDK 21.
- [ ] Bật xử lý chú thích nếu IntelliJ báo lỗi Lombok.
- [ ] Chạy `mvn clean test` trong từng dự án.
- [ ] Ưu tiên ba SQL chính thức trong thư mục `database`; không ghi đè cơ sở dữ liệu thi đã có dữ liệu.
- [ ] Chỉ dùng `database/MSS301_2026_PE.sql` trên cơ sở dữ liệu mới và đối chiếu từng bảng/cột với đề chính thức.
- [ ] Khởi động Restaurant Service (8081), Food Service (8082), rồi Foody Gateway (8080).
- [ ] Thử tạo, cập nhật, xem chi tiết, vô hiệu hóa, lấy danh sách, mọi lỗi kiểm tra hợp lệ, trùng dữ liệu và ánh xạ
  trạng thái không tìm thấy.
- [ ] Thử mọi tuyến công khai qua cổng 8080 và xác minh CORS.
- [ ] Nén riêng từng dự án, không kèm `target`, `.idea` hoặc `.git`; tuân thủ chính xác hướng dẫn đóng gói của EOS trong
  đề.

Thời điểm sinh bản gốc: `2026-07-20T14:23:44.042877200Z`.
