package fu.se180211.food.service.impl;

import fu.se180211.food.common.BusinessException;
import fu.se180211.food.common.ResponseStatuses;
import fu.se180211.food.config.RestaurantClient;
import fu.se180211.food.dto.ApiResponseDTO;
import fu.se180211.food.dto.FoodDTO;
import fu.se180211.food.dto.FoodResponseDTO;
import fu.se180211.food.dto.RestaurantDTO;
import fu.se180211.food.entity.Food;
import fu.se180211.food.repository.FoodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodServiceImplTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private RestaurantClient restaurantClient;

    private FoodServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FoodServiceImpl(foodRepository, restaurantClient);
    }

    @Test
    void createValidFoodChecksRestaurantAndNormalizesStatus() {
        when(restaurantClient.getRestaurant(1L)).thenReturn(ApiResponseDTO.success(restaurant(1L)));
        when(foodRepository.save(any(Food.class))).thenAnswer(invocation -> {
            Food saved = invocation.getArgument(0);
            saved.setFoodId(20L);
            return saved;
        });
        FoodDTO request = validRequest();
        request.setName("  Pho Bo  ");
        request.setStatus("active");

        FoodDTO result = service.create(request);

        assertThat(result.getFoodId()).isEqualTo(20L);
        assertThat(result.getName()).isEqualTo("Pho Bo");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getRestaurantId()).isEqualTo(1L);
    }

    @Test
    void createRejectsUnknownRestaurantWithStatusFour() {
        when(restaurantClient.getRestaurant(1L)).thenReturn(ApiResponseDTO.of(ResponseStatuses.NOT_FOUND, null));

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getApiStatus()).isEqualTo(ResponseStatuses.NOT_FOUND);
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getMessage()).isEqualTo("Restaurant ID is not found");
                });

        verify(foodRepository, never()).save(any());
    }

    @Test
    void detailReturnsFoodResponseDtoWithNestedRestaurant() {
        when(foodRepository.findById(20L)).thenReturn(Optional.of(persistedFood()));
        when(restaurantClient.getRestaurant(1L)).thenReturn(ApiResponseDTO.success(restaurant(1L)));

        FoodResponseDTO result = service.get(20L);

        assertThat(result.getFoodId()).isEqualTo(20L);
        assertThat(result.getIngredients()).isEqualTo("Beef, noodle");
        assertThat(result.getRestaurant().getRestaurantId()).isEqualTo(1L);
    }

    @Test
    void updateIsPartialAndKeepsOmittedValues() {
        Food current = persistedFood();
        when(foodRepository.findById(20L)).thenReturn(Optional.of(current));
        when(foodRepository.save(any(Food.class))).thenAnswer(invocation -> invocation.getArgument(0));
        FoodDTO patch = new FoodDTO();
        patch.setPrice(75_000);

        FoodDTO result = service.update(20L, patch);

        assertThat(result.getPrice()).isEqualTo(75_000);
        assertThat(result.getName()).isEqualTo("Pho Bo");
        assertThat(result.getRestaurantId()).isEqualTo(1L);
        verifyNoInteractions(restaurantClient);
    }

    @Test
    void listRejectsNegativePage() {
        assertThatThrownBy(() -> service.list(-1, 10, null, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getApiStatus()).isEqualTo(ResponseStatuses.VALIDATION_FAILED));
    }

    private FoodDTO validRequest() {
        FoodDTO dto = new FoodDTO();
        dto.setName("Pho Bo");
        dto.setPrice(50_000);
        dto.setIngredients("Beef, noodle");
        dto.setRestaurantId(1L);
        dto.setStatus("ACTIVE");
        return dto;
    }

    private Food persistedFood() {
        Food entity = new Food();
        entity.setFoodId(20L);
        entity.setName("Pho Bo");
        entity.setPrice(50_000);
        entity.setIngredients("Beef, noodle");
        entity.setRestaurantId(1L);
        entity.setStatus("ACTIVE");
        return entity;
    }

    private RestaurantDTO restaurant(long id) {
        RestaurantDTO dto = new RestaurantDTO();
        dto.setRestaurantId(id);
        dto.setName("BBQ Hoa Lac");
        dto.setStatus("ACTIVE");
        return dto;
    }
}
