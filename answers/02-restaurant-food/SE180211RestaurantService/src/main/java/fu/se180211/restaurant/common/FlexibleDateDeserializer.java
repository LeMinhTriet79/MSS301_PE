package fu.se180211.restaurant.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Accepts the ISO-8601 format required by R04 and the dd/MM/yyyy format shown
 * in the RestaurantDTO example. Responses are still serialized as ISO-8601
 * by {@code @JsonFormat} on RestaurantDTO.openDate.
 */
public class FlexibleDateDeserializer extends JsonDeserializer<Date> {

    private static final DateTimeFormatter SAMPLE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu");

    @Override
    public Date deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String raw = parser.getValueAsString();
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim();
        try {
            return Date.from(Instant.parse(value));
        } catch (DateTimeParseException ignored) {
            // ISO values with an explicit non-Z offset are valid ISO-8601 too.
        }

        try {
            return Date.from(OffsetDateTime.parse(value).toInstant());
        } catch (DateTimeParseException ignored) {
            // Fall through to the exact date-only sample printed in the paper.
        }

        try {
            LocalDate date = LocalDate.parse(value, SAMPLE_FORMAT);
            return Date.from(date.atStartOfDay().toInstant(ZoneOffset.UTC));
        } catch (DateTimeParseException ex) {
            throw InvalidFormatException.from(
                    parser,
                    "openDate must use ISO-8601 (for example 2026-03-14T10:00:00Z) "
                            + "or the paper's dd/MM/yyyy sample format",
                    value,
                    Date.class);
        }
    }
}
