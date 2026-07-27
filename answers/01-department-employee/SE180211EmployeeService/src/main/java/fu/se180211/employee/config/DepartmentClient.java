package fu.se180211.employee.config;

import fu.se180211.employee.dto.ApiResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "department-service", url = "${department.service.url:http://localhost:8081}")
public interface DepartmentClient {
    @GetMapping("/api/departments/{departmentId}")
    ApiResponseDTO<Object> getDepartment(@PathVariable("departmentId") Long departmentId);
}
