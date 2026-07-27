package fu.se180211.restaurant.controller;

import fu.se180211.restaurant.dto.ApiResponseDTO;
import fu.se180211.restaurant.dto.PageDTO;
import fu.se180211.restaurant.dto.RestaurantDTO;
import fu.se180211.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/restaurants", produces = MediaType.APPLICATION_JSON_VALUE)
public class SE180211RestaurantController {

    private final RestaurantService service;

    public SE180211RestaurantController(RestaurantService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<RestaurantDTO>> create(@Valid @RequestBody RestaurantDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.success(service.create(dto)));
    }

    @PutMapping(value = "/{restaurantId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<RestaurantDTO>> update(
            @PathVariable("restaurantId") Long restaurantId,
            @Valid @RequestBody RestaurantDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(service.update(restaurantId, dto)));
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<ApiResponseDTO<RestaurantDTO>> detail(@PathVariable("restaurantId") Long restaurantId) {
        return ResponseEntity.ok(ApiResponseDTO.success(service.get(restaurantId)));
    }

    @DeleteMapping("/{restaurantId}")
    public ResponseEntity<ApiResponseDTO<Void>> deactivate(@PathVariable("restaurantId") Long restaurantId) {
        service.deactivate(restaurantId);
        return ResponseEntity.ok(ApiResponseDTO.success(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageDTO<RestaurantDTO>>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String ownerName) {
        return ResponseEntity.ok(ApiResponseDTO.success(service.list(page, size, name, ownerName)));
    }
}
