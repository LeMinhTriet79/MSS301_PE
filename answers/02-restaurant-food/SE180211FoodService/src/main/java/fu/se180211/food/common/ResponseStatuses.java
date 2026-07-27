package fu.se180211.food.common;

public final class ResponseStatuses {
    public static final int INTERNAL_ERROR = 0;
    public static final int SUCCESS = 1;
    public static final int VALIDATION_FAILED = 2;
    public static final int DUPLICATE_CODE = 3;
    public static final int NOT_FOUND = 4;

    private ResponseStatuses() {
    }
}
