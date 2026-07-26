package fu.se181979.departmentservice.controller;

import fu.se181979.departmentservice.dto.ApiResponseDTO;
import fu.se181979.departmentservice.dto.DepartmentDTO;
import fu.se181979.departmentservice.entity.Department;
import fu.se181979.departmentservice.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO> createDepartment(@RequestBody DepartmentDTO dto) {
        ApiResponseDTO response = departmentService.createDepartment(dto);

        if(response.getStatus() == 1) {
            return  ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else if(response.getStatus() == 0) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<ApiResponseDTO> getDepartmentDetail(@PathVariable("departmentId") Integer departmentId) {
        ApiResponseDTO response = departmentService.getDepartmentDetail(departmentId);

        if (response.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else if (response.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }
}
