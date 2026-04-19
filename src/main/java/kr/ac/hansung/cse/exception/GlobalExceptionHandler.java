package kr.ac.hansung.cse.exception;

import kr.ac.hansung.cse.dto.ErrorResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * =====================================================================
 * GlobalExceptionHandler - REST API 전역 예외 처리기
 * =====================================================================
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 *   - 모든 @RestController에서 발생한 예외를 중앙에서 처리합니다.
 *   - 예외 종류별로 적절한 HTTP 상태 코드와 JSON 에러 응답을 반환합니다.
 *
 * [예외 처리 우선순위]
 *   더 구체적인 예외 타입이 우선 적용됩니다.
 *   ProductNotFoundException > IllegalArgumentException > Exception
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 404 Not Found: 상품을 찾을 수 없는 경우
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        404,
                        "Not Found",
                        ex.getMessage()
                ));
    }

    /**
     * 400 Bad Request: @Valid 검증 실패
     *
     * @RequestBody + @Valid 조합에서 검증 실패 시 발생합니다.
     * 각 필드의 오류 메시지를 fieldErrors 배열로 상세 반환합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map((FieldError e) -> new ErrorResponse.FieldError(e.getField(), e.getDefaultMessage()))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        400,
                        "Bad Request",
                        "입력값 검증에 실패했습니다.",
                        fieldErrors
                ));
    }

    /**
     * 400 Bad Request: 비즈니스 규칙 위반
     *
     * 예) 가격이 음수인 경우 (ProductService에서 발생)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        400,
                        "Bad Request",
                        ex.getMessage()
                ));
    }

    /**
     * 500 Internal Server Error: 데이터베이스 오류
     *
     * Spring이 JDBC/JPA 예외를 DataAccessException 계층으로 변환합니다.
     * 민감한 DB 오류 정보는 클라이언트에게 노출하지 않습니다.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        500,
                        "Internal Server Error",
                        "데이터베이스 처리 중 오류가 발생했습니다."
                ));
    }

    /**
     * 500 Internal Server Error: 예상치 못한 모든 예외 (최종 안전망)
     *
     * 예외 상세 정보를 숨겨 보안을 유지합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        500,
                        "Internal Server Error",
                        "예상치 못한 오류가 발생했습니다."
                ));
    }
}
