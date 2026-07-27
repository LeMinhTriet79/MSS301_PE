package fu.se180211.restaurant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponseDTO<T> {
    private int status;
    private String message;
    private T data;

    public ApiResponseDTO() {
    }

    public ApiResponseDTO(int status, T data) {
        this(status, messageFor(status), data);
    }

    public ApiResponseDTO(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponseDTO<T> success(T data) {
        return new ApiResponseDTO<>(1, data);
    }

    public static <T> ApiResponseDTO<T> of(int status, T data) {
        return new ApiResponseDTO<>(status, data);
    }

    public static <T> ApiResponseDTO<T> error(int status, String message) {
        return new ApiResponseDTO<>(status,
                message == null || message.isBlank() ? messageFor(status) : message,
                null);
    }

    private static String messageFor(int status) {
        return switch (status) {
            case 1 -> "Successful";
            case 2 -> "Data validation failed";
            case 3 -> "Duplicated data";
            case 4 -> "Data is not found";
            default -> "Internal server error";
        };
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int v) {
        status = v;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String v) {
        message = v;
    }

    public T getData() {
        return data;
    }

    public void setData(T v) {
        data = v;
    }
}
