package fu.se180211.restaurant.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class RestaurantDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsR04IsoUtcTimestamp() throws Exception {
        RestaurantDTO dto = objectMapper.readValue(
                "{\"openDate\":\"2026-03-14T10:00:00Z\"}",
                RestaurantDTO.class);

        assertThat(dto.getOpenDate()).isEqualTo(Date.from(Instant.parse("2026-03-14T10:00:00Z")));
    }

    @Test
    void alsoAcceptsDateSamplePrintedInPaper() throws Exception {
        RestaurantDTO dto = objectMapper.readValue(
                "{\"openDate\":\"20/05/2025\"}",
                RestaurantDTO.class);

        assertThat(dto.getOpenDate()).isEqualTo(Date.from(Instant.parse("2025-05-20T00:00:00Z")));
    }

    @Test
    void alwaysSerializesOpenDateAsIsoUtc() throws Exception {
        RestaurantDTO dto = new RestaurantDTO();
        dto.setOpenDate(Date.from(Instant.parse("2026-03-14T10:00:00Z")));

        assertThat(objectMapper.writeValueAsString(dto))
                .contains("\"openDate\":\"2026-03-14T10:00:00Z\"");
    }
}
