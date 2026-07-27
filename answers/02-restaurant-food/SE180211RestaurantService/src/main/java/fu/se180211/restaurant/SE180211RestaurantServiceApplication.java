package fu.se180211.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SE180211RestaurantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SE180211RestaurantServiceApplication.class, args);
    }
}
