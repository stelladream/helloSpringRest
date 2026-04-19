package kr.ac.hansung.cse.dto;

import lombok.Getter;

/**
 * REST API 공통 응답 래퍼
 *
 * 모든 API 응답을 일관된 형태로 클라이언트에 전달합니다.
 *
 * 성공 응답 예시:
 * {
 *   "success": true,
 *   "message": "상품이 등록되었습니다.",
 *   "data": { ... }
 * }
 *
 * 실패 응답은 ErrorResponse를 사용합니다.
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /** 메시지 없이 데이터만 반환 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }

    /** 메시지와 함께 데이터 반환 */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /** 데이터 없이 메시지만 반환 (삭제 등) */
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null);
    }
}
