package fu.se181979.employee.controller;

import fu.se181979.employee.dto.ApiResponseDTO;
import fu.se181979.employee.dto.EmployeeDTO;
import fu.se181979.employee.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    // 1. CREATE EMPLOYEE
    @PostMapping
    public ResponseEntity<ApiResponseDTO> createEmployee(@RequestBody EmployeeDTO dto) {
        ApiResponseDTO response = employeeService.createEmployee(dto);

        if (response.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response); // HTTP 201[cite: 3]
        } else if (response.getStatus() == 0) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // HTTP 500[cite: 3]
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // HTTP 400[cite: 3]
        }
    }

    // 2. GET EMPLOYEE DETAIL
    @GetMapping("/{employeeId}")
    public ResponseEntity<ApiResponseDTO> getEmployeeDetail(@PathVariable("employeeId") Integer employeeId) {
        ApiResponseDTO response = employeeService.getEmployeeDetail(employeeId);

        if (response.getStatus() == 1) {
            return ResponseEntity.status(HttpStatus.OK).body(response); // HTTP 200[cite: 3]
        } else if (response.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // HTTP 400[cite: 3]
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // HTTP 500[cite: 3]
        }
    }
}
