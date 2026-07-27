package fu.se180211.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "fu.se180211.employee.config")
public class SE180211EmployeeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SE180211EmployeeServiceApplication.class, args);
    }
}
