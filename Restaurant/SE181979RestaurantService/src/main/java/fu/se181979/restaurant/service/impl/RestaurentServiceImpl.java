package fu.se181979.restaurant.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se181979.restaurant.dto.ApiResponse;
import fu.se181979.restaurant.dto.RestaurantDTO;
import fu.se181979.restaurant.entity.Restaurant;
import fu.se181979.restaurant.repository.RestaurantRepository;
import fu.se181979.restaurant.service.RestaurentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RestaurentServiceImpl implements RestaurentService {

    @Autowired
    private RestaurantRepository restaurantRepository;


    @Override
    public ApiResponse createRestaurant(RestaurantDTO dto) {
       

            Restaurant entity = new Restaurant();

            entity.setName(dto.getName());
            entity.setOwnerName(dto.getOwnerName());
            entity.setPriceFrom(dto.getPriceFrom());
            entity.setPriceTo(dto.getPriceTo());
            entity.setPhone(dto.getPhone());
            entity.setAddress(dto.getAddress());
            entity.setOpenDate(dto.getOpenDate());
            entity.setStatus("ACTIVE"); // Thường mặc định là ACTIVE lúc tạo mới

            entity.setCategoryId(dto.getCategoryId());
            Restaurant savedEntity = restaurantRepository.save(entity);

            // Nếu có Feign, truyền otherResponse.getData() vào tham số thứ 2. Nếu không, truyền null.
            return new ApiResponse(1, "successfully", mapToDTO(savedEntity));

    }

    // =========================================================
    // 2. GET DETAIL (LẤY CHI TIẾT THEO ID)
    // =========================================================
    @Override
    public ApiResponse getRestaurantDetail(Integer id) {
        try {
            Restaurant entity = restaurantRepository.findById(id).orElse(null);
            if (entity == null) {
                return new ApiResponse(4, "not found" ,null); // 4: Lỗi 400 Không tìm thấy
            }


            Object otherData = null;
            /*
            ApiResponse otherResponse = [otherEntity]Client.get[OtherEntity]ByID(entity.get[OtherId]());
            otherData = (otherResponse != null && otherResponse.getStatus() == 1) ? otherResponse.getData() : null;
            */

            return new ApiResponse(1, "successfully",mapToDTO(entity));
        } catch (Exception e) {
            return new ApiResponse(0,"server error" ,null);
        }
    }

    // =========================================================
    // 3. UPDATE (CHỈNH SỬA)
    // =========================================================
//    @Override
//    public ApiResponse updateRestaurant(Integer id, RestaurantDTO dto) {
//        try {
//            Restaurant entity = restaurantRepository.findById(id).orElse(null);
//            if (entity == null) {
//                return new ApiResponse(4, null); // 4: Lỗi 400 Không tìm thấy
//            }
//
//            // [THAY THẾ]: Cập nhật các trường (nếu client có gửi lên thì mới update)
//            if (dto.getName() != null) entity.setName(dto.getName());
//            if (dto.get[Field2]() != null) entity.set[Field2](dto.get[Field2]());
//            // ...
//
//            Restaurant savedEntity = restaurantRepository.save(entity);
//            return new ApiResponse(1, mapToDTO(savedEntity, null));
//        } catch (Exception e) {
//            return new ApiResponse(0, null);
//        }
//    }
//
//    // =========================================================
//    // 4. DEACTIVATE (XÓA MỀM / HỦY KÍCH HOẠT)
//    // =========================================================
//    @Override
//    public ApiResponse deactivateRestaurant(Integer id) {
//        try {
//            Restaurant entity = restaurantRepository.findById(id).orElse(null);
//            if (entity == null) {
//                return new ApiResponse(4, null);
//            }
//            entity.setStatus("INACTIVE"); // Đổi trạng thái thành INACTIVE
//            restaurantRepository.save(entity);
//            return new ApiResponse(1, null);
//        } catch (Exception e) {
//            return new ApiResponse(0, null);
//        }
//    }

    // =========================================================
    // HÀM BỔ TRỢ: MAP TỪ ENTITY SANG DTO
    // =========================================================
    private RestaurantDTO mapToDTO(Restaurant entity) {
        RestaurantDTO dto = new RestaurantDTO();

        // [THAY THẾ]: Mapping cơ bản (Tự gõ)
        dto.setRestaurantId(entity.getId());
        dto.setName(entity.getName());
        dto.setOwnerName(entity.getOwnerName());
        dto.setPriceFrom(entity.getPriceFrom());
        dto.setPriceTo(entity.getPriceTo());
        dto.setPhone(entity.getPhone());
        dto.setAddress(entity.getAddress());
        dto.setOpenDate(entity.getOpenDate());
        dto.setStatus(entity.getStatus());
        dto.setCategoryId(entity.getCategoryId());


        return dto;
    }
}
