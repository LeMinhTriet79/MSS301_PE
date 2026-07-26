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

    @PostMapping
    public ResponseEntity<ApiResponseDTO> createEmployee(@RequestBody EmployeeDTO dto) {
        ApiResponseDTO response = employeeService.createEmployee(dto);
        if (response.getStatus() == 1) return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201[cite: 16]
        if (response.getStatus() == 0) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500[cite: 16]
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400[cite: 16]
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<ApiResponseDTO> getEmployeeDetail(@PathVariable("employeeId") Integer employeeId) {
        ApiResponseDTO response = employeeService.getEmployeeDetail(employeeId);
        if (response.getStatus() == 1) return ResponseEntity.status(HttpStatus.OK).body(response); // 200[cite: 16]
        if (response.getStatus() == 4) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400[cite: 16]
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500[cite: 16]
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<ApiResponseDTO> updateEmployee(@PathVariable("employeeId") Integer employeeId, @RequestBody EmployeeDTO dto) {
        ApiResponseDTO response = employeeService.updateEmployee(employeeId, dto);
        if (response.getStatus() == 1) return ResponseEntity.status(HttpStatus.OK).body(response); // 200[cite: 16]
        if (response.getStatus() == 0) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500[cite: 16]
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400[cite: 16]
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<ApiResponseDTO> deactivateEmployee(@PathVariable("employeeId") Integer employeeId) {
        ApiResponseDTO response = employeeService.deactivateEmployee(employeeId);
        if (response.getStatus() == 1) return ResponseEntity.status(HttpStatus.OK).body(response); // 200[cite: 16]
        if (response.getStatus() == 4) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400[cite: 16]
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500[cite: 16]
    }
}
