package fu.se181979.Food.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@Entity
@Table(name = "Foods")
public class Food {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "food_id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @NotNull
    @Nationalized
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull
    @Column(name = "price", nullable = false)
    private Integer price;

    @Size(max = 500)
    @NotNull
    @Nationalized
    @Column(name = "ingredient", nullable = false, length = 500)
    private String ingredient;

    @NotNull
    @Column(name = "restaurant_id", nullable = false)
    private Integer restaurantId;

    @Size(max = 20)
    @NotNull
    @Nationalized
    @Column(name = "status", nullable = false, length = 20)
    private String status;


}