package fu.se180211.department.controller;

import fu.se180211.department.dto.ApiResponseDTO;
import fu.se180211.department.dto.DepartmentDTO;
import fu.se180211.department.dto.PageDTO;
import fu.se180211.department.service.DepartmentService;
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
@RequestMapping(value = "/api/departments", produces = MediaType.APPLICATION_JSON_VALUE)
public class SE180211DepartmentController {

    private final DepartmentService service;

    public SE180211DepartmentController(DepartmentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<DepartmentDTO>> create(@Valid @RequestBody DepartmentDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.success(service.create(dto)));
    }

    @PutMapping(value = "/{departmentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<DepartmentDTO>> update(@PathVariable Long departmentId,
                                                                @Valid @RequestBody DepartmentDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(service.update(departmentId, dto)));
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<ApiResponseDTO<DepartmentDTO>> detail(@PathVariable Long departmentId) {
        return ResponseEntity.ok(ApiResponseDTO.success(service.get(departmentId)));
    }

    @DeleteMapping("/{departmentId}")
    public ResponseEntity<ApiResponseDTO<Void>> deactivate(@PathVariable Long departmentId) {
        service.deactivate(departmentId);
        return ResponseEntity.ok(ApiResponseDTO.success(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageDTO<DepartmentDTO>>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponseDTO.success(service.list(page, size, name, status)));
    }
}
