package fu.se180211.employee.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponseDTO<T> {
    private int status;
    private T data;
    private String timestamp;

    public ApiResponseDTO() {
    }

    public ApiResponseDTO(int status, T data) {
        this.status = status;
        this.data = data;
        this.timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }

    public static <T> ApiResponseDTO<T> success(T data) {
        return new ApiResponseDTO<>(1, data);
    }

    public static <T> ApiResponseDTO<T> of(int status, T data) {
        return new ApiResponseDTO<>(status, data);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int v) {
        status = v;
    }

    public T getData() {
        return data;
    }

    public void setData(T v) {
        data = v;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String v) {
        timestamp = v;
    }
}
