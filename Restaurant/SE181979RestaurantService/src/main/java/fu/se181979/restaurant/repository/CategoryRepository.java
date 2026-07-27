package fu.se181979.restaurant.repository;

import fu.se181979.restaurant.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}