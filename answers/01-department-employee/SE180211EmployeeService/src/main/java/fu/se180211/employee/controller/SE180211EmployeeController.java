package fu.se180211.employee.controller;

import fu.se180211.employee.dto.ApiResponseDTO;
import fu.se180211.employee.dto.EmployeeDTO;
import fu.se180211.employee.dto.PageDTO;
import fu.se180211.employee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/employees", produces = MediaType.APPLICATION_JSON_VALUE)
public class SE180211EmployeeController {

    private final EmployeeService service;

    public SE180211EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<EmployeeDTO>> create(@Valid @RequestBody EmployeeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.success(service.create(dto)));
    }

    @PutMapping(value = "/{employeeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<EmployeeDTO>> update(@PathVariable Long employeeId,
                                                              @Valid @RequestBody EmployeeDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(service.update(employeeId, dto)));
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<ApiResponseDTO<EmployeeDTO>> detail(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponseDTO.success(service.get(employeeId)));
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<ApiResponseDTO<Void>> deactivate(@PathVariable Long employeeId) {
        service.deactivate(employeeId);
        return ResponseEntity.ok(ApiResponseDTO.success(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageDTO<EmployeeDTO>>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponseDTO.success(service.list(page, size, name, status)));
    }
}
