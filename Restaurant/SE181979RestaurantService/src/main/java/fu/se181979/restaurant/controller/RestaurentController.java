package fu.se181979.restaurant.controller;

import fu.se181979.restaurant.dto.ApiResponse;
import fu.se181979.restaurant.dto.RestaurantDTO;
import fu.se181979.restaurant.service.RestaurentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurents")
public class RestaurentController {

    @Autowired
    private RestaurentService restaurantService;

    // 1. CREATE
    @PostMapping
    public ResponseEntity<ApiResponse> createRestaurant(@RequestBody RestaurantDTO dto) {
        ApiResponse response = restaurantService.createRestaurant(dto);
        if (response.getStatus() == 1) return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201 Created
        if (response.getStatus() == 0) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400
    }

    // 2. GET DETAIL
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getRestaurantDetail(@PathVariable("id") Integer id) {
        ApiResponse response = restaurantService.getRestaurantDetail(id);
        if (response.getStatus() == 1) return ResponseEntity.status(HttpStatus.OK).body(response); // 200 OK
        if (response.getStatus() == 4) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500
    }

    // 3. UPDATE
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse> updateRestaurant(@PathVariable("id") Integer id, @RequestBody RestaurantDTO dto) {
//        ApiResponse response = restaurantService.updateRestaurant(id, dto);
//        if (response.getStatus() == 1) return ResponseEntity.status(HttpStatus.OK).body(response); // 200 OK
//        if (response.getStatus() == 0) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400
//    }
//
//    // 4. DEACTIVATE (DELETE)
//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse> deactivateRestaurant(@PathVariable("id") Integer id) {
//        ApiResponse response = restaurantService.deactivateRestaurant(id);
//        if (response.getStatus() == 1) return ResponseEntity.status(HttpStatus.OK).body(response); // 200 OK
//        if (response.getStatus() == 4) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500
//    }
}
