# Hướng dẫn tự học và gõ lại bài Department – Employee – Gateway

Tài liệu này đi cùng ba dự án đáp án:

- `SE180211DepartmentService`
- `SE180211EmployeeService`
- `SE180211EmployeeGateway`

Mục tiêu của tài liệu không chỉ là chỉ cách chạy bài có sẵn. Thứ tự dưới đây được thiết kế để bạn có thể mở IntelliJ,
tạo dự án trống và **tự gõ lại toàn bộ bài theo một luồng hợp lý trong phòng thi**.

> `SE180211` chỉ là mã sinh viên mẫu. Đọc mục **Đổi MSSV** ở cuối trước khi nộp.

---

## 1. Chốt yêu cầu trước khi viết mã nguồn

### 1.1 Kiến trúc phải tạo

```text
Client
  |
  | http://localhost:8080
  v
Employee Gateway (8080)
  |-- /api/departments/** --> Department Service (8081) --> SQL Server
  `-- /api/employees/**   --> Employee Service   (8082) --> SQL Server
                                      |
                                      `-- OpenFeign --> Department Service
```

Ba dự án độc lập, đều đóng gói `jar`, đều dùng JDK 21:

| Dự án                          | Cổng | Công nghệ chính                        | Cơ sở dữ liệu     |
|--------------------------------|-----:|----------------------------------------|-------------------|
| `<StudentID>DepartmentService` | 8081 | Spring MVC, Spring Data JPA            | `MSS301_2026_PE`  |
| `<StudentID>EmployeeService`   | 8082 | Spring MVC, Spring Data JPA, OpenFeign | `MSS301_2026_PE`  |
| `<StudentID>EmployeeGateway`   | 8080 | Spring Cloud Gateway WebFlux, Security | Không truy cập DB |

### 1.2 Bảng trạng thái nghiệp vụ phải nhớ

| `status` trong phản hồi | Ý nghĩa                                        | HTTP thường dùng |
|------------------------:|------------------------------------------------|-----------------:|
|                       0 | Lỗi hệ thống                                   |              500 |
|                       1 | Thành công                                     |     200 hoặc 201 |
|                       2 | Dữ liệu không hợp lệ                           |              400 |
|                       3 | Trùng mã Department                            |              400 |
|                       4 | Không tìm thấy đối tượng/Department tham chiếu |              400 |

### 1.3 Cách xử lý các điểm mâu thuẫn trong đề

Đề Word và SQL có vài điểm không thể đồng thời hiểu theo nghĩa đen. Đáp án này dùng thứ tự ưu tiên: yêu cầu “0 điểm nếu
sai” → file SQL chính thức → bảng DTO/API → JSON minh họa.

1. Bảng `ApiResponseDTO` ghi `status`, `data`, `timestamp`, trong khi một JSON minh họa có `message`. Mã nguồn chọn *
   *timestamp**, vì bảng DTO là định nghĩa chính thức. Không thêm `message` để tránh sai cấu trúc.
2. Bảng cơ sở dữ liệu trong Word đánh dấu `parent_id` là bắt buộc, nhưng mô tả DTO nói cấp công ty được để null và SQL
   chính thức khai báo `NULL`. Mã nguồn chọn **cho phép null**.
3. Word yêu cầu `effectiveDate` sau `2000-01-01` và trước hôm nay + 360 ngày; SQL chính thức yêu cầu từ hôm nay trở đi.
   Mã nguồn lấy **giao của hai điều kiện**: `today <= date < today + 360 days`. Nhờ vậy yêu cầu hợp lệ ở Java chắc chắn
   không bị SQL Server từ chối.
4. R03 nói `page` bắt đầu từ 0 và cấu hình mặc định là 0, dù JSON minh họa dùng page 1. Mã nguồn chọn **bắt đầu từ 0**.
5. Dòng lỗi của API chi tiết Employee ghi “Department is not found”, nhưng endpoint và tham số đường dẫn đều là
   Employee. Mã nguồn trả status 4 với nghĩa **Employee is not found**.
6. Hai tệp SQL không khai báo khóa ngoại. Đáp án không tự thêm FK vào DDL. Employee Service vẫn gọi Department Service
   để xác nhận `departmentId` tồn tại trước khi lưu.
7. SQL Department dùng `DATE`, còn JSON mẫu dùng `20/05/2025`. DTO dùng `dd/MM/yyyy`. Trường `timestamp` của phản hồi
   vẫn là ISO-8601 UTC, ví dụ `2026-03-14T10:00:00Z`.

---

## 2. Chuẩn bị máy

Trước ngày thi nên chuẩn bị:

- IntelliJ IDEA có JDK 21.
- Các thư viện Maven đã được tải vào `.m2` để có thể biên dịch ngoại tuyến.
- SQL Server 2016 trở lên chạy ở `localhost:1433`.
- Login SQL Server: username `sa`, password `sa`.
- Maven dùng đúng JDK 21: kiểm tra bằng `mvn -version`.

Kết quả đúng phải có dòng gần giống:

```text
Java version: 21
```

Nếu IntelliJ báo lỗi Lombok, vào:

```text
Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors
```

và bật xử lý annotation, mục **Enable annotation processing** trong giao diện.

---

## 3. Tạo cơ sở dữ liệu

### 3.1 Tạo cơ sở dữ liệu trước

Trong SQL Server Management Studio:

```sql
IF DB_ID(N'MSS301_2026_PE') IS NULL
BEGIN
    CREATE DATABASE MSS301_2026_PE;
END
GO
```

### 3.2 Chạy SQL theo đúng thứ tự

Trong thư mục `database` có ba lựa chọn:

- `01_create_departments_official.sql`: bản rõ ràng theo SQL Department chính thức được cấp.
- `02_create_employees_official.sql`: bản rõ ràng theo SQL Employee chính thức được cấp.
- `MSS301_2026_PE.sql`: bản gộp, từ chối ghi đè nếu bảng đã tồn tại.

Khi làm thi, ưu tiên chạy hai tệp chính thức theo thứ tự `01` rồi `02`.

Kiểm tra nhanh:

```sql
USE MSS301_2026_PE;
SELECT TOP 10 * FROM dbo.departments;
SELECT TOP 10 * FROM dbo.employees;
```

Không tự đổi các tên `departments`, `employees`, `department_id`, `employee_id` hoặc bất kỳ tên cột nào; đề cảnh báo sai
tên bảng/cột có thể bị chấm 0.

---

## 4. Tạo Department Service từ Spring Initializr

### 4.1 Chọn thông số

Trong IntelliJ chọn **File → New → Project → Spring Boot/Spring Initializr**:

| Mục                         | Giá trị                     |
|-----------------------------|-----------------------------|
| Kiểu dự án (`Type`)         | Maven                       |
| Ngôn ngữ (`Language`)       | Java                        |
| Spring Boot                 | 3.5.11                      |
| Nhóm (`Group`)              | `fu.se180211`               |
| Tên (`Artifact/Name`)       | `SE180211DepartmentService` |
| Gói (`Package`)             | `fu.se180211.department`    |
| Kiểu đóng gói (`Packaging`) | Jar                         |
| Java                        | 21                          |

Chọn các thư viện:

- Spring Web
- Spring Data JPA
- Validation
- MS SQL Server Driver
- Lombok
- Spring Boot DevTools

Springdoc không có sẵn trong mọi giao diện Initializr, nên thêm thủ công vào `pom.xml`:

```xml
<properties>
    <java.version>21</java.version>
    <springdoc.version>2.8.14</springdoc.version>
</properties>

<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

Đảm bảo parent là:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.11</version>
    <relativePath/>
</parent>
```

### 4.2 Tạo đúng cây package

```text
fu.se180211.department
|-- SE180211DepartmentServiceApplication.java
|-- common
|   |-- BusinessException.java
|   |-- GlobalExceptionHandler.java
|   `-- ResponseStatuses.java
|-- config
|   |-- CorsConfig.java
|   `-- OpenApiConfig.java
|-- controller
|   `-- SE180211DepartmentController.java
|-- dto
|   |-- ApiResponseDTO.java
|   |-- DepartmentDTO.java
|   `-- PageDTO.java
|-- entity
|   `-- Department.java
|-- repository
|   `-- DepartmentRepository.java
|-- service
|   |-- DepartmentService.java
|   `-- impl
|       `-- DepartmentServiceImpl.java
```

Hãy tạo package trước, rồi gõ lớp theo thứ tự ở các mục tiếp theo. Thứ tự này giúp mỗi lớp mới chỉ phụ thuộc vào lớp đã
có.

### 4.3 Gõ lớp entity `Department`

Ánh xạ phải khớp SQL:

| Java                 | SQL                                 | Quy tắc                     |
|----------------------|-------------------------------------|-----------------------------|
| `Long departmentId`  | `department_id INT IDENTITY`        | `@Id`, `IDENTITY`           |
| `String name`        | `name NVARCHAR(50) NOT NULL`        | `nullable=false`, độ dài 50 |
| `String code`        | `code NVARCHAR(10) NOT NULL UNIQUE` | duy nhất, độ dài 10         |
| `Date effectiveDate` | `effective_date DATE NULL`          | `@Temporal(DATE)`           |
| `String status`      | `status NVARCHAR(10) NULL`          | độ dài 10                   |
| `String location`    | `location NVARCHAR(100) NULL`       | độ dài 100                  |
| `Long parentId`      | `parent_id INT NULL`                | cho phép null               |

Khung quan trọng:

```java
@Getter
@Setter
@Entity
@Table(name = "departments")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Temporal(TemporalType.DATE)
    @Column(name = "effective_date")
    private Date effectiveDate;
}
```

Gõ tiếp `location`, `status`, `parentId` theo tệp đáp án. Không dùng `ddl-auto=create`; bảng được tạo bằng SQL chính
thức.

### 4.4 Gõ ba DTO

`DepartmentDTO` là đối tượng yêu cầu/phản hồi. Không trả Entity trực tiếp vì DTO là yêu cầu của đề.

```java
@Getter
@Setter
public class DepartmentDTO {
    private Long departmentId;

    @Size(max = 10)
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    private String code;

    @Size(max = 50)
    private String name;

    @Size(max = 100)
    private String location;

    @Size(max = 10)
    private String status;

    @JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC")
    private Date effectiveDate;

    private Long parentId;
}
```

Không đặt `@NotBlank` thẳng lên `code`/`name`: POST yêu cầu hai trường này, nhưng PUT là cập nhật một phần và cho phép
bỏ chúng. Kiểm tra trường bắt buộc được thực hiện trong `validateCreate`; PUT chỉ kiểm tra trường thực sự có giá trị.

`ApiResponseDTO<T>` phải có đúng ba thuộc tính:

```java
private int status;
private T data;
private String timestamp;
```

Hàm khởi tạo luôn tạo timestamp UTC:

```java
this.timestamp = Instant.now()
        .truncatedTo(ChronoUnit.SECONDS)
        .toString();
```

Hai phương thức tạo giúp lớp điều khiển ngắn và đồng nhất:

```java
public static <T> ApiResponseDTO<T> success(T data) {
    return new ApiResponseDTO<>(1, data);
}

public static <T> ApiResponseDTO<T> of(int status, T data) {
    return new ApiResponseDTO<>(status, data);
}
```

`PageDTO<T>` lấy dữ liệu từ `Page<T>`:

```java
public PageDTO(Page<T> p) {
    size = p.getSize();
    page = p.getNumber();
    totalPages = p.getTotalPages();
    totalElements = p.getTotalElements();
    first = p.isFirst();
    last = p.isLast();
    content = p.getContent();
}
```

### 4.5 Gõ kho dữ liệu (repository)

```java
public interface DepartmentRepository
        extends JpaRepository<Department, Long>,
                JpaSpecificationExecutor<Department> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndDepartmentIdNot(String code, Long departmentId);
}
```

`JpaSpecificationExecutor` dùng để ghép điều kiện lọc `name` và `status` động. Hai phương thức `exists...` xử lý mã
trùng khi tạo và cập nhật.

### 4.6 Gõ các lớp xử lý ngoại lệ dùng chung

`ResponseStatuses` chỉ là các hằng số `0..4`. `BusinessException` giữ đồng thời status API và status HTTP:

```java
public class BusinessException extends RuntimeException {
    private final int apiStatus;
    private final HttpStatus httpStatus;

    public BusinessException(int apiStatus, HttpStatus httpStatus, String message) {
        super(message);
        this.apiStatus = apiStatus;
        this.httpStatus = httpStatus;
    }
}
```

`GlobalExceptionHandler` chia lỗi thành ba nhóm:

1. `BusinessException`: dùng status HTTP/API đã chọn.
2. JSON sai, kiểu dữ liệu sai hoặc Bean Validation không đạt: HTTP 400, status 2.
3. Lỗi không dự kiến: HTTP 500, status 0.

Mẫu phương thức xử lý quan trọng:

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ApiResponseDTO<Void>> handleBusiness(BusinessException ex) {
    return ResponseEntity.status(ex.getHttpStatus())
            .body(ApiResponseDTO.of(ex.getApiStatus(), null));
}
```

### 4.7 Gõ interface của service

```java
public interface DepartmentService {
    DepartmentDTO create(DepartmentDTO dto);
    DepartmentDTO update(Long id, DepartmentDTO dto);
    DepartmentDTO get(Long id);
    void deactivate(Long id);
    PageDTO<DepartmentDTO> list(Integer page, Integer size, String name, String status);
}
```

### 4.8 Gõ `DepartmentServiceImpl` theo từng nghiệp vụ

Khai báo trước:

```java
private static final int MAX_PAGE_SIZE = 100;
private static final Set<String> STATUS_VALUES =
        Set.of("ACTIVE", "INACTIVE", "CLOSED");
private static final Pattern CODE_PATTERN =
        Pattern.compile("^[A-Za-z0-9]+$");
```

#### Tạo mới

Thứ tự đúng:

1. DTO không null.
2. `code` bắt buộc, tối đa 10 ký tự, chỉ gồm chữ và số.
3. `name` bắt buộc, tối đa 50.
4. `location` tối đa 100 nếu có.
5. Kiểm tra ngày và `parentId` nếu có.
6. Kiểm tra mã trùng không phân biệt hoa thường.
7. Xóa khoảng trắng thừa ở chuỗi, gán status luôn là `ACTIVE`.
8. Lưu rồi chuyển Entity thành DTO.

Điểm dễ sai: không dùng status gửi lên khi tạo; đề nói status mặc định là ACTIVE.

#### Cập nhật một phần

Đầu tiên tìm thực thể; không có thì trả status 4. Sau đó chỉ cập nhật trường khác null:

```java
if (dto.getCode() != null) entity.setCode(clean(dto.getCode()));
if (dto.getName() != null) entity.setName(clean(dto.getName()));
if (dto.getLocation() != null) entity.setLocation(cleanNullable(dto.getLocation()));
if (dto.getStatus() != null) entity.setStatus(normalizeStatus(dto.getStatus()));
if (dto.getEffectiveDate() != null) entity.setEffectiveDate(dto.getEffectiveDate());
if (dto.getParentId() != null) entity.setParentId(dto.getParentId());
```

Khi mã thay đổi, dùng `existsByCodeIgnoreCaseAndDepartmentIdNot` để chính bản ghi hiện tại không bị coi là trùng.

#### Xem chi tiết và vô hiệu hóa

```java
public DepartmentDTO get(Long id) {
    return toDTO(findOrThrow(id));
}

public void deactivate(Long id) {
    Department entity = findOrThrow(id);
    entity.setStatus("INACTIVE");
    repository.save(entity);
}
```

DELETE là xóa mềm, tuyệt đối không gọi `repository.delete`.

#### Lấy danh sách và lọc động

Chuẩn hóa phân trang:

```java
int p = page == null ? 0 : page;
int sz = size == null ? 10 : size;
if (p < 0 || sz < 1 || sz > 100) throw validationFailed();
```

Dùng `Specification` để chỉ thêm điều kiện khi tham số truy vấn có giá trị, sau đó sắp xếp tăng dần theo `departmentId`.

#### Kiểm tra ngày sau khi xử lý mâu thuẫn

```java
LocalDate date = toLocal(value);
LocalDate today = LocalDate.now();
if (!date.isAfter(LocalDate.of(2000, 1, 1))
        || date.isBefore(today)
        || !date.isBefore(today.plusDays(360))) {
    throw validationFailed();
}
```

### 4.9 Gõ lớp điều khiển đúng tên và URI

Tên lớp bắt buộc:

```java
@RestController
@RequestMapping(value = "/api/departments", produces = MediaType.APPLICATION_JSON_VALUE)
public class SE180211DepartmentController { ... }
```

Ánh xạ:

| Nghiệp vụ     | Annotation                          | Kết quả thành công        |
|---------------|-------------------------------------|---------------------------|
| Tạo mới       | `@PostMapping`                      | HTTP 201, status 1        |
| Cập nhật      | `@PutMapping("/{departmentId}")`    | HTTP 200, status 1        |
| Xem chi tiết  | `@GetMapping("/{departmentId}")`    | HTTP 200, status 1        |
| Vô hiệu hóa   | `@DeleteMapping("/{departmentId}")` | HTTP 200, data null       |
| Lấy danh sách | `@GetMapping`                       | HTTP 200, data là PageDTO |

POST/PUT phải có `consumes = application/json`, tham số phần thân có `@Valid @RequestBody`.

### 4.10 Gõ cấu hình

`application.properties`:

```properties
spring.application.name=SE180211DepartmentService
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
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

`OpenApiConfig` tạo bean `OpenAPI` với tiêu đề dự án. `CorsConfig` cho phép GET/POST/PUT/DELETE/OPTIONS; CORS bắt buộc
chính ở Gateway nhưng việc cho phép dịch vụ mở Swagger trực tiếp cũng hữu ích.

---

## 5. Tạo Employee Service

### 5.1 Spring Initializr

| Mục                         | Giá trị                   |
|-----------------------------|---------------------------|
| Nhóm (`Group`)              | `fu.se180211`             |
| Tên (`Artifact/Name`)       | `SE180211EmployeeService` |
| Gói (`Package`)             | `fu.se180211.employee`    |
| Boot                        | 3.5.11                    |
| Kiểu đóng gói (`Packaging`) | Jar                       |
| Java                        | 21                        |

Các thư viện giống Department Service và thêm **OpenFeign**. Trong `pom.xml`, nhập Spring Cloud BOM:

```xml
<properties>
    <java.version>21</java.version>
    <spring-cloud.version>2025.0.2</spring-cloud.version>
    <springdoc.version>2.8.14</springdoc.version>
</properties>

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

Thư viện Feign:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

### 5.2 Cây package

```text
fu.se180211.employee
|-- SE180211EmployeeServiceApplication.java
|-- common
|-- config
|   |-- CorsConfig.java
|   |-- DepartmentClient.java
|   `-- OpenApiConfig.java
|-- controller
|   `-- SE180211EmployeeController.java
|-- dto
|   |-- ApiResponseDTO.java
|   |-- DepartmentDTO.java
|   |-- EmployeeDTO.java
|   `-- PageDTO.java
|-- entity
|   `-- Employee.java
|-- repository
|   `-- EmployeeRepository.java
|-- service
|   |-- EmployeeService.java
|   `-- impl
|       `-- EmployeeServiceImpl.java
```

`ApiResponseDTO`, `PageDTO`, các ngoại lệ dùng chung và cấu hình Swagger có thể gõ theo cùng mẫu Department nhưng phải
đổi package sang `fu.se180211.employee...`.

### 5.3 Gõ thực thể Employee

Ánh xạ:

| Java           | SQL                        | Quy tắc                                   |
|----------------|----------------------------|-------------------------------------------|
| `employeeId`   | `employee_id INT IDENTITY` | PK                                        |
| `fullName`     | `full_name NVARCHAR(100)`  | bắt buộc                                  |
| `email`        | `email NVARCHAR(100)`      | bắt buộc                                  |
| `position`     | `position NVARCHAR(30)`    | bắt buộc; Manager/Developer/Staff theo đề |
| `status`       | `status NVARCHAR(10)`      | bắt buộc; 4 giá trị                       |
| `startDate`    | `start_date DATE`          | bắt buộc                                  |
| `endDate`      | `end_date DATE NULL`       | nếu có phải >= startDate                  |
| `departmentId` | `department_id INT`        | bắt buộc                                  |

Không ánh xạ `@ManyToOne Department`, vì Department nằm ở microservice khác. Thực thể chỉ giữ `Long departmentId`; phản
hồi mới chứa `DepartmentDTO` lấy qua Feign.

### 5.4 Gõ Employee DTO

```java
@Getter
@Setter
public class EmployeeDTO {
    private Long employeeId;
    @Size(max = 100) private String fullName;
    @Size(max = 100) @Email private String email;
    @Size(max = 30) private String position;
    @Size(max = 10) private String status;
    @JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC")
    private Date startDate;
    @JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC")
    private Date endDate;
    private DepartmentDTO department;
}
```

Yêu cầu tạo mới sử dụng đúng cấu trúc của đề: Department lồng nhau, không tự thêm trường `departmentId` ở cấp cao nhất:

```json
{
  "fullName": "Nguyen Van An",
  "email": "an@example.com",
  "position": "Developer",
  "startDate": "10/03/2026",
  "department": {
    "departmentId": 1
  }
}
```

### 5.5 Bật Feign và tạo trình khách

Trong lớp chính:

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "fu.se180211.employee.config")
public class SE180211EmployeeServiceApplication { ... }
```

Trình khách Feign:

```java
@FeignClient(name = "department-service",
        url = "${department.service.url:http://localhost:8081}")
public interface DepartmentClient {
    @GetMapping("/api/departments/{departmentId}")
    ApiResponseDTO<Object> getDepartment(@PathVariable("departmentId") Long departmentId);
}
```

Trong tệp properties:

```properties
department.service.url=http://localhost:8081
```

### 5.6 Gõ logic Employee Service

Các quy tắc:

- status: `ACTIVE`, `LEFT`, `RETIRED`, `INACTIVE`.
- position: `Manager`, `Developer`, `Staff` (cho phép dữ liệu vào khác hoa/thường, lưu đúng giá trị chuẩn).
- thao tác tạo mới luôn đặt status `ACTIVE`.
- `endDate == null` hoặc `endDate >= startDate`.
- `department.departmentId` lồng nhau là bắt buộc khi tạo mới và phải > 0.
- gọi Department API; chỉ status 1 và data khác null mới được lưu.

Luồng tạo mới:

```text
validateCreate(dto)
  -> lấy dto.department.departmentId
  -> gọi DepartmentClient.getDepartment(id)
  -> tạo Employee entity
  -> set ACTIVE
  -> lưu
  -> trả EmployeeDTO có DepartmentDTO đầy đủ
```

Luồng cập nhật:

1. Tìm Employee trước; không có thì trả status 4.
2. Kiểm tra các trường được cung cấp.
3. Nếu có đối tượng `department`, bắt buộc đối tượng đó có ID và gọi Feign kiểm tra.
4. Ghép ngày bắt đầu/kết thúc mới với giá trị cũ trước khi kiểm tra `end >= start`.
5. Lưu rồi lấy Department để phản hồi luôn có đối tượng lồng nhau.

Luồng lấy danh sách dùng `JpaSpecificationExecutor`, lọc tham số `name` vào trường entity `fullName`, lọc status chính
xác, page mặc định 0, size 10, tối đa 100. Sau khi lấy trang entity, ánh xạ từng bản ghi sang DTO và lấy Department
tương ứng.

Xử lý Feign:

- Department trả HTTP 400/404 hoặc status trong phản hồi khác 1 → API status 4, HTTP 400.
- Department Service không hoạt động hoặc trả lỗi 5xx → API status 0, HTTP 500.

### 5.7 Controller và tệp properties

Lớp điều khiển bắt buộc tên `SE180211EmployeeController`, đường dẫn gốc `/api/employees`, năm endpoint giống Department
với tham số đường dẫn `{employeeId}`.

Tệp properties giống Department nhưng:

```properties
spring.application.name=SE180211EmployeeService
server.port=8082
department.service.url=http://localhost:8081
```

---

## 6. Tạo Employee Gateway

### 6.1 Thông số dự án

| Mục                   | Giá trị                   |
|-----------------------|---------------------------|
| Nhóm (`Group`)        | `fu.se180211`             |
| Tên (`Artifact/Name`) | `SE180211EmployeeGateway` |
| Gói (`Package`)       | `fu.se180211.gateway`     |
| Boot                  | 3.5.11                    |
| Java                  | 21                        |

Các thư viện:

- Spring Cloud Gateway Server WebFlux
- Spring Security
- Springdoc WebFlux UI
- Spring Boot Starter Test

Không thêm Spring MVC hoặc JPA vào Gateway. POM cần Spring Cloud BOM `2025.0.2`, tương tự Employee Service.

### 6.2 Cấu hình Security và CORS

Security có trong danh sách thư viện nhưng đề không yêu cầu đăng nhập, vì vậy cho phép mọi yêu cầu và tắt CSRF, HTTP
Basic và biểu mẫu đăng nhập:

```java
return http
    .csrf(ServerHttpSecurity.CsrfSpec::disable)
    .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
    .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
    .authorizeExchange(ex -> ex.anyExchange().permitAll())
    .build();
```

CORS:

```java
CorsConfiguration c = new CorsConfiguration();
c.addAllowedOriginPattern("*");
c.addAllowedMethod("*");
c.addAllowedHeader("*");
c.setAllowCredentials(false);
```

Không dùng `allowCredentials=true` cùng nguồn đại diện bằng ký tự `*`.

### 6.3 Định tuyến

Trong `GatewayRouteConfig`:

```java
.route("department", r -> r
    .path("/api/departments/**")
    .uri("http://localhost:8081"))
.route("employee", r -> r
    .path("/api/employees/**")
    .uri("http://localhost:8082"))
```

Đáp án còn chuyển tiếp hai tài liệu OpenAPI JSON và cấu hình Swagger UI tại cổng 8080 để chọn Department/Employee trong
danh sách thả xuống:

```properties
springdoc.swagger-ui.urls[0].name=Department Service
springdoc.swagger-ui.urls[0].url=/department-service/v3/api-docs
springdoc.swagger-ui.urls[1].name=Employee Service
springdoc.swagger-ui.urls[1].url=/employee-service/v3/api-docs
```

### 6.4 Cấu trúc package của Gateway

Đề liệt kê đủ `entity`, `repository`, `service`, `service.impl`, `controller`, `dto`, `config`, `common`. Gateway không
có cơ sở dữ liệu hay nghiệp vụ CRUD, nên các package không dùng chứa `package-info.java`; không tạo entity/repository
giả. Cách này vừa giữ cây package được yêu cầu, vừa giữ kiến trúc đúng.

---

## 7. Kiểm thử API bằng Swagger/Postman

### 7.1 Thứ tự chạy

1. Khởi động SQL Server.
2. Khởi động Department Service ở cổng 8081.
3. Khởi động Employee Service ở cổng 8082.
4. Khởi động Gateway ở cổng 8080.

Swagger:

- `http://localhost:8081/swagger-ui.html`
- `http://localhost:8082/swagger-ui.html`
- `http://localhost:8080/swagger-ui.html`

Các yêu cầu công khai khi chấm nên gọi qua Gateway ở cổng 8080.

### 7.2 Tạo Department

```http
POST http://localhost:8080/api/departments
Content-Type: application/json

{
  "code": "IT01",
  "name": "Information Technology",
  "location": "Hoa Lac",
  "effectiveDate": "21/07/2026"
}
```

Hãy thay `effectiveDate` bằng ngày từ hôm nay đến trước hôm nay + 360 ngày. Phản hồi:

```json
{
  "status": 1,
  "data": {
    "departmentId": 1,
    "code": "IT01",
    "name": "Information Technology",
    "location": "Hoa Lac",
    "status": "ACTIVE",
    "effectiveDate": "21/07/2026",
    "parentId": null
  },
  "timestamp": "2026-07-20T15:00:00Z"
}
```

Thử lại cùng mã phải nhận HTTP 400/status 3. Bỏ `name`, dùng mã có ký tự `-`, hoặc dùng ngày quá hạn thì phải nhận HTTP
400/status 2.

### 7.3 Cập nhật một phần Department

```http
PUT http://localhost:8080/api/departments/1
Content-Type: application/json

{
  "name": "IT Department",
  "status": "CLOSED"
}
```

Các trường không gửi sẽ được giữ nguyên. Thành công trả HTTP 200/status 1 và data là Department đã cập nhật.

### 7.4 Xem chi tiết Department

```http
GET http://localhost:8080/api/departments/1
```

ID không tồn tại:

```json
{
  "status": 4,
  "data": null,
  "timestamp": "2026-07-20T15:00:00Z"
}
```

Mã HTTP theo đề là 400.

### 7.5 Vô hiệu hóa Department

```http
DELETE http://localhost:8080/api/departments/1
```

Không xóa bản ghi; kiểm tra cơ sở dữ liệu sẽ thấy status đổi thành `INACTIVE`. Data của phản hồi là null, status 1.

### 7.6 Lấy danh sách Department

```http
GET http://localhost:8080/api/departments?page=0&size=10&name=tech&status=ACTIVE
```

Dữ liệu trong phản hồi:

```json
{
  "size": 10,
  "page": 0,
  "totalPages": 1,
  "totalElements": 1,
  "first": true,
  "last": true,
  "content": [
    {
      "departmentId": 1,
      "code": "IT01",
      "name": "Information Technology",
      "status": "ACTIVE"
    }
  ]
}
```

Kiểm thử `page=-1`, `size=0`, `size=101` và status lạ; tất cả phải trả status 2.

### 7.7 Tạo Employee

Department ID 1 phải tồn tại trước:

```http
POST http://localhost:8080/api/employees
Content-Type: application/json

{
  "fullName": "Nguyen Van An",
  "email": "an@example.com",
  "position": "Developer",
  "startDate": "10/03/2026",
  "endDate": null,
  "department": {
    "departmentId": 1
  }
}
```

Phản hồi có status 1, HTTP 201; `status` là ACTIVE và `department` chứa đối tượng lấy từ Department Service. Department
ID không tồn tại phải trả HTTP 400/status 4.

### 7.8 Cập nhật một phần Employee

```http
PUT http://localhost:8080/api/employees/1
Content-Type: application/json

{
  "position": "Manager",
  "endDate": "20/03/2026"
}
```

Nếu đổi department:

```json
{
  "department": {
    "departmentId": 2
  }
}
```

Đối tượng Department có mặt nhưng thiếu ID phải trả status 2. Employee ID không tồn tại phải trả status 4.

### 7.9 Xem chi tiết Employee

```http
GET http://localhost:8080/api/employees/1
```

Kiểm tra phản hồi có `department` lồng nhau, không chỉ có `departmentId`.

### 7.10 Vô hiệu hóa Employee

```http
DELETE http://localhost:8080/api/employees/1
```

Bản ghi vẫn còn, status đổi thành INACTIVE. ID không tồn tại trả status 4.

### 7.11 Lấy danh sách Employee

```http
GET http://localhost:8080/api/employees?page=0&size=10&name=nguyen&status=ACTIVE
```

Mỗi phần tử trong content phải là `EmployeeDTO` với DepartmentDTO đầy đủ.

---

## 8. Kiểm thử đơn vị và điều kiện biên dịch bắt buộc

Hai dịch vụ có kiểm thử ở `src/test/java/.../service/impl`.

Các kiểm thử Department xác nhận:

- tạo mới thành công và mặc định ACTIVE;
- mã trùng → status 3;
- ngày hiệu lực trước hôm nay hoặc từ ngày thứ 360 trở đi → status 2;
- ID không tồn tại → status 4/HTTP 400.

Các kiểm thử Employee xác nhận:

- tạo mới có gọi Department và mặc định ACTIVE;
- Department không tồn tại → status 4;
- endDate trước startDate → status 2;
- Department lồng nhau thiếu ID → status 2;
- Employee không tồn tại → status 4.

Chạy trong từng dự án:

```powershell
mvn clean test
mvn -DskipTests package
```

Gateway không có dịch vụ nghiệp vụ để kiểm thử đơn vị; Maven vẫn kiểm tra toàn bộ mã nguồn có biên dịch được hay không.

Không coi nút Chạy (`Run`) màu xanh là đủ. Trước khi nén, cả ba lệnh sau phải trả mã thoát 0:

```powershell
mvn -f .\SE180211DepartmentService\pom.xml clean test
mvn -f .\SE180211EmployeeService\pom.xml clean test
mvn -f .\SE180211EmployeeGateway\pom.xml clean test
```

---

## 9. Lỗi thường gặp và cách gỡ lỗi

| Hiện tượng                        | Nguyên nhân thường gặp                       | Cách sửa                                                    |
|-----------------------------------|----------------------------------------------|-------------------------------------------------------------|
| `Unsupported class file`          | Maven đang dùng Java 8/17                    | Chạy `mvn -version`, đặt JDK 21                             |
| Getter Lombok bị báo đỏ           | Chưa bật xử lý annotation                    | Bật xử lý annotation và tải lại Maven                       |
| Đăng nhập SQL thất bại            | Chưa bật xác thực SQL hoặc tài khoản `sa`    | Bật chế độ hỗn hợp, kiểm tra `sa/sa` và cổng 1433           |
| `Invalid object name departments` | Chưa chạy SQL hoặc sai cơ sở dữ liệu         | Chạy tệp 01/02 trong `MSS301_2026_PE`                       |
| Hibernate sửa bảng                | `ddl-auto` sai                               | Bắt buộc `none`                                             |
| Tạo Employee trả 500              | Department Service chưa chạy                 | Chạy 8081 trước 8082                                        |
| Employee trả status 4             | Department ID không tồn tại                  | Tạo Department trước                                        |
| Lỗi phân tích ngày                | Gửi `yyyy-MM-dd`                             | DTO yêu cầu `dd/MM/yyyy`                                    |
| Lỗi SQL khi chèn ngày hiệu lực    | Ngày trước hôm nay                           | Dùng ngày hợp lệ trong cửa sổ 360 ngày                      |
| Gateway trả 404                   | Dịch vụ chưa chạy hoặc sai đường dẫn         | Dùng `/api/departments`/`/api/employees`, kiểm tra các cổng |
| 401/403 ở gateway                 | Security chưa `permitAll` hoặc CSRF chưa tắt | So sánh `SecurityConfig`                                    |
| Swagger Gateway trống             | Chưa chạy các dịch vụ                        | Chạy 8081 và 8082, chọn URL trong danh sách thả xuống       |
| `size=101` trả 400                | Đây là hành vi đúng                          | Giá trị tối đa theo đề là 100                               |

Khi gỡ lỗi ngoại lệ, tạm đặt điểm dừng trong `GlobalExceptionHandler.handleOther`. Không đưa dấu vết ngăn xếp hoặc thông
báo nội bộ vào phản hồi cuối vì đề chỉ định DTO status/data/timestamp.

---

## 10. Đổi MSSV từ SE180211 sang mã của bạn

Ví dụ MSSV thật là `SE999999`:

1. Đổi tên thư mục:
    - `SE180211DepartmentService` → `SE999999DepartmentService`
    - `SE180211EmployeeService` → `SE999999EmployeeService`
    - `SE180211EmployeeGateway` → `SE999999EmployeeGateway`
2. Trong mỗi `pom.xml`, đổi `fu.se180211` thành `fu.se999999` và đổi tên artifact/name.
3. Đổi cây thư mục Java `fu/se180211/...` thành `fu/se999999/...`.
4. Thay thế trong toàn bộ dự án:
    - `fu.se180211` → `fu.se999999`
    - `SE180211` → `SE999999`
    - `se180211` → `se999999`
5. Đổi tên ba lớp main/controller và tên tệp tương ứng:
    - `SE999999DepartmentServiceApplication`
    - `SE999999DepartmentController`
    - `SE999999EmployeeServiceApplication`
    - `SE999999EmployeeController`
    - `SE999999EmployeeGatewayApplication`
6. Tải lại Maven và chạy `mvn clean test` cho cả ba dự án.

Lưu ý package Java luôn viết thường; tiền tố tên dự án/lớp dùng đúng chữ hoa như MSSV.

---

## 11. Danh sách công việc gợi ý trong 90 phút

### 0–10 phút: tạo dự án và tải thư viện

- [ ] Chọn JDK 21.
- [ ] Tạo đúng ba tên dự án.
- [ ] Chọn các thư viện.
- [ ] Chốt các cổng, URL cơ sở dữ liệu và package.
- [ ] Tải lại Maven khi Internet còn được phép.

### 10–25 phút: cơ sở dữ liệu + entity + DTO

- [ ] Chạy SQL chính thức.
- [ ] Gõ entity và kiểm tra từng `@Column`.
- [ ] Gõ ApiResponseDTO, PageDTO và DTO nghiệp vụ.

### 25–50 phút: Department Service

- [ ] Repository.
- [ ] Ngoại lệ và status.
- [ ] Các thao tác tạo/cập nhật/chi tiết/vô hiệu hóa/danh sách của dịch vụ.
- [ ] Lớp điều khiển.
- [ ] Swagger và tệp properties.

### 50–70 phút: Employee Service

- [ ] Entity và repository.
- [ ] DepartmentClient + `@EnableFeignClients`.
- [ ] Kiểm tra hợp lệ trong dịch vụ và ánh xạ Department lồng nhau.
- [ ] Lớp điều khiển và tệp properties.

### 70–80 phút: Gateway

- [ ] Định tuyến đến 8081/8082.
- [ ] CORS cho tất cả máy chủ nguồn.
- [ ] Security cho phép mọi yêu cầu bằng `permitAll`.
- [ ] Swagger.

### 80–90 phút: kiểm tra và nộp

- [ ] Biên dịch và kiểm thử cả ba bằng Maven.
- [ ] Kiểm tra nhanh qua cổng 8080.
- [ ] Kiểm tra tên dự án, tên lớp, package, bảng/cột và cổng.
- [ ] Xóa `target`, `.idea`, `.git` khỏi bản nộp nếu quy định không cần.
- [ ] Nén **mỗi dự án thành một tệp riêng**.

---

## 12. Nén ZIP đúng cách để nộp EOS

Kết quả phải là ba ZIP độc lập, không phải một ZIP bọc cả ba:

```text
SE180211DepartmentService.zip
SE180211EmployeeService.zip
SE180211EmployeeGateway.zip
```

Mở mỗi ZIP và kiểm tra ngay cấp đầu có thư mục dự án hoặc các tệp `pom.xml`, `src`; không được vô tình tạo nhiều lớp thư
mục lồng nhau. Không đưa mật khẩu cơ sở dữ liệu khác đề vào bài.

PowerShell tham khảo khi đứng tại thư mục answer:

```powershell
Compress-Archive -Path .\SE180211DepartmentService -DestinationPath .\SE180211DepartmentService.zip
Compress-Archive -Path .\SE180211EmployeeService -DestinationPath .\SE180211EmployeeService.zip
Compress-Archive -Path .\SE180211EmployeeGateway -DestinationPath .\SE180211EmployeeGateway.zip
```

Nên chạy `mvn clean` trước khi zip để không nộp thư mục `target` nặng. Sau khi zip, giải nén thử vào một thư mục khác và
chạy `mvn test` một lần cuối nếu còn thời gian.

---

## 13. Danh sách đối chiếu cuối cùng với đề

- [ ] Spring Boot `3.5.11`.
- [ ] JDK `21`, kiểu đóng gói `jar`.
- [ ] Department 8081, Employee 8082, Gateway 8080.
- [ ] Cơ sở dữ liệu `MSS301_2026_PE`, `sa/sa`, `encrypt=false`, `ddl-auto=none`.
- [ ] Đúng ba tên dự án và hai tên controller có MSSV.
- [ ] Package `fu.<studentId>.department|employee|gateway` viết thường.
- [ ] Đúng mọi bảng/cột trong SQL.
- [ ] Mã Department duy nhất, chỉ gồm chữ/số, tối đa 10 ký tự.
- [ ] Tất cả quy tắc độ dài/bắt buộc/status/ngày đều được kiểm tra hợp lệ.
- [ ] Thao tác tạo mới mặc định ACTIVE.
- [ ] DELETE chỉ đổi INACTIVE.
- [ ] PUT một phần không ghi đè trường bị bỏ qua.
- [ ] Danh sách có page mặc định 0, size 10, tối đa 100.
- [ ] Phản hồi Employee có DepartmentDTO lấy qua Feign.
- [ ] Mọi phản hồi JSON có status/data/timestamp.
- [ ] Lỗi dữ liệu/trùng/không tìm thấy/lỗi nội bộ ánh xạ đúng 2/3/4/0.
- [ ] Gateway định tuyến đúng và CORS chấp nhận mọi nguồn.
- [ ] Swagger hoạt động ở cả ba dự án.
- [ ] Các kiểm thử Maven đều thành công.
- [ ] Ba dự án được nén ZIP riêng.

Nếu cần đối chiếu mã nguồn đầy đủ trong lúc học, mở lớp tương ứng ngay trong ba dự án cạnh tệp hướng dẫn này. Hãy tập gõ
theo thứ tự trong tài liệu ít nhất một lần khi không có Internet; đó là cách phát hiện sớm các thư viện, lệnh import và
annotation dễ quên nhất.
