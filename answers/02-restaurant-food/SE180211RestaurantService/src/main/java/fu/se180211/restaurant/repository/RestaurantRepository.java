package fu.se180211.restaurant.repository;

import fu.se180211.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, JpaSpecificationExecutor<Restaurant> {
    boolean existsByNameIgnoreCase(String v);

    boolean existsByNameIgnoreCaseAndRestaurantIdNot(String v, Long id);
}
