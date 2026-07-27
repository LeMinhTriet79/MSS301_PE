package fu.se180211.restaurant.controller;

import fu.se180211.restaurant.dto.ApiResponseDTO;
import fu.se180211.restaurant.dto.CategoryDTO;
import fu.se180211.restaurant.dto.PageDTO;
import fu.se180211.restaurant.service.CategoryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/categories", produces = MediaType.APPLICATION_JSON_VALUE)
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageDTO<CategoryDTO>>> list(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ApiResponseDTO.success(service.list(page, size)));
    }
}
