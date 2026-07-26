package fu.se182638.department.controller;

import fu.se182638.department.dto.DepartmentDto;
import fu.se182638.department.entity.Department;
import fu.se182638.department.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    //tiêm vào DeptService để xuống Db qua cửa Repo
    @Autowired
    private DepartmentService deptService;

    //tạo mới 1 department, nhận JSon dto gửi lên từ client
    @PostMapping
    public ResponseEntity<DepartmentDto> createDept(@RequestBody DepartmentDto feDto) {

        // cứ làm tới. ko cần try catch, mình sẽ gom chung 1 nơi xử lý các Exception
        DepartmentDto createdDto = deptService.createDepartment(feDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdDto);
    }
}
