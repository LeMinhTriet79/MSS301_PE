package fu.se180211.food;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "fu.se180211.food.config")
public class SE180211FoodServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SE180211FoodServiceApplication.class, args);
    }
}
