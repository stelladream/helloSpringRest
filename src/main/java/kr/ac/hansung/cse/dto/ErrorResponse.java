package kr.ac.hansung.cse.dto;

import lombok.Getter;

import java.util.List;

/**
 * REST API 에러 응답 DTO
 *
 * 에러 응답 예시:
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "존재하지 않는 상품 ID: 999",
 *   "fieldErrors": null
 * }
 *
 * 유효성 검사 실패 시 fieldErrors가 포함됩니다:
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "입력값 검증에 실패했습니다.",
 *   "fieldErrors": [
 *     { "field": "name", "message": "상품명은 필수 입력 항목입니다." }
 *   ]
 * }
 */
@Getter
public class ErrorResponse {

    private final int status;
    private final String error;
    private final String message;
    private final List<FieldError> fieldErrors;

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.fieldErrors = null;
    }

    public ErrorResponse(int status, String error, String message, List<FieldError> fieldErrors) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    /** 필드 단위 유효성 검사 오류 */
    @Getter
    public static class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
