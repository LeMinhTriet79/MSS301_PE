# Hướng dẫn tự học và tự gõ mã nguồn — đề Restaurant / Food / Foody Gateway

Tài liệu này đi cùng ba dự án đáp án:

1. `SE180211RestaurantService` — cổng `8081`;
2. `SE180211FoodService` — cổng `8082`;
3. `SE180211FoodyGateway` — cổng `8080`.

Mục tiêu của tài liệu không phải chỉ hướng dẫn sao chép. Mỗi bước đều giải thích **vì sao cần tệp đó**, **luồng chạy đi
qua đâu**, **cách tự kiểm tra**, và **cách xử lý những chỗ đề Word mâu thuẫn với SQL**.

> Quan trọng: không ai có thể cam kết tuyệt đối “100%” khi bản thân đề có các thông tin mâu thuẫn. Bộ đáp án này ưu tiên
> yêu cầu có thể chấm tự động, lược đồ SQL được cung cấp, bảng điểm cuối API, rồi mới đến ví dụ minh họa. Mọi quyết định
> đã được ghi rõ ở mục 2.

---

## 1. Đọc đề thành danh sách kiểm tra trước khi viết mã

### 1.1. Danh sách yêu cầu bắt buộc, dễ bị chấm 0

- JDK 21.
- Spring Boot `3.5.11`.
- Kiểu đóng gói Maven là `jar`.
- Tên dự án đúng mẫu có MSSV.
- Gói gốc là `fu.<studentId viết thường>.<servicename>`.
- Dùng SQL Server và cơ sở dữ liệu `MSS301_2026_PE`.
- Tên người dùng/mật khẩu đều là `sa`.
- Không đặt đường dẫn ngữ cảnh khác mặc định.
- Restaurant Service chạy cổng `8081`.
- Food Service chạy cổng `8082`.
- Foody Gateway chạy cổng `8080`.
- Các bảng và cột vật lý phải đúng tệp SQL.
- Yêu cầu/phản hồi JSON có `Content-Type: application/json` khi có thân yêu cầu.
- Pagination dùng `page` bắt đầu từ 0 và `size`, mặc định `0/10`, tối đa `100`.
- Cấu trúc tầng/gói phải có `entity`, `repository`, `service`, `service.impl`, `controller`, `dto`, `config`, `common`.

### 1.2. Ma trận dịch vụ

| Dự án              | Trách nhiệm                                 | Cơ sở dữ liệu             | Gọi dịch vụ khác                                            |
|--------------------|---------------------------------------------|---------------------------|-------------------------------------------------------------|
| Restaurant Service | CRUD mềm Restaurant, lấy danh sách Category | `Category`, `restaurants` | Không cần gọi dịch vụ khác                                  |
| Food Service       | CRUD mềm Food                               | `Foods`                   | OpenFeign gọi Restaurant Service để kiểm tra/lấy Restaurant |
| Foody Gateway      | Điểm vào duy nhất, CORS, tổng hợp Swagger   | Không sở hữu dữ liệu      | Chuyển yêu cầu sang cổng 8081/8082                          |

### 1.3. Quy ước trạng thái trong `ApiResponseDTO`

| Trạng thái |         HTTP | Ý nghĩa                                 |
|-----------:|-------------:|-----------------------------------------|
|        `0` |          500 | Lỗi hệ thống                            |
|        `1` | 200 hoặc 201 | Thành công                              |
|        `2` |          400 | Dữ liệu/phan trang không hợp lệ         |
|        `3` |          400 | Trùng tên Restaurant                    |
|        `4` |          400 | Không tìm thấy ID/Restaurant tham chiếu |

Phản hồi luôn có đủ ba trường:

```json
{
  "status": 1,
  "message": "Successful",
  "data": {}
}
```

Khi lỗi hoặc DELETE thành công, `data` vẫn xuất hiện với giá trị `null`. Điều này bám đúng mô tả `data` có thể null.

---

## 2. Các mâu thuẫn Word–SQL và quyết định đã dùng

Đây là phần nên học kỹ. Trong PE, không nên âm thầm đoán; hãy xác định nguồn nào có khả năng được giám khảo/bộ chấm tự
động dùng.

| Mâu thuẫn                                                                                                                             | Quyết định trong đáp án                                                                                                    | Lý do                                                                                                     |
|---------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| Word ghi `restaurants.name` NVARCHAR(50), SQL thật là VARCHAR(100) và UNIQUE                                                          | Thực thể/kiểm tra hợp lệ dùng `length = 100`, DDL dùng VARCHAR(100)                                                        | Tệp SQL là lược đồ vật lý mà ứng dụng thật kết nối tới; R01 yêu cầu kiểm tra hợp lệ theo cột DB           |
| Word `RestaurantDTO` không liệt kê `categoryId`, nhưng SQL bắt buộc `category_id NOT NULL`                                            | Thêm `categoryId` vào DTO                                                                                                  | Không có trường này thì POST không thể tạo bản ghi hợp lệ                                                 |
| Ví dụ `openDate = 20/05/2025`, nhưng R04 yêu cầu ISO-8601 UTC                                                                         | Đầu vào chấp nhận cả hai; đầu ra luôn là ISO, ví dụ `2026-03-14T10:00:00Z`                                                 | Hòa giải được cả ví dụ và quy tắc chung mà không làm phản hồi sai R04                                     |
| Word ghi Food là `ingredients`, SQL đặt cột `ingredient`                                                                              | Java/JSON dùng `ingredients`; JPA dùng `@Column(name = "ingredient")`                                                      | Giữ đúng API đồng thời đúng tên cột chấm DB                                                               |
| Word nói danh sách Restaurant lọc theo `name`/`status` nhưng bảng truy vấn ghi `name`/`ownerName`                                     | Dùng `name` và `ownerName`                                                                                                 | Bảng tham số truy vấn chi tiết đáng tin hơn câu mô tả chung                                               |
| Word nói danh sách Food lọc theo `name`/`status` nhưng bảng truy vấn ghi `name`/`ingredients`                                         | Dùng `name` và `ingredients`                                                                                               | Cùng nguyên tắc trên                                                                                      |
| Chi tiết Food ghi trả FoodDTO, nhưng đề định nghĩa riêng `FoodResponseDTO`; phần danh sách nói PageDTO nhưng định nghĩa `FoodListDTO` | Chi tiết trả `FoodResponseDTO`, danh sách trả `FoodListDTO`; tạo/cập nhật trả `FoodDTO`                                    | Nếu không dùng hai DTO được đề định nghĩa riêng thì chúng trở nên vô nghĩa                                |
| Word ghi `ingredients`, SQL ghi `ingredient`; Word không cho độ dài tối đa của `status`, SQL dùng NVARCHAR(20)                        | Thực thể bám SQL, DTO bám tên JSON của Word                                                                                | Tránh lỗi `invalid column` và vẫn đúng dữ liệu JSON                                                       |
| Chú thích SQL nói FK nhưng cả ba tập lệnh không tạo FOREIGN KEY                                                                       | Không tự thêm FK vào DDL; kiểm tra Category cục bộ và Restaurant qua Feign trong Java                                      | Không thay đổi lược đồ được phát; vẫn bảo vệ tính toàn vẹn nghiệp vụ                                      |
| Tập lệnh `restaurants` thiếu dấu phẩy sau `[category_id] INT NOT NULL`                                                                | Tệp `database/MSS301_2026_PE.sql` đã sửa dấu phẩy                                                                          | Nếu chạy nguyên văn tập lệnh lỗi, SQL Server báo lỗi cú pháp trước PRIMARY KEY                            |
| Đề ghi “Department ID/Department not found” trong phần Food                                                                           | Trả thông báo đúng miền nghiệp vụ là `Restaurant ID is not found`/`Food is not found` nhưng giữ trạng thái `4`, HTTP `400` | “Department” là lỗi sao chép rõ ràng                                                                      |
| PUT một phần và JSON `null`                                                                                                           | Mã nguồn cơ bản xem `null` như trường bị bỏ qua                                                                            | Jackson DTO thường không phân biệt “không gửi trường” và “gửi trường: null”; xem mục 16 nếu muốn nâng cấp |

### Thứ tự ưu tiên khi gặp một mâu thuẫn mới

1. Tên dự án/gói/cổng/cơ sở dữ liệu và các mục được đề cảnh báo chấm 0.
2. Tệp SQL thật được phát.
3. Bảng điểm cuối API: phương thức, URL, mã HTTP, trạng thái.
4. Bảng DTO.
5. Common rules R01–R04.
6. Câu mô tả và JSON ví dụ.

---

## 3. Chuẩn bị máy trước khi thi

### 3.1. Phần mềm

- IntelliJ IDEA có Maven support.
- JDK 21; cả Project SDK và trình chạy Maven đều chọn đúng JDK 21.
- SQL Server 2016 trở lên.
- SQL Server Management Studio hoặc Azure Data Studio.
- Các thư viện phụ thuộc Maven đã tải sẵn vào `%USERPROFILE%\.m2\repository` nếu lúc thi không có Internet.

Kiểm tra:

```powershell
java -version
mvn -version
```

Cả hai lệnh phải hiển thị Java 21.

### 3.2. Quy tắc đổi MSSV

Đáp án mẫu dùng `SE180211`/`se180211`. Nếu MSSV của bạn là `SE123456`:

- thay `SE180211` bằng `SE123456` trong tên thư mục, `artifactId`, `name`, tên ứng dụng và tên controller;
- thay `se180211` bằng `se123456` trong gói và đường dẫn thư mục Java;
- giữ `fu` và tên dịch vụ viết thường: `restaurant`, `food`, `gateway`;
- sau khi đổi, dùng **Edit → Find → Find in Files** tìm lại cả `SE180211` và `se180211`; kết quả phải bằng 0;
- bấm nạp lại Maven, sau đó chạy `mvn clean test` cho từng dự án.

Ví dụ gói hợp lệ:

```text
fu.se123456.restaurant
fu.se123456.food
fu.se123456.gateway
```

Không viết gói là `fu.SE123456...` vì đề yêu cầu `<studentId>` viết thường.

---

## 4. Tạo cơ sở dữ liệu đúng thứ tự

### 4.1. Cách nhanh và an toàn

Mở [database/MSS301_2026_PE.sql](database/MSS301_2026_PE.sql), kết nối SQL Server với tài khoản `sa`, chạy toàn bộ tập
lệnh.

Tập lệnh này:

- chỉ tạo cơ sở dữ liệu nếu chưa tồn tại;
- không `DROP` bảng;
- dừng bằng `THROW` nếu bảng đích đã tồn tại;
- tạo đúng `Category`, `restaurants`, `Foods`;
- đã sửa lỗi thiếu dấu phẩy của tập lệnh Restaurant;
- không tự tạo FOREIGN KEY mà ba tệp SQL gốc không khai báo.

### 4.2. Tạo dữ liệu Category để kiểm thử

Đề chỉ cho API `GET /api/categories`, không cho API tạo Category, và SQL không có câu INSERT. Vì
`restaurants.category_id` bắt buộc, cần ít nhất một Category để thử POST Restaurant.

Sau khi tạo lược đồ, chạy [database/SEED_CATEGORIES_OPTIONAL.sql](database/SEED_CATEGORIES_OPTIONAL.sql). Đây chỉ là dữ
liệu kiểm thử tùy chọn; chạy lặp lại không tạo tên trùng.

Kiểm tra:

```sql
USE MSS301_2026_PE;

SELECT * FROM dbo.Category;
SELECT * FROM dbo.restaurants;
SELECT * FROM dbo.Foods;
```

Ghi lại `category_id` thật để dùng trong POST Restaurant.

### 4.3. Cách ánh xạ SQL sang Java cần nhớ

| SQL                 | Java                                 | Ghi chú                                         |
|---------------------|--------------------------------------|-------------------------------------------------|
| `INT IDENTITY`      | `Long` + `@GeneratedValue(IDENTITY)` | Đề yêu cầu ID kiểu `long`                       |
| `INT` cho phép null | `Integer`                            | Không dùng `int`, vì `int` không biểu diễn null |
| `VARCHAR/NVARCHAR`  | `String`                             | Ghi đúng `length`                               |
| `DATETIME2`         | `java.util.Date`                     | Bảng DTO yêu cầu đúng loại này                  |
| snake_case          | camelCase                            | Gắn `@Column(name = "...")`                     |

---

## 5. Tạo ba dự án bằng Spring Initializr

Tạo ba dự án **riêng biệt**, không tạo cấu trúc đa mô-đun. Đề yêu cầu nén từng thư mục dự án.

### 5.1. Restaurant Service

Trong IntelliJ: chọn **File → New → Project → Spring Boot** (đây là nhãn tiếng Anh nguyên bản của giao diện).

| Tùy chọn trên giao diện     | Giá trị                     |
|-----------------------------|-----------------------------|
| Ngôn ngữ (`Language`)       | Java                        |
| Loại (`Type`)               | Maven                       |
| JDK                         | 21                          |
| Java                        | 21                          |
| Kiểu đóng gói (`Packaging`) | Jar                         |
| Spring Boot                 | 3.5.11                      |
| Nhóm (`Group`)              | `fu.se180211`               |
| Mã tạo phẩm (`Artifact`)    | `SE180211RestaurantService` |
| Tên (`Name`)                | `SE180211RestaurantService` |
| Gói (`Package`)             | `fu.se180211.restaurant`    |

Chọn các thư viện phụ thuộc:

- Spring Web;
- Spring Data JPA;
- Validation;
- MS SQL Server Driver;
- Lombok;
- Spring Boot DevTools;
- OpenFeign — đề có liệt kê khung công nghệ này trong phần Restaurant;
- thêm Springdoc WebMVC trong `pom.xml` vì Initializr thường không liệt kê.

### 5.2. Food Service

| Tùy chọn trên giao diện           | Giá trị               |
|-----------------------------------|-----------------------|
| Nhóm (`Group`)                    | `fu.se180211`         |
| Mã tạo phẩm/Tên (`Artifact/Name`) | `SE180211FoodService` |
| Gói (`Package`)                   | `fu.se180211.food`    |
| Spring Boot/Java/Kiểu đóng gói    | 3.5.11 / 21 / Jar     |

Các thư viện phụ thuộc giống Restaurant và bắt buộc có OpenFeign vì Food phải lấy RestaurantDTO từ cổng 8081.

### 5.3. Foody Gateway

| Tùy chọn trên giao diện           | Giá trị                |
|-----------------------------------|------------------------|
| Nhóm (`Group`)                    | `fu.se180211`          |
| Mã tạo phẩm/Tên (`Artifact/Name`) | `SE180211FoodyGateway` |
| Gói (`Package`)                   | `fu.se180211.gateway`  |
| Spring Boot/Java/Kiểu đóng gói    | 3.5.11 / 21 / Jar      |

Các thư viện phụ thuộc:

- Spring Cloud Gateway Server WebFlux;
- Springdoc OpenAPI WebFlux UI;
- Spring Boot Starter Test.

Không thêm JPA/MSSQL vào gateway: gateway không sở hữu bảng. Nếu thêm, Spring Boot có thể cố tự cấu hình nguồn dữ liệu
không cần thiết và gateway khó khởi động. Không thêm Security chỉ để cấu hình CORS; một `CorsWebFilter` là đủ.

### 5.4. Phiên bản cần có trong POM

```xml
<properties>
    <java.version>21</java.version>
    <spring-cloud.version>2025.0.2</spring-cloud.version>
    <springdoc.version>2.8.14</springdoc.version>
</properties>
```

Các dự án dùng Spring Cloud cần nhập BOM:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Springdoc cho MVC:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

Gateway thay `webmvc-ui` bằng `springdoc-openapi-starter-webflux-ui`.

Sau khi sửa POM, bấm **Load Maven Changes** (nạp thay đổi Maven). Nếu Lombok đỏ dù Maven xây dựng được, chọn *
*Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing**; đây là các
nhãn nguyên bản của giao diện IntelliJ.

---

## 6. Cây thư mục cần tạo

### 6.1. Restaurant Service

```text
SE180211RestaurantService/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/fu/se180211/restaurant/
    │   │   ├── SE180211RestaurantServiceApplication.java
    │   │   ├── common/
    │   │   │   ├── BusinessException.java
    │   │   │   ├── FlexibleDateDeserializer.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   └── ResponseStatuses.java
    │   │   ├── config/
    │   │   │   ├── CorsConfig.java
    │   │   │   └── OpenApiConfig.java
    │   │   ├── controller/
    │   │   │   ├── CategoryController.java
    │   │   │   └── SE180211RestaurantController.java
    │   │   ├── dto/
    │   │   │   ├── ApiResponseDTO.java
    │   │   │   ├── CategoryDTO.java
    │   │   │   ├── PageDTO.java
    │   │   │   └── RestaurantDTO.java
    │   │   ├── entity/
    │   │   │   ├── Category.java
    │   │   │   └── Restaurant.java
    │   │   ├── repository/
    │   │   │   ├── CategoryRepository.java
    │   │   │   └── RestaurantRepository.java
    │   │   └── service/
    │   │       ├── CategoryService.java
    │   │       ├── RestaurantService.java
    │   │       └── impl/
    │   │           ├── CategoryServiceImpl.java
    │   │           └── RestaurantServiceImpl.java
    │   └── resources/application.properties
    └── test/java/fu/se180211/restaurant/...
```

### 6.2. Food Service

```text
SE180211FoodService/
├── pom.xml
└── src/main/
    ├── java/fu/se180211/food/
    │   ├── SE180211FoodServiceApplication.java
    │   ├── common/{BusinessException,GlobalExceptionHandler,ResponseStatuses}.java
    │   ├── config/{CorsConfig,OpenApiConfig,RestaurantClient}.java
    │   ├── controller/SE180211FoodController.java
    │   ├── dto/{ApiResponseDTO,FoodDTO,FoodResponseDTO,FoodListDTO,PageDTO,RestaurantDTO}.java
    │   ├── entity/Food.java
    │   ├── repository/FoodRepository.java
    │   └── service/
    │       ├── FoodService.java
    │       └── impl/FoodServiceImpl.java
    └── resources/application.properties
```

`PageDTO` trong dự án Food không được điểm cuối Food dùng trực tiếp, nhưng được giữ để đối chiếu quy ước của đề. DTO
thực tế cho danh sách Food là `FoodListDTO`.

### 6.3. Gateway

```text
SE180211FoodyGateway/
├── pom.xml
└── src/main/
    ├── java/fu/se180211/gateway/
    │   ├── SE180211FoodyGatewayApplication.java
    │   ├── config/{CorsConfig,GatewayRouteConfig,OpenApiConfig}.java
    │   ├── common/package-info.java
    │   ├── controller/package-info.java
    │   ├── dto/package-info.java
    │   ├── entity/package-info.java
    │   ├── repository/package-info.java
    │   └── service/
    │       ├── package-info.java
    │       └── impl/package-info.java
    └── resources/application.properties
```

Các `package-info.java` làm hiện diện đúng cấu trúc gói được đề liệt kê, nhưng không tạo `entity`/`repository` không cần
thiết cho gateway.

---

## 7. Gõ mã nguồn Restaurant Service theo thứ tự

### Bước R1 — `application.properties`

Gõ đúng các dòng cốt lõi sau:

```properties
spring.application.name=SE180211RestaurantService
server.port=8081

spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=MSS301_2026_PE;encrypt=false;
spring.datasource.username=sa
spring.datasource.password=sa
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

spring.jpa.hibernate.ddl-auto=none
spring.jpa.open-in-view=false
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect

spring.jackson.time-zone=UTC
spring.jackson.date-format=yyyy-MM-dd'T'HH:mm:ssX
spring.jackson.serialization.write-dates-as-timestamps=false

springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

Giải thích:

- `ddl-auto=none`: không để Hibernate đổi lược đồ thi.
- `encrypt=false`: đúng URL đề cho môi trường cục bộ.
- `open-in-view=false`: `controller` không vô tình truy vấn DB ngoài giao dịch.
- UTC + ISO: bám R04.

Tự kiểm tra: mở tệp
thật [SE180211RestaurantService/src/main/resources/application.properties](SE180211RestaurantService/src/main/resources/application.properties).

### Bước R2 — Lớp chính

```java
@SpringBootApplication
@EnableFeignClients
public class SE180211RestaurantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SE180211RestaurantServiceApplication.class, args);
    }
}
```

Lớp phải nằm ngay gói gốc `fu.se180211.restaurant`; nếu để sâu hơn, quá trình quét thành phần có thể bỏ sót
controller/repository.

### Bước R3 — Thực thể `Category`

```java
@Getter
@Setter
@Entity
@Table(name = "Category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;
}
```

Ba chi tiết phải gõ đúng: bảng số ít `Category`, cột `category_id`, và tên duy nhất với độ dài 100.

### Bước R4 — Thực thể `Restaurant`

Gõ các trường theo đúng ánh xạ:

```java
@Getter
@Setter
@Entity
@Table(name = "restaurants")
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String owner;

    @Column(name = "address", nullable = false, length = 100)
    private String address;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "open_date", nullable = false)
    private Date openDate;

    @Column(name = "price_from")
    private Integer priceFrom;

    @Column(name = "price_to")
    private Integer priceTo;

    @Column(name = "phone", nullable = false, length = 11)
    private String phone;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;
}
```

Không tự thêm `@ManyToOne` ở đây. SQL không khai báo FK và đề cần DTO chứa ID đơn giản; giữ `categoryId` giúp mã nguồn
thi dễ kiểm soát hơn. Việc Category tồn tại được dịch vụ kiểm bằng `CategoryRepository.existsById`.

### Bước R5 — Kho truy cập dữ liệu (`repository`)

```java
public interface CategoryRepository extends JpaRepository<Category, Long> {}
```

```java
public interface RestaurantRepository
        extends JpaRepository<Restaurant, Long>, JpaSpecificationExecutor<Restaurant> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndRestaurantIdNot(String name, Long restaurantId);
}
```

`JpaSpecificationExecutor` dùng để ghép bộ lọc tùy chọn. Phương thức thứ hai tránh báo trùng chính bản ghi đang cập
nhật.

### Bước R6 — DTO

`RestaurantDTO` có các trường:

```java
private Long restaurantId;
@Size(max = 100) private String name;
@Size(max = 100) private String owner;
private Integer priceFrom;
private Integer priceTo;
@Size(max = 11) private String phone;
@Size(max = 100) private String address;
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
@JsonDeserialize(using = FlexibleDateDeserializer.class)
private Date openDate;
@Size(max = 10) private String status;
private Long categoryId;
```

Dùng kiểu bao `Long/Integer`, không dùng kiểu nguyên thủy, vì PUT là cập nhật một phần: `null` biểu thị không cập nhật
trường đó.

`CategoryDTO` chỉ có `categoryId`, `name`. `PageDTO<T>` phải có đúng:

```text
size, page, totalPages, totalElements, first, last, content
```

Hàm tạo nhận `Page<T>` và sao chép `getSize()`, `getNumber()`, `getTotalPages()`, `getTotalElements()`, `isFirst()`,
`isLast()`, `getContent()`.

`ApiResponseDTO<T>` phải có các phương thức tạo đối tượng:

```java
public static <T> ApiResponseDTO<T> success(T data) {
    return new ApiResponseDTO<>(1, data);
}

public static <T> ApiResponseDTO<T> error(int status, String message) {
    return new ApiResponseDTO<>(status, message, null);
}
```

Đối chiếu mã nguồn đầy đủ
tại [dto/ApiResponseDTO.java](SE180211RestaurantService/src/main/java/fu/se180211/restaurant/dto/ApiResponseDTO.java)
và [dto/PageDTO.java](SE180211RestaurantService/src/main/java/fu/se180211/restaurant/dto/PageDTO.java).

### Bước R7 — Hòa giải định dạng ngày

Tạo `common/FlexibleDateDeserializer.java`. Thuật toán:

1. thử `Instant.parse(value)` cho chuỗi có `Z`;
2. thử `OffsetDateTime.parse(value)` cho chuỗi có độ lệch múi giờ như `+07:00`;
3. thử `LocalDate.parse(value, "dd/MM/uuuu")` cho định dạng trong ví dụ;
4. nếu cả ba thất bại, ném `InvalidFormatException` để bộ xử lý trả HTTP 400/trạng thái 2.

Đoạn cốt lõi:

```java
try {
    return Date.from(Instant.parse(value));
} catch (DateTimeParseException ignored) { }

try {
    return Date.from(OffsetDateTime.parse(value).toInstant());
} catch (DateTimeParseException ignored) { }

LocalDate date = LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/uuuu"));
return Date.from(date.atStartOfDay().toInstant(ZoneOffset.UTC));
```

Đầu ra vẫn theo `@JsonFormat` ISO UTC, nên R04 không bị phá.

### Bước R8 — Quy ước ngoại lệ

`ResponseStatuses` là lớp chứa các hằng số `0..4`. Ngoài thông báo, `BusinessException` giữ hai phần:

```java
private final int apiStatus;
private final HttpStatus httpStatus;
```

`GlobalExceptionHandler` có ba nhánh:

1. `BusinessException` → dùng HTTP/trạng thái/thông báo do dịch vụ quyết định;
2. lỗi kiểm tra hợp lệ, JSON sai, kiểu tham số đường dẫn sai → HTTP 400/trạng thái 2;
3. lỗi không dự kiến → HTTP 500/trạng thái 0 và không trả ngăn xếp lỗi cho máy khách.

Điểm quan trọng: bộ xử lý phải dùng `ex.getMessage()` cho lỗi nghiệp vụ; nếu luôn sinh thông báo chung, lỗi
`Name is duplicated` và `Restaurant is not found` không còn đúng bảng điểm cuối API.

### Bước R9 — Giao diện dịch vụ

```java
public interface RestaurantService {
    RestaurantDTO create(RestaurantDTO dto);
    RestaurantDTO update(Long restaurantId, RestaurantDTO dto);
    RestaurantDTO get(Long restaurantId);
    void deactivate(Long restaurantId);
    PageDTO<RestaurantDTO> list(Integer page, Integer size, String name, String ownerName);
}
```

```java
public interface CategoryService {
    PageDTO<CategoryDTO> list(Integer page, Integer size);
}
```

### Bước R10 — `RestaurantServiceImpl.create`

Thứ tự xử lý rất quan trọng:

1. kiểm tra hợp lệ mọi cột `NOT NULL`, độ dài tối đa và trạng thái;
2. kiểm `categoryId` dương và tồn tại;
3. loại khoảng trắng thừa của tên và kiểm tra trùng không phân biệt hoa/thường;
4. ánh xạ DTO → thực thể;
5. chuẩn hóa trạng thái thành chữ hoa;
6. lưu rồi ánh xạ thực thể đã có ID → DTO.

Khung mã nguồn:

```java
validateCreate(dto);
validateLookup(dto.getCategoryId());
String cleanName = clean(dto.getName());
if (repo.existsByNameIgnoreCase(cleanName)) {
    throw duplicate(); // status 3, HTTP 400
}

Restaurant entity = new Restaurant();
entity.setName(cleanName);
entity.setOwner(clean(dto.getOwner()));
// map các field còn lại
entity.setStatus(normalizeStatus(dto.getStatus()));
return toDTO(repo.save(entity));
```

Không gán `restaurantId` từ thân yêu cầu khi tạo mới; SQL Server phải tự sinh IDENTITY.

### Bước R11 — Cập nhật một phần

```java
Restaurant entity = findOrThrow(restaurantId);
validateUpdate(dto);

if (dto.getCategoryId() != null) validateLookup(dto.getCategoryId());
if (dto.getName() != null
        && repo.existsByNameIgnoreCaseAndRestaurantIdNot(clean(dto.getName()), restaurantId)) {
    throw duplicate();
}

if (dto.getName() != null) entity.setName(clean(dto.getName()));
if (dto.getOwner() != null) entity.setOwner(clean(dto.getOwner()));
// chỉ set những field khác null
return toDTO(repo.save(entity));
```

Luôn tìm thực thể trước. Nếu ID không có, trả HTTP 400/trạng thái 4. Không tạo bản ghi mới trong PUT.

### Bước R12 — Xem chi tiết, vô hiệu hóa và lấy danh sách

Chức năng xem chi tiết dùng `repo.findById(...).orElseThrow(...)`.

Vô hiệu hóa là xóa mềm:

```java
Restaurant entity = findOrThrow(restaurantId);
entity.setStatus("INACTIVE");
repo.save(entity);
```

Không gọi `repo.delete`, vì đề yêu cầu đặt trạng thái thành `INACTIVE`.

Lấy danh sách:

```java
int actualPage = page == null ? 0 : page;
int actualSize = size == null ? 10 : size;
if (actualPage < 0 || actualSize < 1 || actualSize > 100) throw validationFailed();

Specification<Restaurant> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    if (nameFilter != null) {
        predicates.add(cb.like(cb.lower(root.get("name")), "%" + nameFilter + "%"));
    }
    if (ownerFilter != null) {
        predicates.add(cb.like(cb.lower(root.get("owner")), "%" + ownerFilter + "%"));
    }
    return cb.and(predicates.toArray(new Predicate[0]));
};
```

Sau đó gọi `findAll(spec, PageRequest.of(..., Sort.by("restaurantId")))` và ánh xạ sang `PageDTO`.

### Bước R13 — Danh sách Category

`CategoryServiceImpl` cũng chuẩn hóa page/size và validate `0 <= page`, `1 <= size <= 100`, rồi:

```java
repo.findAll(PageRequest.of(page, size, Sort.by("categoryId")))
    .map(this::toDTO);
```

Không tạo POST/PUT/DELETE Category vì đề chỉ yêu cầu GET danh sách.

### Bước R14 — Bộ điều khiển (`controller`) đúng tên và đường dẫn

Phải dùng chính xác placeholder trong đề, không chỉ `/{id}`:

```java
@RestController
@RequestMapping(value = "/api/restaurants", produces = MediaType.APPLICATION_JSON_VALUE)
public class SE180211RestaurantController {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    // 201

    @PutMapping(value = "/{restaurantId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    // 200

    @GetMapping("/{restaurantId}")
    // 200

    @DeleteMapping("/{restaurantId}")
    // 200, data null

    @GetMapping
    // page, size, name, ownerName
}
```

Controller Category dùng `/api/categories`, tham số truy vấn `page`, `size`, và `produces=application/json`.

### Bước R15 — CORS và Swagger

`CorsConfig implements WebMvcConfigurer`, cho phép mọi mẫu nguồn, các phương thức `GET/POST/PUT/DELETE/OPTIONS` và mọi
tiêu đề HTTP.

`OpenApiConfig` tạo `OpenAPI` bean với title `SE180211RestaurantService API`.

Tự kiểm tra:

- `http://localhost:8081/v3/api-docs` trả JSON;
- `http://localhost:8081/swagger-ui.html` mở giao diện Swagger.

---

## 8. Gõ mã nguồn Food Service theo thứ tự

### Bước F1 — Tệp cấu hình và lớp chính

Tệp cấu hình giống Restaurant nhưng:

```properties
spring.application.name=SE180211FoodService
server.port=8082
restaurant.service.url=http://localhost:8081
```

Lớp chính:

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "fu.se180211.food.config")
public class SE180211FoodServiceApplication { ... }
```

Nếu quên `@EnableFeignClients`, FoodService không tạo được bean `RestaurantClient` và ứng dụng không khởi động.

### Bước F2 — Thực thể `Food`

```java
@Getter
@Setter
@Entity
@Table(name = "Foods")
public class Food {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "food_id")
    private Long foodId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "ingredient", nullable = false, length = 500)
    private String ingredients;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;
}
```

Đặc biệt: trường Java là `ingredients`, cột SQL là `ingredient`.

### Bước F3 — Kho truy cập dữ liệu (`repository`)

```java
public interface FoodRepository
        extends JpaRepository<Food, Long>, JpaSpecificationExecutor<Food> {
}
```

Không thêm phương thức kiểm tra trùng tên vì SQL `Foods.name` không có UNIQUE và điểm cuối không định nghĩa trạng thái 3
cho Food.

### Bước F4 — Phân biệt ba Food DTO

#### `FoodDTO` — yêu cầu tạo/cập nhật và phản hồi tạo/cập nhật

```text
foodId, name, price, ingredients, restaurantId, status
```

Giới hạn độ dài khi kiểm tra hợp lệ: `name` 100, `ingredients` 500, `status` 20.

#### `FoodResponseDTO` — chi tiết và phần tử trong danh sách

```text
foodId, name, price, ingredients, restaurant: RestaurantDTO
```

Không trả riêng `restaurantId`; thay bằng object Restaurant đầy đủ đúng bảng DTO.

#### `FoodListDTO`

```text
pageSize, pageNo, totalPages, first, last, foods: List<FoodResponseDTO>
```

Đề không liệt kê `totalElements` trong FoodListDTO nên không tự thêm.

Dự án Food cần bản sao `RestaurantDTO`, vì DTO truyền giữa hai dịch vụ không nên nhập lớp mã nguồn từ dự án Restaurant.
Bản sao phải có cùng trường JSON để Feign giải tuần tự hóa.

### Bước F5 — Máy khách Feign có kiểu dữ liệu cụ thể

```java
@FeignClient(
    name = "restaurant-service",
    url = "${restaurant.service.url:http://localhost:8081}")
public interface RestaurantClient {
    @GetMapping("/api/restaurants/{restaurantId}")
    ApiResponseDTO<RestaurantDTO> getRestaurant(
        @PathVariable("restaurantId") Long restaurantId);
}
```

Không dùng `Object` nếu đã biết kiểu phản hồi. `ApiResponseDTO<RestaurantDTO>` giúp trình biên dịch bắt lỗi và bỏ bước
chuyển đổi thủ công.

### Bước F6 — `FoodServiceImpl.create`

Thứ tự:

1. kiểm tra hợp lệ `name`, `price`, `ingredients`, `status` và `restaurantId`;
2. gọi Feign lấy Restaurant;
3. chỉ tiếp tục khi lời gọi HTTP thành công, trạng thái phản hồi = 1 và `data` khác null;
4. ánh xạ/lưu Food;
5. trả `FoodDTO`.

```java
validateCreate(dto);
Long restaurantId = resolveRefId(dto, true);
RestaurantDTO restaurant = fetchRef(restaurantId);

Food entity = new Food();
entity.setName(clean(dto.getName()));
entity.setPrice(dto.getPrice());
entity.setIngredients(clean(dto.getIngredients()));
entity.setStatus(normalizeStatus(dto.getStatus()));
entity.setRestaurantId(restaurant.getRestaurantId());
return toDTO(repo.save(entity), restaurant);
```

`fetchRef` ánh xạ lỗi:

- Restaurant trả HTTP 400/404 hoặc phản hồi không có `data` → HTTP 400/trạng thái 4;
- Feign hoặc mạng gặp lỗi khác → HTTP 500/trạng thái 0;
- không swallow `BusinessException` đã tạo.

### Bước F7 — Cập nhật/xem chi tiết/lấy danh sách

Cập nhật một phần chỉ gọi Feign khi yêu cầu có `restaurantId` mới. Nếu yêu cầu không đổi quan hệ, dịch vụ giữ
`restaurantId` hiện tại đã được kiểm lúc tạo; điều này tránh một lời gọi mạng không cần thiết. Chức năng xem chi
tiết/lấy danh sách vẫn gọi Restaurant Service vì phản hồi phải chứa RestaurantDTO mới nhất.

Xem chi tiết:

```java
Food entity = findOrThrow(foodId);
return toResponseDTO(entity, fetchRef(entity.getRestaurantId()));
```

Chức năng lấy danh sách tạo bộ lọc Specification theo `name` và trường thực thể `ingredients`, sau đó ánh xạ mỗi Food
sang `FoodResponseDTO` có Restaurant lồng bên trong.

> Hiệu năng: cách này có thể tạo N lời gọi Feign cho N Food. Với PE và mỗi trang tối đa 100 phần tử, mã nguồn rõ ràng
> được ưu tiên. Trong môi trường thực tế nên xử lý theo lô hoặc lưu đệm Restaurant.

### Bước F8 — Vô hiệu hóa

Không xóa row:

```java
Food entity = findOrThrow(foodId);
entity.setStatus("INACTIVE");
repo.save(entity);
```

### Bước F9 — Bộ điều khiển (`controller`)

```java
@RequestMapping(value = "/api/foods", produces = MediaType.APPLICATION_JSON_VALUE)
```

Các điểm cuối API:

- `POST /api/foods` → 201, `ApiResponseDTO<FoodDTO>`;
- `PUT /api/foods/{foodId}` → 200, `ApiResponseDTO<FoodDTO>`;
- `GET /api/foods/{foodId}` → 200, `ApiResponseDTO<FoodResponseDTO>`;
- `DELETE /api/foods/{foodId}` → 200, `data` bằng null;
- `GET /api/foods?page=&size=&name=&ingredients=` → `ApiResponseDTO<FoodListDTO>`.

Ghi chính xác `/{foodId}` và `@PathVariable("foodId")` để Swagger hiện đúng tên tham số của đề.

---

## 9. Gõ mã nguồn Foody Gateway

### Bước G1 — Tệp cấu hình

```properties
spring.application.name=SE180211FoodyGateway
server.port=8080

restaurant.service.url=http://localhost:8081
food.service.url=http://localhost:8082

springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.urls[0].name=Restaurant Service
springdoc.swagger-ui.urls[0].url=/restaurant-service/v3/api-docs
springdoc.swagger-ui.urls[1].name=Food Service
springdoc.swagger-ui.urls[1].url=/food-service/v3/api-docs
```

### Bước G2 — Cấu hình tuyến

Tiêm URL bằng `@Value`, không ghi cố định trong bốn tuyến:

```java
public GatewayRouteConfig(
        @Value("${restaurant.service.url:http://localhost:8081}") String restaurantServiceUrl,
        @Value("${food.service.url:http://localhost:8082}") String foodServiceUrl) {
    this.restaurantServiceUrl = restaurantServiceUrl;
    this.foodServiceUrl = foodServiceUrl;
}
```

Tạo bốn tuyến:

```java
return builder.routes()
    .route("restaurant-service", r -> r
        .path("/api/restaurants/**", "/api/categories/**")
        .uri(restaurantServiceUrl))
    .route("food-service", r -> r
        .path("/api/foods/**")
        .uri(foodServiceUrl))
    .route("restaurant-openapi", r -> r
        .path("/restaurant-service/v3/api-docs")
        .filters(f -> f.setPath("/v3/api-docs"))
        .uri(restaurantServiceUrl))
    .route("food-openapi", r -> r
        .path("/food-service/v3/api-docs")
        .filters(f -> f.setPath("/v3/api-docs"))
        .uri(foodServiceUrl))
    .build();
```

Không quên `/api/categories/**`. Nếu thiếu, gateway không còn là điểm vào duy nhất cho toàn bộ API bắt buộc.

### Bước G3 — CORS một nguồn duy nhất

```java
@Bean
public CorsWebFilter corsWebFilter() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.addAllowedOriginPattern("*");
    configuration.addAllowedMethod("*");
    configuration.addAllowedHeader("*");
    configuration.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return new CorsWebFilter(source);
}
```

Không đồng thời khai báo một bộ CORS toàn cục khác trong tệp cấu hình, vì hai bộ lọc có thể tạo tiêu đề HTTP lặp. Với
`allowedOriginPattern("*")`, Spring có thể phản chiếu nguồn của yêu cầu trong `Access-Control-Allow-Origin`; điều này
vẫn có nghĩa là chấp nhận mọi máy chủ.

### Bước G4 — Swagger tổng hợp

Khi cả hai dịch vụ đang chạy, mở:

```text
http://localhost:8080/swagger-ui.html
```

Danh sách thả xuống phải có `Restaurant Service` và `Food Service`. Hai JSON trung gian:

```text
http://localhost:8080/restaurant-service/v3/api-docs
http://localhost:8080/food-service/v3/api-docs
```

---

## 10. Luồng yêu cầu cần hiểu

### 10.1. Tạo Restaurant

```text
Client :8080
  → Gateway route /api/restaurants/**
  → RestaurantController :8081
  → RestaurantServiceImpl
  → CategoryRepository.existsById
  → RestaurantRepository.save
  → ApiResponseDTO, HTTP 201
```

### 10.2. Tạo Food

```text
Client :8080
  → Gateway /api/foods/**
  → FoodController :8082
  → FoodServiceImpl
  → RestaurantClient (OpenFeign)
  → RestaurantController :8081
  → nếu Restaurant tồn tại: FoodRepository.save
  → ApiResponseDTO, HTTP 201
```

Vì vậy, thứ tự khởi động bắt buộc khi kiểm thử Food là Restaurant trước, Food sau và Gateway cuối.

---

## 11. Chạy ứng dụng

### 11.1. Xây dựng cả ba dự án

Mở cửa sổ lệnh tại từng dự án:

```powershell
mvn clean test
```

Không chỉ chạy `compile`: pha `test` còn bắt lỗi trùng dữ liệu/trạng thái, cập nhật một phần, ngày tháng, tuyến Category
và CORS.

### 11.2. Khởi động đúng thứ tự

Mở ba terminal:

```powershell
cd SE180211RestaurantService
mvn spring-boot:run
```

```powershell
cd SE180211FoodService
mvn spring-boot:run
```

```powershell
cd SE180211FoodyGateway
mvn spring-boot:run
```

Kiểm tra cổng trên Windows:

```powershell
Get-NetTCPConnection -State Listen | Where-Object LocalPort -in 8080,8081,8082
```

Nếu Restaurant báo lỗi `login failed`:

- kiểm SQL Server đang bật TCP/IP và cổng 1433;
- kiểm chế độ xác thực SQL;
- kiểm tên người dùng `sa` và mật khẩu đúng `sa` theo đề;
- kiểm tra tên cơ sở dữ liệu;
- kiểm không có phiên bản dịch vụ khác chiếm cổng.

Nếu Food báo không tạo được RestaurantClient: kiểm tra `@EnableFeignClients` và thư viện phụ thuộc OpenFeign.

Nếu gateway trả 502: dịch vụ đích chưa chạy hoặc URL trong tệp cấu hình sai.

---

## 12. Kiểm thử mọi điểm cuối bằng yêu cầu/phản hồi mẫu

Tệp [API_TESTS.http](API_TESTS.http) đã có sẵn toàn bộ yêu cầu. Trong IntelliJ, mở tệp rồi bấm biểu tượng tam giác cạnh
từng yêu cầu.

### 12.1. GET danh sách Category

```http
GET http://localhost:8080/api/categories?page=0&size=10
Accept: application/json
```

Phản hồi 200/trạng thái 1:

```json
{
  "status": 1,
  "message": "Successful",
  "data": {
    "size": 10,
    "page": 0,
    "totalPages": 1,
    "totalElements": 3,
    "first": true,
    "last": true,
    "content": [
      {"categoryId": 1, "name": "Vietnamese Food"}
    ]
  }
}
```

### 12.2. POST Restaurant

```http
POST http://localhost:8080/api/restaurants
Content-Type: application/json

{
  "name": "BBQ Hoa Lac",
  "owner": "ABC Company",
  "priceFrom": 2000,
  "priceTo": 3000,
  "phone": "01234567890",
  "address": "Hoa Lac Park",
  "openDate": "2026-03-14T10:00:00Z",
  "status": "ACTIVE",
  "categoryId": 1
}
```

Phản hồi 201/trạng thái 1; `data.restaurantId` được SQL Server sinh.

Các lỗi phải thử:

- bỏ `name` → 400/trạng thái 2;
- name dài hơn 100 → 400/trạng thái 2;
- name trùng không phân biệt hoa/thường → 400/trạng thái 3, thông báo `Name is duplicated`;
- trường `status` khác ACTIVE/INACTIVE → 400/trạng thái 2;
- categoryId không tồn tại → 400/trạng thái 4;
- openDate sai cả ISO và dd/MM/yyyy → 400/trạng thái 2.

### 12.3. PUT cập nhật một phần Restaurant

```http
PUT http://localhost:8080/api/restaurants/1
Content-Type: application/json

{
  "owner": "New Owner Company",
  "priceTo": 3500
}
```

Phản hồi 200/trạng thái 1. `name`, `openDate`, `address` không gửi phải giữ nguyên. ID không tồn tại → 400/trạng thái 4.

### 12.4. GET chi tiết Restaurant

```http
GET http://localhost:8080/api/restaurants/1
```

Trường `data` của phản hồi là RestaurantDTO; ngày đầu ra có dạng ISO UTC. ID không tồn tại → HTTP 400/trạng thái 4.

### 12.5. DELETE Restaurant

```http
DELETE http://localhost:8080/api/restaurants/1
```

Phản hồi:

```json
{"status":1,"message":"Successful","data":null}
```

Sau đó truy vấn SELECT phải cho thấy bản ghi vẫn tồn tại với `status = INACTIVE`.

### 12.6. GET danh sách Restaurant

```http
GET http://localhost:8080/api/restaurants?page=0&size=10&name=BBQ&ownerName=Owner
```

Bộ lọc là khớp một phần và không phân biệt hoa/thường. Thử `page=-1`, `size=0`, `size=101`; tất cả phải trả 400/trạng
thái 2.

### 12.7. POST Food

```http
POST http://localhost:8080/api/foods
Content-Type: application/json

{
  "name": "Pho Bo",
  "price": 50000,
  "ingredients": "Beef, rice noodle, herbs",
  "restaurantId": 1,
  "status": "ACTIVE"
}
```

Phản hồi 201/trạng thái 1, `data` là FoodDTO:

```json
{
  "status": 1,
  "message": "Successful",
  "data": {
    "foodId": 1,
    "name": "Pho Bo",
    "price": 50000,
    "ingredients": "Beef, rice noodle, herbs",
    "status": "ACTIVE",
    "restaurantId": 1
  }
}
```

Restaurant ID không tồn tại → HTTP 400/trạng thái 4. Nếu Restaurant Service tắt → HTTP 500/trạng thái 0.

### 12.8. PUT cập nhật một phần Food

```http
PUT http://localhost:8080/api/foods/1
Content-Type: application/json

{
  "price": 55000,
  "ingredients": "Beef, rice noodle, herbs, onion"
}
```

Phản hồi 200/trạng thái 1. Các trường bị bỏ qua được giữ nguyên. Food ID không tồn tại → 400/trạng thái 4.

### 12.9. GET chi tiết Food

```http
GET http://localhost:8080/api/foods/1
```

`data` phải là FoodResponseDTO có Restaurant lồng:

```json
{
  "status": 1,
  "message": "Successful",
  "data": {
    "foodId": 1,
    "name": "Pho Bo",
    "price": 55000,
    "ingredients": "Beef, rice noodle, herbs, onion",
    "restaurant": {
      "restaurantId": 1,
      "name": "BBQ Hoa Lac",
      "owner": "New Owner Company",
      "priceFrom": 2000,
      "priceTo": 3500,
      "phone": "01234567890",
      "address": "Hoa Lac Park",
      "openDate": "2026-03-14T10:00:00Z",
      "status": "ACTIVE",
      "categoryId": 1
    }
  }
}
```

### 12.10. GET danh sách Food

```http
GET http://localhost:8080/api/foods?page=0&size=10&name=Pho&ingredients=beef
```

`data` là FoodListDTO:

```json
{
  "pageSize": 10,
  "pageNo": 0,
  "totalPages": 1,
  "first": true,
  "last": true,
  "foods": [
    {
      "foodId": 1,
      "name": "Pho Bo",
      "price": 55000,
      "ingredients": "Beef, rice noodle, herbs, onion",
      "restaurant": {"restaurantId": 1, "name": "BBQ Hoa Lac"}
    }
  ]
}
```

### 12.11. DELETE Food

```http
DELETE http://localhost:8080/api/foods/1
```

Phản hồi HTTP 200/trạng thái 1/`data = null`; bản ghi DB còn tồn tại với `status = INACTIVE`.

---

## 13. Các kiểm thử đơn vị/tích hợp đã có

### Restaurant Service — 8 kiểm thử

- tạo mới hợp lệ: bỏ khoảng trắng thừa, chuẩn hóa trạng thái;
- tên trùng → trạng thái 3;
- Category không tồn tại → trạng thái 4;
- cập nhật một phần giữ trường bị bỏ qua;
- size > 100 → trạng thái 2;
- phân tích ngày ISO;
- đọc định dạng ví dụ dd/MM/yyyy;
- tuần tự hóa thành ISO UTC.

### Food Service — 5 kiểm thử

- tạo mới và tra cứu bằng Feign;
- Restaurant không tồn tại → trạng thái 4;
- ánh xạ chi tiết có RestaurantDTO lồng bên trong;
- cập nhật một phần;
- trang âm → trạng thái 2.

### Gateway — 2 kiểm thử

- tồn tại đủ tuyến Restaurant, Category, Food và hai tuyến OpenAPI;
- yêu cầu CORS tiền kiểm chấp nhận nguồn bất kỳ.

Chạy riêng một kiểm thử khi gỡ lỗi:

```powershell
mvn -Dtest=RestaurantServiceImplTest test
mvn -Dtest=FoodServiceImplTest test
mvn -Dtest=GatewayConfigurationTest test
```

Kiểm thử đạt không thay thế việc kiểm thử với SQL Server thật. Kiểm thử đơn vị dùng đối tượng mô phỏng cho
repository/Feign; trước khi nộp vẫn phải chạy API đầu-cuối theo mục 12.

---

## 14. Gỡ lỗi theo từng tầng

### API trả 400/trạng thái 2

Đặt điểm dừng lần lượt:

1. Controller xem JSON được liên kết vào DTO thế nào.
2. `validateCreate` hoặc `validateUpdate`.
3. `normalizeStatus`.
4. GlobalExceptionHandler.

Kiểm tra độ dài tối đa theo SQL, trường bắt buộc và định dạng ngày.

### API trả 400/trạng thái 4 ở Food

1. Gọi thẳng `GET http://localhost:8081/api/restaurants/{restaurantId}`.
2. Nếu lời gọi trực tiếp đúng, kiểm tra `restaurant.service.url`.
3. Đặt điểm dừng tại `FoodServiceImpl.fetchRef`.
4. Kiểm `ApiResponseDTO<RestaurantDTO>.status == 1` và `data != null`.

### SQL báo `invalid column`

So sánh các chú thích:

- `owner` ↔ `owner_name`;
- `openDate` ↔ `open_date`;
- `ingredients` ↔ `ingredient`;
- các ID ↔ snake_case.

### Chưa bắt được dữ liệu trùng

Kiểm tra `restaurants.name` có ràng buộc duy nhất, kho truy cập dữ liệu có `existsByNameIgnoreCase`, và thao tác cập
nhật dùng biến thể `...AndRestaurantIdNot`.

### CORS lỗi trên trình duyệt

Dùng yêu cầu OPTIONS và kiểm tra:

```http
Origin: https://example.test
Access-Control-Request-Method: GET
```

Phản hồi phải là 2xx và có `Access-Control-Allow-Origin`. Không cấu hình CORS hai lần.

---

## 15. Danh sách kiểm tra trước khi nén

### 15.1. Danh sách kiểm tra chức năng

- [ ] SQL Server có đủ ba bảng và đúng cột.
- [ ] GET Category qua cổng 8080 được.
- [ ] POST Restaurant trả 201/trạng thái 1.
- [ ] Restaurant trùng tên trả 400/trạng thái 3.
- [ ] Cập nhật một phần chỉ đổi trường được gửi lên.
- [ ] DELETE chỉ chuyển `status` thành INACTIVE.
- [ ] Tạo Food có kiểm tra Restaurant bằng Feign.
- [ ] Chi tiết/danh sách Food có RestaurantDTO lồng bên trong.
- [ ] page/size không hợp lệ trả trạng thái 2.
- [ ] Mọi ID không tồn tại trả HTTP 400/trạng thái 4.
- [ ] Phản hồi lỗi có `data: null`.
- [ ] Swagger trực tiếp và qua gateway mở được.
- [ ] Yêu cầu CORS tiền kiểm qua gateway thành công.

### 15.2. Danh sách kiểm tra cấu trúc có thể bị chấm 0

- [ ] Project SDK/trình chạy Maven dùng JDK 21.
- [ ] Boot đúng 3.5.11.
- [ ] `packaging=jar`.
- [ ] MSSV và gói đã đổi đúng chữ hoa/thường.
- [ ] Cổng 8081/8082/8080.
- [ ] JDBC URL, cơ sở dữ liệu, tên người dùng/mật khẩu đúng đề.
- [ ] Không có đường dẫn ngữ cảnh tùy chỉnh.
- [ ] Tên bảng/cột đúng SQL.
- [ ] `mvn clean test` đạt ở cả ba dự án.

### 15.3. Nén đúng cách

Đề yêu cầu nén mã nguồn dự án. Mỗi dự án có một tệp ZIP riêng:

```text
SE180211RestaurantService.zip
SE180211FoodService.zip
SE180211FoodyGateway.zip
```

Trước khi nén:

1. chạy `mvn clean` để xóa `target`;
2. đóng ứng dụng để không giữ khóa tệp;
3. không đưa `.git`, nhật ký, tệp kết xuất hoặc bản sao lưu cơ sở dữ liệu nặng vào ZIP;
4. giữ `pom.xml`, `src/main`, và nếu muốn chứng minh kiểm thử thì giữ `src/test`;
5. mở thử ZIP, bảo đảm cấp đầu tiên là thư mục dự án hoặc `pom.xml` đúng hướng dẫn EOS;
6. không nén cả thư mục `02-restaurant-food` thành một tệp nếu EOS yêu cầu từng dự án.

Lệnh PowerShell mẫu, chạy từ thư mục chứa ba dự án:

```powershell
Compress-Archive -Path .\SE180211RestaurantService -DestinationPath .\SE180211RestaurantService.zip
Compress-Archive -Path .\SE180211FoodService -DestinationPath .\SE180211FoodService.zip
Compress-Archive -Path .\SE180211FoodyGateway -DestinationPath .\SE180211FoodyGateway.zip
```

Nếu tệp ZIP đã tồn tại, xóa đúng tệp ZIP cũ trước; không dùng lệnh xóa đệ quy vào thư mục mã nguồn.

---

## 16. Giới hạn có chủ ý và bài tập nâng cao

### 16.1. Phân biệt trường bị bỏ qua với giá trị null tường minh trong PUT một phần

DTO Java thông thường liên kết cả `{}` và `{"priceFrom": null}` thành `priceFrom == null`. Mã nguồn hiện coi cả hai là
“không đổi” để bảo đảm trường bị bỏ qua được giữ nguyên.

Nếu giám khảo yêu cầu null tường minh phải xóa giá trị cho phép null, có thể:

- tạo UpdateRestaurantDTO dùng `JsonNullable<Integer>`; hoặc
- thêm setter tùy chỉnh và cờ `priceFromProvided`; hoặc
- nhận `JsonNode` rồi kiểm `node.has("priceFrom")`.

Không nên tự thêm khi đề không nói rõ vì làm DTO phức tạp và có thể lệch bảng thuộc tính lớp.

### 16.2. N+1 lời gọi Feign khi lấy danh sách Food

Mỗi Food cần RestaurantDTO nên bản dễ học gọi Feign cho từng phần tử. Trong môi trường thực tế có thể lưu đệm theo
`restaurantId` trong phạm vi một yêu cầu hoặc xây điểm cuối xử lý theo lô, nhưng đề không cho điểm cuối như vậy.

### 16.3. Cơ sở dữ liệu không có FK thật

Kiểm tra hợp lệ trong Java giảm khả năng lưu ID rác nhưng không loại bỏ điều kiện tranh chấp giữa các dịch vụ. Chỉ thêm
FK nếu giám khảo xác nhận được phép sửa lược đồ chính thức.

---

## 17. Thứ tự học/gõ đề xuất trong 85 phút

Một kế hoạch luyện tập thực tế:

1. **0–8 phút:** đọc đề, khoanh cổng/dự án/gói/DTO/điểm cuối/trạng thái.
2. **8–15 phút:** chạy SQL, sửa lỗi cú pháp được phát nếu cần, kiểm lược đồ.
3. **15–30 phút:** tạo dự án Restaurant, thực thể/repository/DTO.
4. **30–42 phút:** dịch vụ/controller Restaurant, xử lý lỗi và danh sách Category.
5. **42–55 phút:** thực thể Food/DTO/repository/Feign.
6. **55–66 phút:** dịch vụ/controller Food.
7. **66–73 phút:** các tuyến Gateway và CORS.
8. **73–80 phút:** khởi động 3 ứng dụng, kiểm thử trường hợp thành công và các trạng thái chính.
9. **80–85 phút:** `mvn clean`, rà MSSV/cổng/gói, nén và mở kiểm tra tệp ZIP.

Khi luyện ở nhà, không cần ép đúng 85 phút ngay. Lần đầu hãy tự gõ chậm và đặt điểm dừng. Lần hai không nhìn phần cài
đặt dịch vụ. Lần ba dùng đồng hồ và chỉ xem danh sách kiểm tra.

---

## 18. Nguồn thông tin chuẩn trong thư mục đáp án

- Mã nguồn hoàn chỉnh: ba thư mục dự án cạnh tài liệu này.
- Yêu cầu chạy trực tiếp trong IntelliJ: [API_TESTS.http](API_TESTS.http).
- Lược đồ đã sửa cú pháp an toàn: [database/MSS301_2026_PE.sql](database/MSS301_2026_PE.sql).
- Dữ liệu kiểm thử tùy chọn: [database/SEED_CATEGORIES_OPTIONAL.sql](database/SEED_CATEGORIES_OPTIONAL.sql).
- Báo cáo xác minh cuối: [VERIFICATION.md](VERIFICATION.md).
- Ghi chú rà soát tự động ban đầu: [MANUAL_REVIEW.md](MANUAL_REVIEW.md).

Hãy coi mã nguồn là đáp án tham chiếu, còn tài liệu này là lộ trình để bạn có thể tự tạo lại đáp án mà hiểu từng quyết
định.
