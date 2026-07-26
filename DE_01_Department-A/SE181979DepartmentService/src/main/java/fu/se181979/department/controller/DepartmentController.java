package fu.se181979.department.controller;

import fu.se181979.department.dto.ApiResponseDTO;
import fu.se181979.department.dto.DepartmentDTO;
import fu.se181979.department.service.DepartmentService;
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
        if(response.getStatus() == 1) return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201[cite: 16]
        if(response.getStatus() == 0) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500[cite: 16]
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400[cite: 16]
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<ApiResponseDTO> getDepartmentDetail(@PathVariable("departmentId") Integer departmentId) {
        ApiResponseDTO response = departmentService.getDepartmentDetail(departmentId);
        if (response.getStatus() == 1) return ResponseEntity.status(HttpStatus.OK).body(response); // 200[cite: 16]
        if (response.getStatus() == 4) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400[cite: 16]
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500[cite: 16]
    }

    @PutMapping("/{departmentId}")
    public ResponseEntity<ApiResponseDTO> updateDepartment(@PathVariable("departmentId") Integer departmentId, @RequestBody DepartmentDTO dto) {
        ApiResponseDTO response = departmentService.updateDepartment(departmentId, dto);
        if(response.getStatus() == 1) return ResponseEntity.status(HttpStatus.OK).body(response); // 200[cite: 16]
        if(response.getStatus() == 0) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500[cite: 16]
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400[cite: 16]
    }

    @DeleteMapping("/{departmentId}")
    public ResponseEntity<ApiResponseDTO> deactivateDepartment(@PathVariable("departmentId") Integer departmentId) {
        ApiResponseDTO response = departmentService.deactivateDepartment(departmentId);
        if (response.getStatus() == 1) return ResponseEntity.status(HttpStatus.OK).body(response); // 200[cite: 16]
        if (response.getStatus() == 4) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400[cite: 16]
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500[cite: 16]
    }

    }

