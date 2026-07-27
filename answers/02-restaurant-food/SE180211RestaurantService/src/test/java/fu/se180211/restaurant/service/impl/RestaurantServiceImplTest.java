package fu.se180211.restaurant.service.impl;

import fu.se180211.restaurant.common.BusinessException;
import fu.se180211.restaurant.common.ResponseStatuses;
import fu.se180211.restaurant.dto.RestaurantDTO;
import fu.se180211.restaurant.entity.Restaurant;
import fu.se180211.restaurant.repository.CategoryRepository;
import fu.se180211.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private RestaurantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RestaurantServiceImpl(restaurantRepository, categoryRepository);
    }

    @Test
    void createValidRestaurantTrimsTextAndNormalizesStatus() {
        RestaurantDTO request = validRequest();
        request.setName("  BBQ Hoa Lac  ");
        request.setStatus("active");
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(restaurantRepository.existsByNameIgnoreCase("BBQ Hoa Lac")).thenReturn(false);
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> {
            Restaurant saved = invocation.getArgument(0);
            saved.setRestaurantId(10L);
            return saved;
        });

        RestaurantDTO result = service.create(request);

        assertThat(result.getRestaurantId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("BBQ Hoa Lac");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getCategoryId()).isEqualTo(1L);
    }

    @Test
    void createRejectsDuplicateNameWithSpecifiedStatusThree() {
        RestaurantDTO request = validRequest();
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(restaurantRepository.existsByNameIgnoreCase("BBQ Hoa Lac")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getApiStatus()).isEqualTo(ResponseStatuses.DUPLICATE_CODE);
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getMessage()).isEqualTo("Name is duplicated");
                });

        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownMandatoryCategory() {
        when(categoryRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getApiStatus()).isEqualTo(ResponseStatuses.NOT_FOUND);
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void updateIsPartialAndKeepsOmittedValues() {
        Restaurant current = persistedRestaurant();
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(current));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RestaurantDTO patch = new RestaurantDTO();
        patch.setOwner("  New Owner  ");

        RestaurantDTO result = service.update(10L, patch);

        assertThat(result.getOwner()).isEqualTo("New Owner");
        assertThat(result.getName()).isEqualTo("BBQ Hoa Lac");
        assertThat(result.getOpenDate()).isEqualTo(current.getOpenDate());
        verify(categoryRepository, never()).existsById(anyLong());
    }

    @Test
    void listRejectsPageSizeOverOneHundred() {
        assertThatThrownBy(() -> service.list(0, 101, null, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getApiStatus()).isEqualTo(ResponseStatuses.VALIDATION_FAILED));
    }

    private RestaurantDTO validRequest() {
        RestaurantDTO dto = new RestaurantDTO();
        dto.setName("BBQ Hoa Lac");
        dto.setOwner("ABC Company");
        dto.setAddress("Hoa Lac Park");
        dto.setOpenDate(Date.from(Instant.parse("2026-03-14T10:00:00Z")));
        dto.setPriceFrom(2_000);
        dto.setPriceTo(3_000);
        dto.setPhone("01234567890");
        dto.setStatus("ACTIVE");
        dto.setCategoryId(1L);
        return dto;
    }

    private Restaurant persistedRestaurant() {
        Restaurant entity = new Restaurant();
        entity.setRestaurantId(10L);
        entity.setName("BBQ Hoa Lac");
        entity.setOwner("ABC Company");
        entity.setAddress("Hoa Lac Park");
        entity.setOpenDate(Date.from(Instant.parse("2026-03-14T10:00:00Z")));
        entity.setPriceFrom(2_000);
        entity.setPriceTo(3_000);
        entity.setPhone("01234567890");
        entity.setStatus("ACTIVE");
        entity.setCategoryId(1L);
        return entity;
    }
}
