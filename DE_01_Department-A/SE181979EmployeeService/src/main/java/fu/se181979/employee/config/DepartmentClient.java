package fu.se181979.employee.config;

import fu.se181979.employee.dto.ApiResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "department-service", url = "http://localhost:8081")
public interface DepartmentClient {

    @GetMapping("/api/departments/{departmentId}")
    ApiResponseDTO getDepartmentByID(@PathVariable("departmentId") Integer departmentId);
}