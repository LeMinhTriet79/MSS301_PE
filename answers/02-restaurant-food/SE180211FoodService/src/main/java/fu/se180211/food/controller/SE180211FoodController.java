package fu.se180211.food.controller;

import fu.se180211.food.dto.ApiResponseDTO;
import fu.se180211.food.dto.FoodDTO;
import fu.se180211.food.dto.FoodListDTO;
import fu.se180211.food.dto.FoodResponseDTO;
import fu.se180211.food.service.FoodService;
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
@RequestMapping(value = "/api/foods", produces = MediaType.APPLICATION_JSON_VALUE)
public class SE180211FoodController {

    private final FoodService service;

    public SE180211FoodController(FoodService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<FoodDTO>> create(@Valid @RequestBody FoodDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.success(service.create(dto)));
    }

    @PutMapping(value = "/{foodId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<FoodDTO>> update(
            @PathVariable("foodId") Long foodId,
            @Valid @RequestBody FoodDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(service.update(foodId, dto)));
    }

    @GetMapping("/{foodId}")
    public ResponseEntity<ApiResponseDTO<FoodResponseDTO>> detail(@PathVariable("foodId") Long foodId) {
        return ResponseEntity.ok(ApiResponseDTO.success(service.get(foodId)));
    }

    @DeleteMapping("/{foodId}")
    public ResponseEntity<ApiResponseDTO<Void>> deactivate(@PathVariable("foodId") Long foodId) {
        service.deactivate(foodId);
        return ResponseEntity.ok(ApiResponseDTO.success(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<FoodListDTO>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String ingredients) {
        return ResponseEntity.ok(ApiResponseDTO.success(service.list(page, size, name, ingredients)));
    }
}
